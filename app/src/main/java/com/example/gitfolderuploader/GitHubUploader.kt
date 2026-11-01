package com.example.gitfolderuploader

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class GitHubException(message: String, val code: Int = 0) : IOException(message)

data class UploadConfig(
    val token: String,
    val owner: String,
    val repo: String,
    val branch: String,
    val message: String,
    val subDir: String,
    val excludes: List<String>,
    val createRepo: Boolean,
    val privateRepo: Boolean,
)

internal const val MAX_FILE_BYTES = 25L * 1024 * 1024 // এর চেয়ে বড় ফাইল বাদ

internal data class LocalFile(val repoPath: String, val doc: DocumentFile, val size: Long)

internal class ScanResult(val files: List<LocalFile>, val skipped: List<String>)

/** ফোল্ডার ঘুরে সব ফাইলের তালিকা বানায় (git status-এর মতো প্রিভিউর জন্যও ব্যবহৃত) */
internal fun scanFolderTree(root: DocumentFile, prefix: String, excludes: List<String>): ScanResult {
    val files = ArrayList<LocalFile>()
    val skipped = ArrayList<String>()

    fun isExcluded(name: String): Boolean = excludes.any { p ->
        if (p.startsWith("*")) name.endsWith(p.substring(1), ignoreCase = true) else name == p
    }

    fun walk(dir: DocumentFile, base: String) {
        for (f in dir.listFiles()) {
            val name = f.name ?: continue
            if (isExcluded(name)) continue
            val path = if (base.isEmpty()) name else "$base/$name"
            when {
                f.isDirectory -> walk(f, path)
                f.isFile -> {
                    val size = f.length()
                    if (size > MAX_FILE_BYTES) skipped += "$path (${size / 1048576}MB — অনেক বড়)"
                    else files += LocalFile(path, f, size)
                }
            }
        }
    }

    walk(root, prefix)
    files.sortBy { it.repoPath }
    return ScanResult(files, skipped)
}

internal fun humanSize(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", b / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", b / 1048576.0)
}

/** টোকেন যাচাই করে GitHub ইউজারনেম ফেরত দেয় */
suspend fun fetchLogin(token: String): String = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    val req = Request.Builder()
        .url("https://api.github.com/user")
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github+json")
        .build()
    try {
        client.newCall(req).execute().use {
            val body = it.body?.string().orEmpty()
            when (it.code) {
                200 -> JSONObject(body).getString("login")
                401 -> throw GitHubException("টোকেন ভুল বা মেয়াদোত্তীর্ণ", 401)
                else -> throw GitHubException("যাচাই ব্যর্থ (${it.code})", it.code)
            }
        }
    } catch (e: GitHubException) {
        throw e
    } catch (e: IOException) {
        throw GitHubException("নেটওয়ার্ক সমস্যা: ${e.message}")
    }
}

data class RepoInfo(
    val fullName: String,
    val isPrivate: Boolean,
    val defaultBranch: String,
)

/** টোকেনের মাধ্যমে যে রিপোগুলোতে push করা যায় তার তালিকা (সাম্প্রতিক push আগে) */
suspend fun fetchRepos(token: String): List<RepoInfo> = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()
    val out = ArrayList<RepoInfo>()
    try {
        for (page in 1..10) {
            val req = Request.Builder()
                .url(
                    "https://api.github.com/user/repos?per_page=100&page=$page" +
                        "&sort=pushed&affiliation=owner,collaborator,organization_member"
                )
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build()
            val n = client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code == 401) throw GitHubException("টোকেন ভুল বা মেয়াদোত্তীর্ণ", 401)
                if (resp.code != 200) throw GitHubException("তালিকা আনা ব্যর্থ (${resp.code})", resp.code)
                val arr = JSONArray(body)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val canPush = o.optJSONObject("permissions")?.optBoolean("push", true) ?: true
                    if (canPush && !o.optBoolean("archived", false)) {
                        out += RepoInfo(
                            fullName = o.getString("full_name"),
                            isPrivate = o.optBoolean("private", false),
                            defaultBranch = o.optString("default_branch", "main"),
                        )
                    }
                }
                arr.length()
            }
            if (n < 100) break
        }
    } catch (e: GitHubException) {
        throw e
    } catch (e: IOException) {
        throw GitHubException("নেটওয়ার্ক সমস্যা: ${e.message}")
    }
    out
}

private class Resp(val code: Int, val body: String, val headers: Headers) {
    fun obj(): JSONObject = JSONObject(body)

    fun ok(what: String): Resp {
        if (code in 200..299) return this
        val msg = runCatching { JSONObject(body).optString("message") }.getOrDefault("")
        throw GitHubException("$what ব্যর্থ ($code): $msg", code)
    }
}

/**
 * পুরো ফোল্ডারকে GitHub Git Data API দিয়ে এক কমিটে আপলোড করে।
 * blobs -> tree -> commit -> ref
 */
class GitHubUploader(
    private val resolver: ContentResolver,
    private val cfg: UploadConfig,
    private val log: (String) -> Unit,
    private val progress: (done: Int, total: Int) -> Unit,
) {
    private companion object {
        const val SMALL = 2L * 1024 * 1024   // এর চেয়ে বড় ফাইল একটা একটা করে যাবে
        const val MIN_INTERVAL_MS = 850L     // GitHub secondary rate limit (~80/মিনিট) এড়াতে
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val api = "https://api.github.com/repos/${cfg.owner}/${cfg.repo}"

    private val throttleLock = Mutex()
    private var nextSlot = 0L

    private suspend fun throttle() {
        throttleLock.withLock {
            val now = System.currentTimeMillis()
            val start = maxOf(now, nextSlot)
            if (start > now) delay(start - now)
            nextSlot = start + MIN_INTERVAL_MS
        }
    }

    private suspend fun call(method: String, url: String, body: String? = null): Resp {
        var attempt = 0
        while (true) {
            throttle()
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${cfg.token}")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .method(method, body?.toRequestBody(jsonType))
                .build()

            val r = try {
                withContext(Dispatchers.IO) {
                    client.newCall(req).execute().use {
                        Resp(it.code, it.body?.string().orEmpty(), it.headers)
                    }
                }
            } catch (e: IOException) {
                if (++attempt > 4) throw GitHubException("নেটওয়ার্ক সমস্যা: ${e.message}")
                log("🔁 নেটওয়ার্ক সমস্যা, আবার চেষ্টা ($attempt)...")
                delay(2000L * attempt)
                continue
            }

            val limited = r.code == 429 ||
                (r.code == 403 && (r.body.contains("rate limit", ignoreCase = true) || r.headers["retry-after"] != null))
            if (limited && attempt < 6) {
                attempt++
                val wait = r.headers["retry-after"]?.toLongOrNull() ?: 60L
                log("⏳ GitHub rate limit — ${wait}s অপেক্ষা...")
                delay(wait * 1000)
                continue
            }
            if (r.code >= 500 && attempt < 3) {
                attempt++
                delay(2000L * attempt)
                continue
            }
            return r
        }
    }

    suspend fun run(root: DocumentFile): String {
        // ১. লোকাল ফাইল স্ক্যান
        log("📂 ফোল্ডার স্ক্যান হচ্ছে...")
        val scan = withContext(Dispatchers.IO) {
            scanFolderTree(root, cfg.subDir.trim('/'), cfg.excludes)
        }
        val files = scan.files
        scan.skipped.forEach { log("⚠️ বাদ: $it") }
        if (files.isEmpty()) throw GitHubException("আপলোড করার মতো কোনো ফাইল পাওয়া যায়নি")
        log("✅ ${files.size} টি ফাইল পাওয়া গেছে (${humanSize(files.sumOf { it.size })})")

        // ২. রিপো ও ব্রাঞ্চ
        val (baseCommit, createBranch) = resolveBase()

        // ৩. blobs
        log("⬆️ ফাইল আপলোড হচ্ছে...")
        val shas = arrayOfNulls<String>(files.size)
        var done = 0
        progress(0, files.size)
        val small = files.indices.filter { files[it].size <= SMALL }
        val large = files.indices.filter { files[it].size > SMALL }
        val sem = Semaphore(3)
        coroutineScope {
            small.map { i ->
                async {
                    sem.withPermit {
                        shas[i] = uploadBlob(files[i])
                        progress(++done, files.size)
                    }
                }
            }.awaitAll()
        }
        for (i in large) {
            log("📦 বড় ফাইল: ${files[i].repoPath}")
            shas[i] = uploadBlob(files[i])
            progress(++done, files.size)
        }

        // ৪. tree
        log("🌳 ফোল্ডার-কাঠামো (tree) তৈরি হচ্ছে...")
        val baseTree = call("GET", "$api/git/commits/$baseCommit")
            .ok("বেস কমিট").obj().getJSONObject("tree").getString("sha")
        val entries = JSONArray()
        files.forEachIndexed { i, lf ->
            entries.put(
                JSONObject()
                    .put("path", lf.repoPath)
                    .put("mode", modeOf(lf.repoPath))
                    .put("type", "blob")
                    .put("sha", shas[i]!!)
            )
        }
        val newTree = call(
            "POST", "$api/git/trees",
            JSONObject().put("base_tree", baseTree).put("tree", entries).toString()
        ).ok("ট্রি তৈরি").obj().getString("sha")

        if (newTree == baseTree) {
            log("ℹ️ কোনো পরিবর্তন নেই — রিপোতে সব ফাইল আগে থেকেই হুবহু এক")
            return "https://github.com/${cfg.owner}/${cfg.repo}/tree/${cfg.branch}"
        }

        // ৫. commit
        log("📝 কমিট তৈরি হচ্ছে...")
        val commitSha = call(
            "POST", "$api/git/commits",
            JSONObject()
                .put("message", cfg.message)
                .put("tree", newTree)
                .put("parents", JSONArray().put(baseCommit))
                .toString()
        ).ok("কমিট তৈরি").obj().getString("sha")

        // ৬. ব্রাঞ্চ আপডেট / তৈরি
        if (createBranch) {
            call(
                "POST", "$api/git/refs",
                JSONObject().put("ref", "refs/heads/${cfg.branch}").put("sha", commitSha).toString()
            ).ok("ব্রাঞ্চ তৈরি")
        } else {
            call(
                "PATCH", "$api/git/refs/heads/${encodeBranch(cfg.branch)}",
                JSONObject().put("sha", commitSha).put("force", false).toString()
            ).ok("ব্রাঞ্চ আপডেট")
        }
        log("🎉 সম্পন্ন! ${files.size} টি ফাইল এক কমিটে push হয়েছে (${commitSha.take(7)})")
        return "https://github.com/${cfg.owner}/${cfg.repo}/commit/$commitSha"
    }

    /** (বেস কমিটের SHA, নতুন ব্রাঞ্চ বানাতে হবে কিনা) */
    private suspend fun resolveBase(): Pair<String, Boolean> {
        var repoResp = call("GET", api)
        if (repoResp.code == 401) throw GitHubException("টোকেন ভুল বা মেয়াদোত্তীর্ণ")
        if (repoResp.code == 404) {
            if (!cfg.createRepo) {
                throw GitHubException(
                    "রিপোজিটরি পাওয়া যায়নি, অথবা টোকেনের এই রিপোতে অ্যাক্সেস নেই " +
                        "(নতুন বানাতে 'রিপো না থাকলে বানাও' অন করুন)"
                )
            }
            log("🆕 রিপো নেই, বানানো হচ্ছে...")
            createRepo()
            repoResp = call("GET", api)
        }
        val repoObj = repoResp.ok("রিপো তথ্য").obj()
        val perms = repoObj.optJSONObject("permissions")
        if (perms != null && !perms.optBoolean("push", false)) {
            throw GitHubException("এই টোকেনে এই রিপোতে push (write) অনুমতি নেই")
        }
        val default = repoObj.optString("default_branch", "main")

        getRefSha(cfg.branch)?.let { return it to false }

        var def = getRefSha(default)
        if (def == null) {
            log("🆕 রিপো খালি, প্রথম কমিট দিয়ে চালু করা হচ্ছে...")
            initEmptyRepo(default)
            def = getRefSha(default) ?: throw GitHubException("খালি রিপো চালু করা যায়নি")
            if (cfg.branch == default) return def to false
        }
        log("🌿 ব্রাঞ্চ '${cfg.branch}' নেই — নতুন বানানো হবে")
        return def to true
    }

    private suspend fun createRepo() {
        val me = call("GET", "https://api.github.com/user").ok("ইউজার তথ্য").obj().getString("login")
        val url = if (me.equals(cfg.owner, ignoreCase = true)) {
            "https://api.github.com/user/repos"
        } else {
            "https://api.github.com/orgs/${cfg.owner}/repos"
        }
        call(
            "POST", url,
            JSONObject().put("name", cfg.repo).put("private", cfg.privateRepo).toString()
        ).ok("রিপো তৈরি")
        log("✅ রিপো তৈরি হয়েছে (${if (cfg.privateRepo) "private" else "public"})")
    }

    private suspend fun getRefSha(branch: String): String? {
        val r = call("GET", "$api/git/ref/heads/${encodeBranch(branch)}")
        return when (r.code) {
            200 -> r.obj().getJSONObject("object").getString("sha")
            404, 409 -> null // 409 = খালি রিপো
            else -> {
                r.ok("ব্রাঞ্চ তথ্য")
                null
            }
        }
    }

    private suspend fun initEmptyRepo(branch: String) {
        val content = Base64.encodeToString("# ${cfg.repo}\n".toByteArray(), Base64.NO_WRAP)
        call(
            "PUT", "$api/contents/README.md",
            JSONObject()
                .put("message", "Initial commit")
                .put("content", content)
                .put("branch", branch)
                .toString()
        ).ok("খালি রিপো চালু")
    }

    private suspend fun uploadBlob(lf: LocalFile): String {
        val b64 = withContext(Dispatchers.IO) {
            val bytes = resolver.openInputStream(lf.doc.uri)?.use { it.readBytes() }
                ?: throw GitHubException("ফাইল পড়া যায়নি: ${lf.repoPath}")
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
        // base64-এ এস্কেপ করার মতো কোনো ক্যারেক্টার নেই, তাই সরাসরি বসানো নিরাপদ
        val body = "{\"encoding\":\"base64\",\"content\":\"$b64\"}"
        return call("POST", "$api/git/blobs", body)
            .ok("আপলোড (${lf.repoPath})").obj().getString("sha")
    }

    private fun modeOf(path: String): String {
        val n = path.substringAfterLast('/')
        return if (n == "gradlew" || n.endsWith(".sh")) "100755" else "100644"
    }

    private fun encodeBranch(b: String) = b.split("/").joinToString("/") { Uri.encode(it) }
}
