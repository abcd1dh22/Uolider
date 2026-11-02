package com.example.gitfolderuploader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lang.load(this)
        enableEdgeToEdge()
        setContent {
            AppTheme { Screen() }
        }
    }
}

// ───────────────────────── ছোট ছোট UI অংশ ─────────────────────────

/** বাটনের ভেতরে আইকন + লেখা */
@Composable
private fun BtnLabel(icon: ImageVector, text: String, weight: FontWeight? = null) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(text, fontWeight = weight)
}

@Composable
private fun IconBadge(icon: ImageVector, bg: Color, fg: Color, size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .background(bg, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
private fun Pill(text: String, color: Color, icon: ImageVector? = null) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** উপরের গ্রেডিয়েন্ট হেডার */
@Composable
private fun Hero(account: String?, onSettings: () -> Unit) {
    val brush = Brush.linearGradient(
        if (isDark()) listOf(Color(0xFF1E40AF), Color(0xFF4338CA))
        else listOf(Color(0xFF2563EB), Color(0xFF4F46E5))
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Ic.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                "Git Folder Uploader",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (account != null) Ic.CheckCircle else Ic.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    if (account != null) L("@$account হিসেবে সংযুক্ত", "Connected as @$account") else tr("এখনো লগইন করা নেই"),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                )
            }
        }
        IconButton(
            onClick = onSettings,
            modifier = Modifier.background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
        ) {
            Icon(Ic.Settings, contentDescription = tr("সেটিংস"), tint = Color.White)
        }
    }
}

/** কিছু না থাকলে আইকনসহ ছোট বার্তা */
@Composable
private fun EmptyState(icon: ImageVector, text: String) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = cs.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(32.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** সেটিংস পেজের উপরের বার (পেছনে যাওয়ার বাটনসহ) */
@Composable
private fun SettingsTop(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = onBack) { Icon(Ic.ArrowBack, contentDescription = tr("ফিরে যান")) }
        Text(tr("সেটিংস"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

/** ছোট বাছাই-বাটন সারি (ভাষা/থিমের জন্য) */
@Composable
private fun ChoiceRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surfaceVariant, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEachIndexed { i, label ->
                val sel = selected == i
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) cs.surface else Color.Transparent)
                        .clickable { onSelect(i) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (sel) cs.primary else cs.onSurfaceVariant,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

/** আপলোড | বিল্ড ও APK — আইকনসহ ট্যাব */
@Composable
private fun SegmentedTabs(selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(Ic.CloudUpload to tr("আপলোড"), Ic.Build to tr("বিল্ড ও APK")).forEachIndexed { i, (icon, label) ->
                val sel = selected == i
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) cs.surface else Color.Transparent)
                        .clickable { onSelect(i) }
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (sel) cs.primary else cs.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        label,
                        color = if (sel) cs.primary else cs.onSurfaceVariant,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    done: Boolean,
    summary: String? = null,
    autoCollapse: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val ok = successColor()
    var expanded by remember { mutableStateOf(true) }
    // ধাপ সম্পূর্ণ হলে নিজে গুটিয়ে যায়, অসম্পূর্ণ হলে আবার খোলে
    LaunchedEffect(done) {
        if (autoCollapse) expanded = !done
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outline),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.animateContentSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = summary != null) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBadge(
                    icon = if (done) Ic.CheckCircle else icon,
                    bg = if (done) ok.copy(alpha = 0.14f) else cs.primaryContainer,
                    fg = if (done) ok else cs.onPrimaryContainer,
                )
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val sub = if (!expanded && done && summary != null) summary else subtitle
                    if (sub != null) {
                        Text(
                            sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!expanded && done) ok else cs.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                if (summary != null) {
                    Icon(
                        if (expanded) Ic.ExpandLess else Ic.ExpandMore,
                        contentDescription = null,
                        tint = cs.onSurfaceVariant,
                    )
                }
            }
            if (expanded) content()
        }
    }
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outline),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBadge(icon, cs.primaryContainer, cs.onPrimaryContainer)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
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
    leading: ImageVector? = null,
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
        leadingIcon = if (leading != null) {
            { Icon(leading, contentDescription = null) }
        } else null,
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
private fun androidx.compose.foundation.layout.RowScope.Stat(label: String, value: String) {
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(9.dp).background(Color(0xFFFF5F57), CircleShape))
                Box(Modifier.size(9.dp).background(Color(0xFFFEBC2E), CircleShape))
                Box(Modifier.size(9.dp).background(Color(0xFF28C840), CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(tr("টার্মিনাল"), color = TermDim, fontSize = 11.sp)
            }
            Spacer(Modifier.height(4.dp))
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

// ───────────────────────── বিল্ড ও APK ট্যাব ─────────────────────────

private class ConfirmAction(val title: String, val text: String, val onYes: () -> Unit)

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
    val dangerColors = ButtonDefaults.textButtonColors(contentColor = cs.error)

    SectionCard(Ic.Build, tr("বিল্ড (GitHub Actions)"), repoLabel ?: tr("আপলোড ট্যাবের ধাপ ২-এ রিপো দিন")) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.refreshRuns() },
                enabled = !vm.loadingRuns && !busy,
                shape = MaterialTheme.shapes.medium,
            ) { BtnLabel(Ic.Refresh, if (vm.loadingRuns) tr("আনা হচ্ছে…") else tr("রিফ্রেশ")) }

            val old = vm.runs.count { it.status == "completed" } - 3
            if (old > 0) {
                OutlinedButton(
                    onClick = {
                        confirm = ConfirmAction(
                            tr("পুরনো বিল্ড পরিষ্কার"),
                            L("সবচেয়ে নতুন ৩টি রেখে বাকি $old টি বিল্ড GitHub থেকে মুছে যাবে।","The newest 3 are kept; the other $old builds will be deleted from GitHub."),
                        ) { vm.cleanOldRuns() }
                    },
                    enabled = !busy,
                    shape = MaterialTheme.shapes.medium,
                ) { BtnLabel(Ic.DeleteSweep, L("পুরনো পরিষ্কার ($old)","Clean old ($old)")) }
            }
        }

        vm.runsError?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Ic.Warning, contentDescription = null, tint = cs.error, modifier = Modifier.size(16.dp))
                Text(it, color = cs.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (vm.runs.isEmpty() && !vm.loadingRuns && vm.runsError == null) {
            EmptyState(Ic.Build, tr("কোনো বিল্ড পাওয়া যায়নি"))
        }

        vm.runs.take(10).forEach { r ->
            val running = r.status != "completed"
            val (label, color, runIcon) = when {
                running -> Triple(if (r.status == "queued") tr("অপেক্ষায়") else tr("চলছে"), cs.primary, Ic.Refresh)
                r.conclusion == "success" -> Triple(tr("সফল"), ok, Ic.CheckCircle)
                r.conclusion == "cancelled" -> Triple(tr("বাতিল"), cs.onSurfaceVariant, Ic.Close)
                else -> Triple(tr("ব্যর্থ"), cs.error, Ic.Warning)
            }
            Surface(
                color = cs.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        IconBadge(runIcon, color.copy(alpha = 0.14f), color, size = 36.dp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                r.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "#${r.number}  ·  ${r.branch}  ·  ${timeAgo(r.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                        }
                        Pill(label, color)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (vm.busyRunId == r.id) {
                            Text(tr("চলছে…"), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        } else {
                            if (r.status == "completed" && r.conclusion == "success") {
                                Button(
                                    onClick = { vm.downloadApk(r) { f -> installApk(ctx, f) } },
                                    enabled = !busy,
                                    shape = MaterialTheme.shapes.medium,
                                ) { BtnLabel(Ic.Download, tr("APK নামান ও ইনস্টল")) }
                            }
                            if (r.status == "completed" && r.conclusion != "success") {
                                OutlinedButton(
                                    onClick = { vm.rerun(r) },
                                    enabled = !busy,
                                    shape = MaterialTheme.shapes.medium,
                                ) { BtnLabel(Ic.Replay, tr("আবার চালান")) }
                            }
                            if (r.status == "completed") {
                                TextButton(
                                    onClick = {
                                        confirm = ConfirmAction(
                                            tr("বিল্ড মুছবেন?"),
                                            L("বিল্ড #${r.number} ও তার APK আর্টিফ্যাক্ট GitHub থেকে মুছে যাবে।","Build #${r.number} and its APK artifact will be deleted from GitHub."),
                                        ) { vm.deleteRun(r) }
                                    },
                                    enabled = !busy,
                                    colors = dangerColors,
                                ) { BtnLabel(Ic.Delete, tr("মুছুন")) }
                            }
                        }
                    }
                }
            }
        }
    }

    SectionCard(Ic.SystemUpdate, tr("ডাউনলোড করা APK"), tr("ফোনে সংরক্ষিত — ইনস্টল বা মুছে ফেলুন")) {
        if (vm.apkFiles.isEmpty()) {
            EmptyState(Ic.SystemUpdate, tr("কোনো APK নেই"))
        }
        vm.apkFiles.forEach { f ->
            Surface(
                color = cs.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        IconBadge(Ic.SystemUpdate, cs.primaryContainer, cs.onPrimaryContainer, size = 36.dp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                f.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                humanSize(f.length()),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { installApk(ctx, f) },
                            shape = MaterialTheme.shapes.medium,
                        ) { BtnLabel(Ic.SystemUpdate, tr("ইনস্টল")) }
                        TextButton(
                            onClick = {
                                confirm = ConfirmAction("APK মুছবেন?", L("${f.name} ফোন থেকে মুছে যাবে।","${f.name} will be deleted from this phone.")) { vm.deleteApk(f) }
                            },
                            colors = dangerColors,
                        ) { BtnLabel(Ic.Delete, tr("মুছুন")) }
                    }
                }
            }
        }
        if (vm.apkFiles.size > 1) {
            OutlinedButton(
                onClick = {
                    confirm = ConfirmAction(
                        tr("সব APK মুছবেন?"),
                        L("ফোনে রাখা ${vm.apkFiles.size} টি APK-ই মুছে যাবে।","All ${vm.apkFiles.size} APKs on this phone will be deleted."),
                    ) { vm.deleteAllApks() }
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error),
            ) { BtnLabel(Ic.DeleteSweep, tr("সব APK মুছুন")) }
        }
    }

    vm.buildMsg?.let {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (vm.buildMsgError) Ic.Warning else Ic.CheckCircle,
                contentDescription = null,
                tint = if (vm.buildMsgError) cs.error else ok,
                modifier = Modifier.size(18.dp),
            )
            Text(
                it,
                color = if (vm.buildMsgError) cs.error else ok,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    Text(
        tr("টোকেনে Actions অনুমতি লাগবে: Classic → repo  ·  Fine-grained → Actions: Read and write"),
        style = MaterialTheme.typography.bodySmall,
        color = cs.onSurfaceVariant,
    )

    confirm?.let { c ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            icon = { Icon(Ic.Delete, contentDescription = null, tint = cs.error) },
            title = { Text(c.title) },
            text = { Text(c.text) },
            confirmButton = {
                TextButton(
                    onClick = {
                        c.onYes()
                        confirm = null
                    },
                    colors = dangerColors,
                ) { Text(tr("হ্যাঁ, মুছুন")) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(tr("না")) } },
        )
    }
}

// ───────────────────────── রিপো বাছাইয়ের পপআপ ─────────────────────────

@Composable
private fun RepoPicker(vm: MainViewModel, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val list = vm.repos.filter { it.fullName.contains(query.trim(), ignoreCase = true) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = cs.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = 600.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconBadge(Ic.Book, cs.primaryContainer, cs.onPrimaryContainer, size = 36.dp)
                        Text(tr("রিপো বাছাই করুন"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Row {
                        IconButton(onClick = { vm.loadRepos(force = true) }, enabled = !vm.loadingRepos) {
                            Icon(Ic.Refresh, contentDescription = tr("রিফ্রেশ"))
                        }
                        IconButton(onClick = onDismiss) { Icon(Ic.Close, contentDescription = tr("বন্ধ")) }
                    }
                }
                Field(tr("রিপো খুঁজুন"), query, { query = it }, leading = Ic.Search)
                when {
                    vm.loadingRepos -> Text(
                        tr("তালিকা আনা হচ্ছে…"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                    vm.reposError != null -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Ic.Warning, contentDescription = null, tint = cs.error, modifier = Modifier.size(18.dp))
                        Text(vm.reposError ?: "", style = MaterialTheme.typography.bodyMedium, color = cs.error)
                    }
                    list.isEmpty() -> EmptyState(
                        Ic.Search,
                        if (vm.repos.isEmpty()) tr("এই টোকেনে push করার মতো কোনো রিপো পাওয়া যায়নি") else tr("কিছু মেলেনি"),
                    )
                    else -> {
                        Text(
                            L("${list.size} টি রিপো","${list.size} repos"),
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                        LazyColumn(Modifier.weight(1f, fill = false)) {
                            items(list, key = { it.fullName }) { r ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable {
                                            vm.pickRepo(r)
                                            onDismiss()
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    IconBadge(Ic.Book, cs.surfaceVariant, cs.onSurfaceVariant, size = 36.dp)
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            r.fullName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            L("ডিফল্ট ব্রাঞ্চ: ${r.defaultBranch}","Default branch: ${r.defaultBranch}"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = cs.onSurfaceVariant,
                                        )
                                    }
                                    if (r.isPrivate) Pill("private", cs.onSurfaceVariant, Ic.Lock)
                                    else Pill("public", successColor())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── মূল স্ক্রিন ─────────────────────────

@Composable
private fun Screen(vm: MainViewModel = viewModel()) {
    val ctx = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val cs = MaterialTheme.colorScheme
    val ok = successColor()
    val dangerColors = ButtonDefaults.textButtonColors(contentColor = cs.error)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.onFolderPicked(uri)
    }

    // নোটিফিকেশনের অনুমতি (Android 13+), তারপর আপলোড শুরু
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.start()
    }
    // সফল আপলোডের পর "নিজে লগআউট" চালু থাকলে স্ক্রিনের টোকেনও মুছে ফেলা
    LaunchedEffect(UploadHub.logoutAfter) {
        if (UploadHub.logoutAfter) {
            UploadHub.logoutAfter = false
            vm.logout(clearAll = false)
        }
    }

    // push চলার সময় স্ক্রিন জাগিয়ে রাখা
    DisposableEffect(vm.running) {
        val window = (ctx as? Activity)?.window
        if (vm.running) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    var page by remember { mutableStateOf(0) }
    var tab by remember { mutableStateOf(0) }
    BackHandler(enabled = page == 1) { page = 0 }

    Surface(modifier = Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.systemBarsPadding().imePadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // লগআউট / সেভ করা তথ্য মোছার নিশ্চিতকরণ (১ = লগআউট, ২ = সব মুছুন)
            var logoutMode by remember { mutableStateOf(0) }
            if (logoutMode != 0) {
                AlertDialog(
                    onDismissRequest = { logoutMode = 0 },
                    icon = { Icon(Ic.Logout, contentDescription = null, tint = cs.error) },
                    title = { Text(if (logoutMode == 1) tr("লগআউট করবেন?") else tr("সব সেভ করা তথ্য মুছবেন?")) },
                    text = {
                        Text(
                            if (logoutMode == 1) {
                                tr("টোকেন এই ফোন থেকে মুছে যাবে। রিপো ও ফোল্ডারের তথ্য থাকবে। আপনার GitHub অ্যাকাউন্ট বা রিপোতে কিছু বদলাবে না।")
                            } else {
                                tr("টোকেন, রিপো, ব্রাঞ্চ ও বাছাই করা ফোল্ডারের তথ্য এই ফোন থেকে মুছে যাবে। GitHub-এ কিছু বদলাবে না।")
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                vm.logout(clearAll = logoutMode == 2)
                                logoutMode = 0
                            },
                            colors = dangerColors,
                        ) { Text(tr("হ্যাঁ, মুছুন")) }
                    },
                    dismissButton = { TextButton(onClick = { logoutMode = 0 }) { Text(tr("না")) } },
                )
            }

            if (page == 1) {
                SettingsTop { page = 0 }
                SectionCard(Ic.Key, tr("অ্যাকাউন্ট ও নিরাপত্তা"), tr("টোকেন সংরক্ষণ ও লগআউটের নিয়ম")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = vm.rememberToken, onCheckedChange = { vm.rememberToken = it }, enabled = !vm.running)
                        Text(tr("টোকেন এনক্রিপ্ট করে মনে রাখো"), style = MaterialTheme.typography.bodyMedium)
                    }
                    SwitchRow(
                        tr("আপলোড শেষে নিজে লগআউট"),
                        tr("সফল push-এর পর টোকেন ফোন থেকে মুছে যাবে"),
                        vm.clearAfterPush, !vm.running,
                    ) { vm.clearAfterPush = it }
                    if (vm.token.isNotBlank()) {
                        TextButton(onClick = { logoutMode = 1 }, enabled = !vm.running, colors = dangerColors) {
                            BtnLabel(Ic.Logout, tr("লগআউট (টোকেন মুছুন)"))
                        }
                    }
                }
                SectionCard(Ic.Book, tr("রিপো তৈরি"), tr("রিপো না থাকলে কী হবে")) {
                    SwitchRow(
                        tr("রিপো না থাকলে বানাও"),
                        tr("টোকেনে রিপো তৈরির অনুমতি লাগবে (Classic: repo)"),
                        vm.createRepo, !vm.running,
                    ) { vm.createRepo = it }
                    if (vm.createRepo) {
                        SwitchRow(tr("প্রাইভেট রিপো"), null, vm.privateRepo, !vm.running) { vm.privateRepo = it }
                    }
                }
                SectionCard(Ic.Folder, tr("ফোল্ডার ও ফিল্টার"), tr("কোন ফাইল যাবে আর কোথায় যাবে")) {
                        Field(
                            tr("রিপোর ভেতরের সাবফোল্ডার (ঐচ্ছিক)"), vm.subDir, { vm.subDir = it },
                            enabled = !vm.running, hint = tr("ফাঁকা রাখলে রিপোর রুটে যাবে"),
                        )
                        Field(
                            tr("বাদ দেওয়া নাম (কমা দিয়ে)"), vm.excludes, { vm.excludes = it },
                            enabled = !vm.running, hint = tr("*.apk এর মতো এক্সটেনশনও দেওয়া যায়"),
                        )
                        SwitchRow(
                            tr("workflows ফোল্ডার বাদ দিন"),
                            tr(".github/workflows আপলোড হবে না (টোকেনে workflow অনুমতি না থাকলে কাজে লাগে)"),
                            vm.skipWorkflows, !vm.running,
                        ) { vm.skipWorkflows = it; vm.scanFolder() }
                        OutlinedButton(
                            onClick = vm::scanFolder,
                            enabled = vm.folderUri != null && !vm.scanning && !vm.running,
                            shape = MaterialTheme.shapes.medium,
                        ) { BtnLabel(Ic.Refresh, tr("আবার স্ক্যান")) }
                }
                SectionCard(Ic.Settings, tr("ভাষা ও থিম"), tr("অ্যাপের ভাষা এবং রং বাছাই করুন")) {
                    ChoiceRow(
                        listOf("বাংলা", "English"),
                        if (Lang.english) 1 else 0,
                    ) { Lang.setEnglish(ctx, it == 1) }
                    ChoiceRow(
                        listOf(tr("ফোনের মতো"), tr("লাইট"), tr("ডার্ক")),
                        Lang.themeMode,
                    ) { Lang.setTheme(ctx, it) }
                }

                TextButton(
                    onClick = { logoutMode = 2 },
                    enabled = !vm.running,
                    colors = dangerColors,
                    modifier = Modifier.fillMaxWidth(),
                ) { BtnLabel(Ic.Delete, tr("সব সেভ করা তথ্য মুছুন")) }
            } else {
            Hero(vm.account) { page = 1 }

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
            SegmentedTabs(tab) { tab = it }

            if (tab == 0) {
                // ধাপ ১ — অ্যাকাউন্ট
                StepCard(Ic.Key, tr("১. অ্যাকাউন্ট"), tr("GitHub Access Token দিয়ে সংযুক্ত হন"), vm.account != null,
                    summary = vm.account?.let { L("@$it সংযুক্ত", "@$it connected") }, autoCollapse = true,
                ) {
                    var visible by remember { mutableStateOf(false) }
                    if (vm.savedAccounts.isNotEmpty()) {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            vm.savedAccounts.forEach { name ->
                                val active = vm.account == name
                                Surface(
                                    color = if (active) cs.primaryContainer else cs.surfaceVariant,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(enabled = !vm.running) { vm.switchAccount(name) },
                                ) {
                                    Row(
                                        Modifier.padding(start = 12.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "@$name",
                                            fontSize = 13.sp,
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (active) cs.primary else cs.onSurfaceVariant,
                                        )
                                        IconButton(onClick = { vm.removeAccount(name) }, enabled = !vm.running, modifier = Modifier.size(30.dp)) {
                                            Icon(Ic.Close, contentDescription = tr("মুছুন"), modifier = Modifier.size(14.dp), tint = cs.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = vm::newAccount,
                                enabled = !vm.running,
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            ) { Text(tr("+ নতুন অ্যাকাউন্ট"), fontSize = 13.sp) }
                        }
                    }
                    Field(
                        "Access Token", vm.token, vm::onTokenChange,
                        enabled = !vm.running, password = true, visible = visible, leading = Ic.Key,
                        trailing = {
                            IconButton(onClick = { visible = !visible }) {
                                Icon(
                                    if (visible) Ic.VisibilityOff else Ic.Visibility,
                                    contentDescription = if (visible) tr("লুকান") else tr("দেখুন"),
                                )
                            }
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
                        ) { BtnLabel(Ic.Check, tr("যাচাই করুন")) }
                        when {
                            vm.verifying -> Text(
                                tr("যাচাই হচ্ছে…"),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                            vm.account != null -> Text(
                                L("@${vm.account} সংযুক্ত","@${vm.account} connected"),
                                color = ok,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                            vm.tokenError != null -> Text(
                                vm.tokenError ?: "",
                                modifier = Modifier.weight(1f),
                                color = cs.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Text(
                        tr("Classic: repo (প্রয়োজনে workflow)   ·   Fine-grained: Contents → Read and write"),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }

                // ধাপ ২ — রিপোজিটরি
                StepCard(
                    Ic.Book, tr("২. রিপোজিটরি"), tr("কোথায় push হবে"),
                    vm.parseRepo(vm.repoInput) != null && vm.branch.isNotBlank(),
                    summary = "${vm.repoInput.trim()}  ·  ${vm.branch}",
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
                            .height(50.dp),
                    ) {
                        BtnLabel(
                            Ic.Search,
                            if (vm.token.isBlank()) tr("আগে টোকেন দিন") else tr("আমার রিপোগুলো থেকে বাছাই করুন"),
                            FontWeight.Medium,
                        )
                    }
                    Field(
                        tr("রিপো (owner/repo বা লিংক)"), vm.repoInput, { vm.repoInput = it },
                        enabled = !vm.running, leading = Ic.Book,
                        hint = tr("তালিকা থেকে বাছাই করুন, অথবা নতুন রিপোর নাম হাতে লিখুন"),
                    )
                    Field(tr("ব্রাঞ্চ"), vm.branch, { vm.branch = it }, enabled = !vm.running)
                    Field(tr("কমিট মেসেজ"), vm.message, { vm.message = it }, enabled = !vm.running)
                    if (showPicker) RepoPicker(vm) { showPicker = false }
                }

                // ধাপ ৩ — ফোল্ডার
                StepCard(Ic.Folder, tr("৩. ফোল্ডার"), tr("যে প্রজেক্ট আপলোড করবেন"), vm.folderUri != null,
                    summary = vm.scanInfo?.let { L("${vm.folderName}  ·  ${it.count} টি ফাইল", "${vm.folderName}  ·  ${it.count} files") } ?: vm.folderName,
                    autoCollapse = true,
                ) {
                    var showPreview by remember { mutableStateOf(false) }

                    FilledTonalButton(
                        onClick = { picker.launch(null) },
                        enabled = !vm.running,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        BtnLabel(
                            Ic.Folder,
                            if (vm.folderName.isEmpty()) "ফোল্ডার বাছাই করুন" else L("${vm.folderName}  ·  বদলান","${vm.folderName}  ·  change"),
                            FontWeight.Medium,
                        )
                    }

                    if (vm.scanning) {
                        Text(tr("স্ক্যান হচ্ছে…"), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                    vm.scanInfo?.let { info ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Stat(tr("ফাইল"), "${info.count}")
                            Stat(tr("মোট সাইজ"), humanSize(info.bytes))
                            Stat(tr("বাদ (বড়)"), "${info.skipped.size}")
                        }
                        TextButton(onClick = { showPreview = !showPreview }) {
                            BtnLabel(
                                if (showPreview) Ic.ExpandLess else Ic.ExpandMore,
                                if (showPreview) tr("ফাইলের তালিকা লুকান") else tr("ফাইলের তালিকা দেখুন"),
                            )
                        }
                        if (showPreview) {
                            Surface(color = TermBg, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    info.preview.forEach {
                                        Text("+ $it", color = TermOk, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    }
                                    if (info.count > info.preview.size) {
                                        Text(
                                            L("… আরও ${info.count - info.preview.size} টি ফাইল","… ${info.count - info.preview.size} more files"),
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


                    Text(
                        tr("Download বা স্টোরেজের মূল ফোল্ডার বাছাই করা যায় না। তার ভেতরের প্রজেক্ট ফোল্ডারটি বাছাই করুন।"),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }

                // ধাপ ৪ — Push
                if (vm.running || vm.logs.isNotEmpty() || vm.resultUrl != null) {
                StepCard(Ic.CloudUpload, tr("৪. Push"), tr("এক কমিটে সব ফাইল · অ্যাপ বন্ধ করলেও চলবে"), vm.resultUrl != null) {
                    if (vm.logs.isNotEmpty()) {
                        Terminal("$ git push origin ${vm.branch}", vm.logs)
                    }

                    vm.resultUrl?.let { url ->
                        Surface(
                            color = ok.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, ok.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Ic.CheckCircle, contentDescription = null, tint = ok, modifier = Modifier.size(26.dp))
                                Column {
                                    Text(tr("কাজ শেষ!"), fontWeight = FontWeight.SemiBold, color = ok)
                                    Text(
                                        tr("এখন GitHub-এ গিয়ে দেখে নিতে পারেন"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        FilledTonalButton(
                            onClick = { uriHandler.openUri(url) },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) { BtnLabel(Ic.OpenInNew, tr("GitHub-এ দেখুন")) }
                    }
                }
                }

            } else {
                BuildsTab(vm)
            }
            }
        }
        if (page == 0 && tab == 0) {
            Surface(color = cs.surface, tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (vm.running) {
                        OutlinedButton(
                            onClick = vm::cancel,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                        ) { BtnLabel(Ic.Close, tr("বাতিল করুন")) }
                    } else {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= 33 &&
                                    ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                                    != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    vm.start()
                                }
                            },
                            enabled = vm.ready,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        ) { BtnLabel(Ic.CloudUpload, "Push to GitHub", FontWeight.SemiBold) }
                        if (!vm.ready) {
                            Text(
                                tr("টোকেন, রিপো ও ফোল্ডার দিলে বাটন চালু হবে"),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }

                    if (vm.running || vm.total > 0) {
                        val pct = if (vm.total == 0) 0 else vm.done * 100 / vm.total
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                L("${vm.done} / ${vm.total} ফাইল","${vm.done} / ${vm.total} files"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                            )
                            Text(
                                "$pct%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cs.primary,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { if (vm.total == 0) 0f else vm.done.toFloat() / vm.total },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = cs.primary,
                            trackColor = cs.surfaceVariant,
                        )
                    }

                }
            }
        }
        }
    }
}
