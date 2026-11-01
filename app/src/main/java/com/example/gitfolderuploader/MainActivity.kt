package com.example.gitfolderuploader

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme { Screen() }
        }
    }
}

// ───────────────────────── ছোট ছোট UI অংশ ─────────────────────────

@Composable
private fun Pill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Text(
            text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    subtitle: String?,
    done: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outline),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(if (done) successColor() else cs.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (done) "✓" else "$number",
                        color = if (done) Color.White else cs.onPrimaryContainer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean = true,
    password: Boolean = false,
    visible: Boolean = true,
    hint: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = cs.primary,
            unfocusedBorderColor = cs.outlineVariant,
            focusedContainerColor = cs.background,
            unfocusedContainerColor = cs.background,
            disabledContainerColor = cs.background,
        ),
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = trailing,
        supportingText = if (hint != null) {
            { Text(hint) }
        } else null,
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun RowScope.Stat(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.weight(1f),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Terminal(prompt: String, lines: List<String>) {
    Surface(color = TermBg, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(prompt, color = TermDim, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            lines.takeLast(150).forEach { l ->
                val (mark, color) = when {
                    l.startsWith("❌") -> "✗" to TermErr
                    l.startsWith("⚠") || l.startsWith("⏳") || l.startsWith("🔁") -> "!" to TermWarn
                    l.startsWith("🎉") || l.startsWith("✅") -> "✓" to TermOk
                    l.startsWith("ℹ") -> "i" to TermDim
                    else -> "›" to TermFg
                }
                // শুরুর ইমোজি বাদ দিয়ে পরিচ্ছন্ন টেক্সট দেখানো
                val clean = l.dropWhile { !it.isLetterOrDigit() || it == 'ℹ' }
                Text(
                    "$mark $clean",
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

// ───────────────────────── মূল স্ক্রিন ─────────────────────────

private class ConfirmAction(val title: String, val text: String, val onYes: () -> Unit)

@Composable
private fun SectionCard(title: String, subtitle: String?, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outline),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

private fun installApk(ctx: Context, file: File) {
    if (Build.VERSION.SDK_INT >= 26 && !ctx.packageManager.canRequestPackageInstalls()) {
        // প্রথমবার: এই অ্যাপকে ইনস্টলের অনুমতি দিতে সেটিংস খুলবে
        val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return
    }
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val i = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(i)
}

@Composable
private fun BuildsTab(vm: MainViewModel) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val ok = successColor()
    var confirm by remember { mutableStateOf<ConfirmAction?>(null) }
    val repoLabel = vm.parseRepo(vm.repoInput)?.let { "${it.first}/${it.second}" }
    val busy = vm.busyRunId != null

    SectionCard("বিল্ড (GitHub Actions)", repoLabel ?: "আপলোড ট্যাবের ধাপ ২-এ রিপো দিন") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.refreshRuns() },
                enabled = !vm.loadingRuns && !busy,
                shape = MaterialTheme.shapes.medium,
            ) { Text(if (vm.loadingRuns) "আনা হচ্ছে…" else "রিফ্রেশ") }

            val old = vm.runs.count { it.status == "completed" } - 3
            if (old > 0) {
                OutlinedButton(
                    onClick = {
                        confirm = ConfirmAction(
                            "পুরনো বিল্ড পরিষ্কার",
                            "সবচেয়ে নতুন ৩টি রেখে বাকি $old টি বিল্ড GitHub থেকে মুছে যাবে।",
                        ) { vm.cleanOldRuns() }
                    },
                    enabled = !busy,
                    shape = MaterialTheme.shapes.medium,
                ) { Text("পুরনো পরিষ্কার ($old)") }
            }
        }

        vm.runsError?.let { Text(it, color = cs.error, style = MaterialTheme.typography.bodySmall) }
        if (vm.runs.isEmpty() && !vm.loadingRuns && vm.runsError == null) {
            Text("কোনো বিল্ড পাওয়া যায়নি", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        }

        vm.runs.take(10).forEach { r ->
            val (label, color) = when {
                r.status != "completed" ->
                    (if (r.status == "queued") "অপেক্ষায়" else "চলছে") to cs.primary
                r.conclusion == "success" -> "সফল" to ok
                r.conclusion == "cancelled" -> "বাতিল" to cs.onSurfaceVariant
                else -> "ব্যর্থ" to cs.error
            }
            Surface(
                color = cs.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            r.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                        )
                        Pill(label, color)
                    }
                    Text(
                        "#${r.number}  ·  ${r.branch}  ·  ${timeAgo(r.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (vm.busyRunId == r.id) {
                            Text("চলছে…", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        } else {
                            if (r.status == "completed" && r.conclusion == "success") {
                                Button(
                                    onClick = { vm.downloadApk(r) { f -> installApk(ctx, f) } },
                                    enabled = !busy,
                                    shape = MaterialTheme.shapes.medium,
                                ) { Text("APK নামান ও ইনস্টল") }
                            }
                            if (r.status == "completed" && r.conclusion != "success") {
                                OutlinedButton(
                                    onClick = { vm.rerun(r) },
                                    enabled = !busy,
                                    shape = MaterialTheme.shapes.medium,
                                ) { Text("আবার চালান") }
                            }
                            if (r.status == "completed") {
                                TextButton(
                                    onClick = {
                                        confirm = ConfirmAction(
                                            "বিল্ড মুছবেন?",
                                            "বিল্ড #${r.number} ও তার APK আর্টিফ্যাক্ট GitHub থেকে মুছে যাবে।",
                                        ) { vm.deleteRun(r) }
                                    },
                                    enabled = !busy,
                                ) { Text("মুছুন", color = cs.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    SectionCard("ডাউনলোড করা APK", "ফোনে সংরক্ষিত — ইনস্টল বা মুছে ফেলুন") {
        if (vm.apkFiles.isEmpty()) {
            Text("কোনো APK নেই", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        }
        vm.apkFiles.forEach { f ->
            Surface(
                color = cs.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(f.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(
                            humanSize(f.length()),
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { installApk(ctx, f) }) { Text("ইনস্টল") }
                    TextButton(
                        onClick = {
                            confirm = ConfirmAction("APK মুছবেন?", "${f.name} ফোন থেকে মুছে যাবে।") { vm.deleteApk(f) }
                        },
                    ) { Text("মুছুন", color = cs.error) }
                }
            }
        }
        if (vm.apkFiles.size > 1) {
            OutlinedButton(
                onClick = {
                    confirm = ConfirmAction(
                        "সব APK মুছবেন?",
                        "ফোনে রাখা ${vm.apkFiles.size} টি APK-ই মুছে যাবে।",
                    ) { vm.deleteAllApks() }
                },
                shape = MaterialTheme.shapes.medium,
            ) { Text("সব মুছুন") }
        }
    }

    vm.buildMsg?.let {
        Text(
            it,
            color = if (vm.buildMsgError) cs.error else ok,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Text(
        "টোকেনে Actions অনুমতি লাগবে: Classic → repo  ·  Fine-grained → Actions: Read and write",
        style = MaterialTheme.typography.bodySmall,
        color = cs.onSurfaceVariant,
    )

    confirm?.let { c ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(c.title) },
            text = { Text(c.text) },
            confirmButton = {
                TextButton(onClick = {
                    c.onYes()
                    confirm = null
                }) { Text("হ্যাঁ, মুছুন", color = cs.error) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("না") } },
        )
    }
}

@Composable
private fun RepoPicker(vm: MainViewModel, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ok = successColor()
    var query by remember { mutableStateOf("") }
    val list = vm.repos.filter { it.fullName.contains(query.trim(), ignoreCase = true) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = cs.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = 560.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("রিপো বাছাই করুন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row {
                        TextButton(onClick = { vm.loadRepos(force = true) }, enabled = !vm.loadingRepos) { Text("রিফ্রেশ") }
                        TextButton(onClick = onDismiss) { Text("বন্ধ") }
                    }
                }
                Field("রিপো খুঁজুন", query, { query = it })
                when {
                    vm.loadingRepos -> Text(
                        "তালিকা আনা হচ্ছে…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                    vm.reposError != null -> Text(
                        vm.reposError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.error,
                    )
                    list.isEmpty() -> Text(
                        if (vm.repos.isEmpty()) "এই টোকেনে push করার মতো কোনো রিপো পাওয়া যায়নি" else "কিছু মেলেনি",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                    else -> {
                        Text(
                            "${list.size} টি রিপো",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                        LazyColumn(Modifier.weight(1f, fill = false)) {
                            items(list, key = { it.fullName }) { r ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            vm.pickRepo(r)
                                            onDismiss()
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(r.fullName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text(
                                            "ডিফল্ট ব্রাঞ্চ: ${r.defaultBranch}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = cs.onSurfaceVariant,
                                        )
                                    }
                                    if (r.isPrivate) Pill("private", cs.onSurfaceVariant) else Pill("public", ok)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Screen(vm: MainViewModel = viewModel()) {
    val ctx = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val cs = MaterialTheme.colorScheme
    val ok = successColor()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.onFolderPicked(uri)
    }

    // push চলার সময় স্ক্রিন জাগিয়ে রাখা
    DisposableEffect(vm.running) {
        val window = (ctx as? Activity)?.window
        if (vm.running) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = cs.background) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // হেডার
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Git Folder Uploader",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "পুরো ফোল্ডার, এক কমিটে GitHub-এ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                }
                val acc = vm.account
                if (acc != null) Pill("@$acc", ok) else Pill("সংযুক্ত নয়", cs.onSurfaceVariant)
            }

            // ট্যাব: আপলোড | বিল্ড ও APK
            var tab by remember { mutableStateOf(0) }
            val hasActive = vm.runs.any { it.status != "completed" }
            LaunchedEffect(tab) {
                if (tab == 1) {
                    vm.refreshRuns()
                    vm.loadApkFiles()
                }
            }
            // চলমান বিল্ড থাকলে প্রতি ৮ সেকেন্ডে নিজে থেকে রিফ্রেশ
            LaunchedEffect(tab, hasActive) {
                if (tab == 1 && hasActive) {
                    while (true) {
                        delay(8000)
                        vm.refreshRuns(silent = true)
                    }
                }
            }
            TabRow(
                selectedTabIndex = tab,
                containerColor = cs.background,
                contentColor = cs.primary,
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("আপলোড") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("বিল্ড ও APK") })
            }

            // লগআউট / সেভ করা তথ্য মোছার নিশ্চিতকরণ (১ = লগআউট, ২ = সব মুছুন)
            var logoutMode by remember { mutableStateOf(0) }
            if (logoutMode != 0) {
                AlertDialog(
                    onDismissRequest = { logoutMode = 0 },
                    title = { Text(if (logoutMode == 1) "লগআউট করবেন?" else "সব সেভ করা তথ্য মুছবেন?") },
                    text = {
                        Text(
                            if (logoutMode == 1) {
                                "টোকেন এই ফোন থেকে মুছে যাবে। রিপো ও ফোল্ডারের তথ্য থাকবে। আপনার GitHub অ্যাকাউন্ট বা রিপোতে কিছু বদলাবে না।"
                            } else {
                                "টোকেন, রিপো, ব্রাঞ্চ ও বাছাই করা ফোল্ডারের তথ্য এই ফোন থেকে মুছে যাবে। GitHub-এ কিছু বদলাবে না।"
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.logout(clearAll = logoutMode == 2)
                            logoutMode = 0
                        }) { Text("হ্যাঁ, মুছুন", color = cs.error) }
                    },
                    dismissButton = { TextButton(onClick = { logoutMode = 0 }) { Text("না") } },
                )
            }

            if (tab == 0) {
            // ধাপ ১ — অ্যাকাউন্ট
            StepCard(1, "অ্যাকাউন্ট", "GitHub Personal Access Token", vm.account != null) {
                var visible by remember { mutableStateOf(false) }
                Field(
                    "Access Token", vm.token, vm::onTokenChange,
                    enabled = !vm.running, password = true, visible = visible,
                    trailing = {
                        TextButton(onClick = { visible = !visible }) { Text(if (visible) "লুকান" else "দেখুন") }
                    },
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = vm::verifyToken,
                        enabled = vm.token.isNotBlank() && !vm.verifying && !vm.running,
                        shape = MaterialTheme.shapes.medium,
                    ) { Text("যাচাই করুন") }
                    when {
                        vm.verifying -> Text("যাচাই হচ্ছে…", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        vm.account != null -> Text(
                            "@${vm.account} হিসেবে সংযুক্ত",
                            color = ok,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                        vm.tokenError != null -> Text(
                            vm.tokenError ?: "",
                            color = cs.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vm.rememberToken, onCheckedChange = { vm.rememberToken = it }, enabled = !vm.running)
                    Text("টোকেন এনক্রিপ্ট করে মনে রাখো", style = MaterialTheme.typography.bodyMedium)
                }
                SwitchRow(
                    "আপলোড শেষে নিজে লগআউট",
                    "সফল push-এর পর টোকেন ফোন থেকে মুছে যাবে",
                    vm.clearAfterPush, !vm.running,
                ) { vm.clearAfterPush = it }
                if (vm.token.isNotBlank()) {
                    TextButton(onClick = { logoutMode = 1 }, enabled = !vm.running) {
                        Text("লগআউট (টোকেন মুছুন)", color = cs.error)
                    }
                }
                Text(
                    "Classic: repo (প্রয়োজনে workflow)   ·   Fine-grained: Contents → Read and write",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }

            // ধাপ ২ — রিপোজিটরি
            StepCard(
                2, "রিপোজিটরি", "কোথায় push হবে",
                vm.parseRepo(vm.repoInput) != null && vm.branch.isNotBlank(),
            ) {
                var showPicker by remember { mutableStateOf(false) }
                FilledTonalButton(
                    onClick = {
                        showPicker = true
                        vm.loadRepos()
                    },
                    enabled = vm.token.isNotBlank() && !vm.running,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(
                        if (vm.token.isBlank()) "রিপো বাছাই (আগে টোকেন দিন)" else "আমার রিপোগুলো থেকে বাছাই করুন",
                        fontWeight = FontWeight.Medium,
                    )
                }
                Field(
                    "রিপো (owner/repo বা লিংক)", vm.repoInput, { vm.repoInput = it },
                    enabled = !vm.running, hint = "তালিকা থেকে বাছাই করুন, অথবা নতুন রিপোর নাম হাতে লিখুন",
                )
                if (showPicker) RepoPicker(vm) { showPicker = false }
                Field("ব্রাঞ্চ", vm.branch, { vm.branch = it }, enabled = !vm.running)
                Field("কমিট মেসেজ", vm.message, { vm.message = it }, enabled = !vm.running)
                SwitchRow(
                    "রিপো না থাকলে বানাও",
                    "টোকেনে রিপো তৈরির অনুমতি লাগবে (Classic: repo)",
                    vm.createRepo, !vm.running,
                ) { vm.createRepo = it }
                if (vm.createRepo) {
                    SwitchRow("প্রাইভেট রিপো", null, vm.privateRepo, !vm.running) { vm.privateRepo = it }
                }
            }

            // ধাপ ৩ — ফোল্ডার
            StepCard(3, "ফোল্ডার", "যে প্রজেক্ট আপলোড করবেন", vm.folderUri != null) {
                var showPreview by remember { mutableStateOf(false) }
                var advanced by remember { mutableStateOf(false) }

                FilledTonalButton(
                    onClick = { picker.launch(null) },
                    enabled = !vm.running,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Text(
                        if (vm.folderName.isEmpty()) "ফোল্ডার বাছাই করুন" else "${vm.folderName}  ·  বদলান",
                        fontWeight = FontWeight.Medium,
                    )
                }

                if (vm.scanning) {
                    Text("স্ক্যান হচ্ছে…", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
                vm.scanInfo?.let { info ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Stat("ফাইল", "${info.count}")
                        Stat("মোট সাইজ", humanSize(info.bytes))
                        Stat("বাদ (বড়)", "${info.skipped.size}")
                    }
                    TextButton(onClick = { showPreview = !showPreview }) {
                        Text(if (showPreview) "▾  ফাইলের তালিকা লুকান" else "▸  ফাইলের তালিকা দেখুন")
                    }
                    if (showPreview) {
                        Surface(color = TermBg, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                info.preview.forEach {
                                    Text("+ $it", color = TermOk, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                }
                                if (info.count > info.preview.size) {
                                    Text(
                                        "… আরও ${info.count - info.preview.size} টি ফাইল",
                                        color = TermDim, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                                    )
                                }
                                info.skipped.forEach {
                                    Text("! $it", color = TermWarn, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                TextButton(onClick = { advanced = !advanced }) {
                    Text(if (advanced) "▾  অ্যাডভান্সড" else "▸  অ্যাডভান্সড")
                }
                if (advanced) {
                    Field(
                        "রিপোর ভেতরের সাবফোল্ডার (ঐচ্ছিক)", vm.subDir, { vm.subDir = it },
                        enabled = !vm.running, hint = "ফাঁকা রাখলে রিপোর রুটে যাবে",
                    )
                    Field(
                        "বাদ দেওয়া নাম (কমা দিয়ে)", vm.excludes, { vm.excludes = it },
                        enabled = !vm.running, hint = "*.apk এর মতো এক্সটেনশনও দেওয়া যায়",
                    )
                    OutlinedButton(
                        onClick = vm::scanFolder,
                        enabled = vm.folderUri != null && !vm.scanning && !vm.running,
                        shape = MaterialTheme.shapes.medium,
                    ) { Text("আবার স্ক্যান") }
                }

                Text(
                    "Download বা স্টোরেজের মূল ফোল্ডার বাছাই করা যায় না। তার ভেতরের প্রজেক্ট ফোল্ডারটি বাছাই করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }

            // ধাপ ৪ — Push
            StepCard(4, "Push", "এক কমিটে সব ফাইল", vm.resultUrl != null) {
                if (vm.running) {
                    OutlinedButton(
                        onClick = vm::cancel,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) { Text("বাতিল করুন") }
                } else {
                    Button(
                        onClick = vm::start,
                        enabled = vm.ready,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) { Text("Push to GitHub", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
                    if (!vm.ready) {
                        Text(
                            "টোকেন, রিপো ও ফোল্ডার দিলে বাটন চালু হবে",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }

                if (vm.running || vm.total > 0) {
                    val pct = if (vm.total == 0) 0 else vm.done * 100 / vm.total
                    LinearProgressIndicator(
                        progress = { if (vm.total == 0) 0f else vm.done.toFloat() / vm.total },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = cs.primary,
                        trackColor = cs.surfaceVariant,
                    )
                    Text(
                        "${vm.done} / ${vm.total} ফাইল  ·  $pct%",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }

                if (vm.logs.isNotEmpty()) {
                    Terminal("$ git push origin ${vm.branch}", vm.logs)
                }

                vm.resultUrl?.let { url ->
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(url) },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) { Text("GitHub-এ দেখুন") }
                }
            }
            TextButton(
                onClick = { logoutMode = 2 },
                enabled = !vm.running,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("সব সেভ করা তথ্য মুছুন", color = cs.error) }
            } else {
                BuildsTab(vm)
            }
        }
    }
}
