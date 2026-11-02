package com.example.gitfolderuploader

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** অ্যাপের ভাষা (বাংলা/ইংরেজি) ও থিম মোড (০ = ফোনের মতো, ১ = লাইট, ২ = ডার্ক) */
object Lang {
    var english by mutableStateOf(false)
    var themeMode by mutableStateOf(0)

    fun load(ctx: Context) {
        val p = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
        english = p.getBoolean("english", false)
        themeMode = p.getInt("themeMode", 0)
    }

    fun setEnglish(ctx: Context, v: Boolean) {
        english = v
        ctx.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("english", v).apply()
    }

    fun setTheme(ctx: Context, m: Int) {
        themeMode = m
        ctx.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("themeMode", m).apply()
    }
}

/** বাংলা লেখাকে ইংরেজিতে বদলায় (ইংরেজি মোডে) */
fun tr(bn: String): String = if (Lang.english) (EN_MAP[bn] ?: bn) else bn

/** ইন্টারপোলেশনসহ লেখার জন্য: দুই ভাষার লেখা সরাসরি দেওয়া */
fun L(bn: String, en: String): String = if (Lang.english) en else bn

@Composable
fun isDark(): Boolean = when (Lang.themeMode) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

private val EN_MAP: Map<String, String> = mapOf(
    "আপনার ফোল্ডার, সোজা GitHub-এ" to "Your folder, straight to GitHub",
    "এখনো লগইন করা নেই" to "Not logged in yet",
    "আপলোড" to "Upload",
    "বিল্ড ও APK" to "Builds & APK",
    "টার্মিনাল" to "Terminal",
    "বিল্ড (GitHub Actions)" to "Builds (GitHub Actions)",
    "আপলোড ট্যাবের ধাপ ২-এ রিপো দিন" to "Enter a repo in step 2 of the Upload tab",
    "আনা হচ্ছে…" to "Loading…",
    "রিফ্রেশ" to "Refresh",
    "পুরনো বিল্ড পরিষ্কার" to "Clean old builds",
    "কোনো বিল্ড পাওয়া যায়নি" to "No builds found",
    "অপেক্ষায়" to "Queued",
    "চলছে" to "Running",
    "সফল" to "Success",
    "বাতিল" to "Cancelled",
    "ব্যর্থ" to "Failed",
    "চলছে…" to "Running…",
    "APK নামান ও ইনস্টল" to "Download & install APK",
    "আবার চালান" to "Run again",
    "বিল্ড মুছবেন?" to "Delete build?",
    "মুছুন" to "Delete",
    "ডাউনলোড করা APK" to "Downloaded APKs",
    "ফোনে সংরক্ষিত — ইনস্টল বা মুছে ফেলুন" to "Saved on phone — install or delete",
    "কোনো APK নেই" to "No APKs",
    "ইনস্টল" to "Install",
    "APK মুছবেন?" to "Delete APK?",
    "সব APK মুছবেন?" to "Delete all APKs?",
    "সব APK মুছুন" to "Delete all APKs",
    "টোকেনে Actions অনুমতি লাগবে: Classic → repo  ·  Fine-grained → Actions: Read and write" to "Token needs Actions permission: Classic → repo  ·  Fine-grained → Actions: Read and write",
    "হ্যাঁ, মুছুন" to "Yes, delete",
    "না" to "No",
    "রিপো বাছাই করুন" to "Choose a repo",
    "বন্ধ" to "Close",
    "রিপো খুঁজুন" to "Search repos",
    "তালিকা আনা হচ্ছে…" to "Loading list…",
    "এই টোকেনে push করার মতো কোনো রিপো পাওয়া যায়নি" to "No pushable repos found for this token",
    "কিছু মেলেনি" to "No matches",
    "লগআউট করবেন?" to "Log out?",
    "সব সেভ করা তথ্য মুছবেন?" to "Delete all saved data?",
    "টোকেন এই ফোন থেকে মুছে যাবে। রিপো ও ফোল্ডারের তথ্য থাকবে। আপনার GitHub অ্যাকাউন্ট বা রিপোতে কিছু বদলাবে না।" to "The token will be removed from this phone. Repo and folder info stay. Nothing changes on your GitHub account or repos.",
    "টোকেন, রিপো, ব্রাঞ্চ ও বাছাই করা ফোল্ডারের তথ্য এই ফোন থেকে মুছে যাবে। GitHub-এ কিছু বদলাবে না।" to "Token, repo, branch and chosen folder info will be removed from this phone. Nothing changes on GitHub.",
    "১. অ্যাকাউন্ট" to "1. Account",
    "GitHub Access Token দিয়ে সংযুক্ত হন" to "Connect with a GitHub Access Token",
    "লুকান" to "Hide",
    "দেখুন" to "Show",
    "যাচাই করুন" to "Verify",
    "যাচাই হচ্ছে…" to "Verifying…",
    "টোকেন এনক্রিপ্ট করে মনে রাখো" to "Remember token (encrypted)",
    "আপলোড শেষে নিজে লগআউট" to "Auto logout after upload",
    "সফল push-এর পর টোকেন ফোন থেকে মুছে যাবে" to "Token is removed from the phone after a successful push",
    "লগআউট (টোকেন মুছুন)" to "Log out (remove token)",
    "Classic: repo (প্রয়োজনে workflow)   ·   Fine-grained: Contents → Read and write" to "Classic: repo (+ workflow if needed)   ·   Fine-grained: Contents → Read and write",
    "২. রিপোজিটরি" to "2. Repository",
    "কোথায় push হবে" to "Where to push",
    "আগে টোকেন দিন" to "Enter a token first",
    "আমার রিপোগুলো থেকে বাছাই করুন" to "Choose from my repos",
    "রিপো (owner/repo বা লিংক)" to "Repo (owner/repo or link)",
    "তালিকা থেকে বাছাই করুন, অথবা নতুন রিপোর নাম হাতে লিখুন" to "Pick from the list, or type a new repo name",
    "ব্রাঞ্চ" to "Branch",
    "কমিট মেসেজ" to "Commit message",
    "রিপো না থাকলে বানাও" to "Create repo if missing",
    "টোকেনে রিপো তৈরির অনুমতি লাগবে (Classic: repo)" to "Token needs repo-creation permission (Classic: repo)",
    "প্রাইভেট রিপো" to "Private repo",
    "৩. ফোল্ডার" to "3. Folder",
    "যে প্রজেক্ট আপলোড করবেন" to "The project to upload",
    "ফোল্ডার বাছাই করুন" to "Choose folder",
    "স্ক্যান হচ্ছে…" to "Scanning…",
    "ফাইল" to "Files",
    "মোট সাইজ" to "Total size",
    "বাদ (বড়)" to "Skipped (large)",
    "ফাইলের তালিকা লুকান" to "Hide file list",
    "ফাইলের তালিকা দেখুন" to "Show file list",
    "অ্যাডভান্সড সেটিংস" to "Advanced settings",
    "রিপোর ভেতরের সাবফোল্ডার (ঐচ্ছিক)" to "Subfolder in repo (optional)",
    "ফাঁকা রাখলে রিপোর রুটে যাবে" to "Leave empty to upload to the repo root",
    "বাদ দেওয়া নাম (কমা দিয়ে)" to "Excluded names (comma separated)",
    "*.apk এর মতো এক্সটেনশনও দেওয়া যায়" to "Extensions like *.apk also work",
    "আবার স্ক্যান" to "Rescan",
    "Download বা স্টোরেজের মূল ফোল্ডার বাছাই করা যায় না। তার ভেতরের প্রজেক্ট ফোল্ডারটি বাছাই করুন।" to "You can't pick Download or the storage root. Pick the project folder inside it.",
    "৪. Push" to "4. Push",
    "এক কমিটে সব ফাইল · অ্যাপ বন্ধ করলেও চলবে" to "All files in one commit · keeps running if you close the app",
    "বাতিল করুন" to "Cancel",
    "টোকেন, রিপো ও ফোল্ডার দিলে বাটন চালু হবে" to "The button turns on once token, repo and folder are set",
    "কাজ শেষ!" to "Done!",
    "এখন GitHub-এ গিয়ে দেখে নিতে পারেন" to "You can now check it on GitHub",
    "GitHub-এ দেখুন" to "View on GitHub",
    "সব সেভ করা তথ্য মুছুন" to "Delete all saved data",
    "workflows ফোল্ডার বাদ দিন" to "Skip workflows folder",
    ".github/workflows আপলোড হবে না (টোকেনে workflow অনুমতি না থাকলে কাজে লাগে)" to ".github/workflows won't be uploaded (useful if the token lacks workflow permission)",
    "ভাষা ও থিম" to "Language & theme",
    "অ্যাপের ভাষা এবং রং বাছাই করুন" to "Choose the app language and colours",
    "+ নতুন অ্যাকাউন্ট" to "+ New account",
    "ফোনের মতো" to "System",
    "লাইট" to "Light",
    "ডার্ক" to "Dark",
    "সেটিংস" to "Settings",
    "ফিরে যান" to "Back",
    "অ্যাকাউন্ট ও নিরাপত্তা" to "Account & security",
    "টোকেন সংরক্ষণ ও লগআউটের নিয়ম" to "Token storage and logout rules",
    "রিপো তৈরি" to "Repo creation",
    "রিপো না থাকলে কী হবে" to "What if the repo is missing",
    "ফোল্ডার ও ফিল্টার" to "Folder & filters",
    "কোন ফাইল যাবে আর কোথায় যাবে" to "Which files go, and where",
)
