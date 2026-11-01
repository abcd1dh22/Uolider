package com.example.gitfolderuploader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

data class RunInfo(
    val id: Long,
    val title: String,
    val number: Int,
    val status: String,        // queued / in_progress / completed
    val conclusion: String?,   // success / failure / cancelled ...
    val branch: String,
    val createdAt: String,
)

/** GitHub Actions: বিল্ড তালিকা, APK ডাউনলোড, রান মোছা, আবার চালানো */
class ActionsApi(private val token: String, owner: String, repo: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    private val base = "https://api.github.com/repos/$owner/$repo"

    private fun builder(url: String) = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")

    private suspend fun send(req: Request, what: String): String = withContext(Dispatchers.IO) {
        try {
            client.newCall(req).execute().use { r ->
                val body = r.body?.string().orEmpty()
                if (r.code !in 200..299) {
                    val msg = runCatching { JSONObject(body).optString("message") }.getOrDefault("")
                    throw GitHubException("$what ব্যর্থ (${r.code}): $msg", r.code)
                }
                body
            }
        } catch (e: GitHubException) {
            throw e
        } catch (e: IOException) {
            throw GitHubException("নেটওয়ার্ক সমস্যা: ${e.message}")
        }
    }

    suspend fun runs(): List<RunInfo> {
        val body = send(builder("$base/actions/runs?per_page=20").build(), "বিল্ড তালিকা")
        val arr = JSONObject(body).getJSONArray("workflow_runs")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RunInfo(
                id = o.getLong("id"),
                title = o.optString("display_title").ifBlank { o.optString("name") },
                number = o.optInt("run_number"),
                status = o.optString("status"),
                conclusion = if (o.isNull("conclusion")) null else o.optString("conclusion"),
                branch = o.optString("head_branch"),
                createdAt = o.optString("created_at"),
            )
        }
    }

    suspend fun deleteRun(id: Long) {
        send(builder("$base/actions/runs/$id").delete().build(), "বিল্ড মোছা")
    }

    suspend fun rerun(id: Long) {
        send(builder("$base/actions/runs/$id/rerun").post("".toRequestBody()).build(), "আবার চালানো")
    }

    /** রানের আর্টিফ্যাক্ট (ZIP) নামিয়ে ভেতর থেকে .apk বের করে outDir-এ রাখে */
    suspend fun downloadApk(runId: Long, label: String, outDir: File, tmpDir: File): File {
        val list = send(builder("$base/actions/runs/$runId/artifacts").build(), "আর্টিফ্যাক্ট তালিকা")
        val arr = JSONObject(list).getJSONArray("artifacts")
        var artifactId = -1L
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (!o.optBoolean("expired", false)) {
                artifactId = o.getLong("id")
                break
            }
        }
        if (artifactId < 0) {
            throw GitHubException("এই বিল্ডে APK আর্টিফ্যাক্ট নেই (মেয়াদ শেষ বা মুছে ফেলা হয়েছে)")
        }

        return withContext(Dispatchers.IO) {
            val zip = File(tmpDir, "artifact-$artifactId.zip")
            try {
                client.newCall(builder("$base/actions/artifacts/$artifactId/zip").build()).execute().use { r ->
                    if (r.code !in 200..299) throw GitHubException("ডাউনলোড ব্যর্থ (${r.code})", r.code)
                    r.body!!.byteStream().use { input -> zip.outputStream().use { input.copyTo(it) } }
                }
                var apk: File? = null
                ZipInputStream(zip.inputStream().buffered()).use { zin ->
                    var entry = zin.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.endsWith(".apk")) {
                            val f = File(outDir, "$label-${entry.name.substringAfterLast('/')}")
                            f.outputStream().use { zin.copyTo(it) }
                            apk = f
                            break
                        }
                        entry = zin.nextEntry
                    }
                }
                apk ?: throw GitHubException("ZIP-এর ভেতরে APK পাওয়া যায়নি")
            } catch (e: GitHubException) {
                throw e
            } catch (e: IOException) {
                throw GitHubException("নেটওয়ার্ক/ফাইল সমস্যা: ${e.message}")
            } finally {
                zip.delete()
            }
        }
    }
}

/** ISO সময় থেকে "৫ মিনিট আগে" ধরনের লেখা */
internal fun timeAgo(iso: String): String {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        val t = fmt.parse(iso)?.time ?: return ""
        val m = (System.currentTimeMillis() - t) / 60000
        when {
            m < 1 -> "এইমাত্র"
            m < 60 -> "$m মিনিট আগে"
            m < 1440 -> "${m / 60} ঘণ্টা আগে"
            else -> "${m / 1440} দিন আগে"
        }
    } catch (e: Exception) {
        ""
    }
}
