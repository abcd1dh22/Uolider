package com.example.gitfolderuploader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** সার্ভিস ও স্ক্রিনের মধ্যে আপলোডের অবস্থা ভাগ করার জায়গা */
object UploadHub {
    var running by mutableStateOf(false)
    var done by mutableStateOf(0)
    var total by mutableStateOf(0)
    var resultUrl by mutableStateOf<String?>(null)
    var logoutAfter by mutableStateOf(false)
    val logs = mutableStateListOf<String>()

    @Volatile var pendingCfg: UploadConfig? = null
    @Volatile var pendingUri: Uri? = null
    @Volatile var pendingClearAfter: Boolean = false
    var job: Job? = null
}

/**
 * ফোরগ্রাউন্ড সার্ভিস: অ্যাপ বন্ধ করলে বা ফোন লক হলেও আপলোড চলতে থাকে।
 * চলার সময় প্রগ্রেস নোটিফিকেশন (বাতিল বাটনসহ), শেষে ফলাফল নোটিফিকেশন।
 */
class UploadService : Service() {

    companion object {
        const val ACTION_CANCEL = "com.example.gitfolderuploader.CANCEL"
        private const val CH_PROGRESS = "upload_progress"
        private const val CH_RESULT = "upload_result"
        private const val ID_PROGRESS = 101
        private const val ID_RESULT = 102

        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, UploadService::class.java))
        }

        fun cancel() {
            UploadHub.job?.cancel()
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val nm by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CH_PROGRESS, "আপলোড চলছে", NotificationManager.IMPORTANCE_LOW)
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_RESULT, "আপলোডের ফলাফল", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // নোটিফিকেশনের "বাতিল" বাটন
        if (intent?.action == ACTION_CANCEL) {
            cancel()
            return START_NOT_STICKY
        }

        // startForegroundService-এর পর অবশ্যই দ্রুত startForeground ডাকতে হয়
        val first = progressNotif("শুরু হচ্ছে…", 0, 0)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(ID_PROGRESS, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(ID_PROGRESS, first)
        }

        val cfg = UploadHub.pendingCfg
        val uri = UploadHub.pendingUri
        if (cfg == null || uri == null || UploadHub.job?.isActive == true) {
            if (UploadHub.job?.isActive != true) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            return START_NOT_STICKY
        }
        UploadHub.pendingCfg = null
        UploadHub.pendingUri = null
        val clearAfter = UploadHub.pendingClearAfter

        UploadHub.running = true
        UploadHub.job = scope.launch {
            var ok = false
            try {
                val root = DocumentFile.fromTreeUri(this@UploadService, uri)
                    ?: throw GitHubException("ফোল্ডার খোলা যায়নি, আবার বাছাই করুন")
                var lastPct = -1
                val url = GitHubUploader(
                    resolver = contentResolver,
                    cfg = cfg,
                    log = { UploadHub.logs += it },
                    progress = { d, t ->
                        UploadHub.done = d
                        UploadHub.total = t
                        val pct = if (t == 0) 0 else d * 100 / t
                        if (pct != lastPct) {
                            lastPct = pct
                            nm.notify(ID_PROGRESS, progressNotif("$d / $t ফাইল  ·  $pct%", d, t))
                        }
                    },
                ).run(root)
                UploadHub.resultUrl = url
                ok = true
                postResult(
                    true, "আপলোড সম্পন্ন",
                    "${UploadHub.total} টি ফাইল ${cfg.owner}/${cfg.repo}-এ push হয়েছে", url,
                )
            } catch (e: CancellationException) {
                UploadHub.logs += "⛔ বাতিল করা হয়েছে (যে ফাইলগুলো আপলোড হয়েছে সেগুলো কমিট হয়নি)"
                postResult(false, "আপলোড বাতিল", "${cfg.owner}/${cfg.repo}", null)
                throw e
            } catch (e: Exception) {
                UploadHub.logs += "❌ ${e.message}"
                postResult(false, "আপলোড ব্যর্থ", e.message ?: "অজানা সমস্যা", null)
            } finally {
                UploadHub.running = false
                if (ok && clearAfter) {
                    removeToken()
                    UploadHub.logs += "🔒 টোকেন এই ফোন থেকে মুছে ফেলা হয়েছে (লগআউট)"
                    UploadHub.logoutAfter = true
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        UploadHub.job?.cancel()
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun progressNotif(text: String, done: Int, total: Int): Notification {
        val cancel = PendingIntent.getService(
            this, 1,
            Intent(this, UploadService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val b = NotificationCompat.Builder(this, CH_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("GitHub-এ আপলোড হচ্ছে")
            .setContentText(text)
            .setContentIntent(openApp())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "বাতিল", cancel)
        if (total > 0) b.setProgress(total, done, false) else b.setProgress(0, 0, true)
        return b.build()
    }

    private fun postResult(ok: Boolean, title: String, text: String, url: String?) {
        val b = NotificationCompat.Builder(this, CH_RESULT)
            .setSmallIcon(if (ok) android.R.drawable.stat_sys_upload_done else android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp())
            .setAutoCancel(true)
        if (ok && url != null) {
            val view = PendingIntent.getActivity(
                this, 2, Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                PendingIntent.FLAG_IMMUTABLE,
            )
            b.addAction(0, "GitHub-এ দেখুন", view)
        }
        nm.notify(ID_RESULT, b.build())
    }

    private fun removeToken() {
        try {
            val sp = Accounts.securePrefs(this)
            val t = sp?.getString("token", "") ?: ""
            sp?.edit()?.remove("token")?.apply()
            if (t.isNotBlank()) Accounts.removeByToken(sp, t)
        } catch (_: Exception) {
        }
    }
}
