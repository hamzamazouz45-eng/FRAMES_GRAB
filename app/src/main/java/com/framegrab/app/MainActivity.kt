package com.framegrab.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val vm: MainVM by viewModels()

    // Apply the language the user picked with the AR / EN chip (otherwise follow the phone).
    override fun attachBaseContext(base: Context) {
        val lang = base.getSharedPreferences("p", 0).getString("lang", null)
        if (lang == null) {
            super.attachBaseContext(base)
        } else {
            val loc = Locale(lang)
            Locale.setDefault(loc)
            val cfg = Configuration(base.resources.configuration)
            cfg.setLocale(loc)
            cfg.setLayoutDirection(loc)
            super.attachBaseContext(base.createConfigurationContext(cfg))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        val isArabic = resources.configuration.locales[0].language == "ar"
        setContent {
            FGTheme {
                Screen(
                    vm = vm,
                    langLabel = if (isArabic) "EN" else "ع",
                    onToggleLang = {
                        getSharedPreferences("p", 0).edit()
                            .putString("lang", if (isArabic) "en" else "ar")
                            .apply()
                        recreate()
                    },
                )
            }
        }
    }
}

@Composable
private fun Screen(vm: MainVM, langLabel: String, onToggleLang: () -> Unit) {
    val ctx = LocalContext.current
    val activity = ctx as? ComponentActivity

    val pickVideo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(vm::pickVideo) }
    val pickFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(vm::pickFolder) }

    // Keep the screen awake while extracting.
    DisposableEffect(vm.running) {
        if (vm.running) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(FG.ScreenBg)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header: big Arabic name with a small English line under it
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.app_name),
                    color = FG.Text,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.tagline),
                    color = FG.Teal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                )
            }
            LangChip(langLabel, onToggleLang)
        }

        // 1. Video
        TileCard(
            title = stringResource(R.string.choose_video),
            subtitle = if (vm.video == null) {
                stringResource(R.string.no_video)
            } else {
                "${vm.videoName} · ${fmtDuration(vm.durationSec)}"
            },
            onClick = { pickVideo.launch(arrayOf("video/*")) },
            enabled = !vm.running,
        ) { IconFilm() }

        // Preview with crop overlay
        vm.thumb?.let { CropPreview(it, vm.cropPct.toInt()) }

        // 2. Folder
        TileCard(
            title = stringResource(R.string.choose_folder),
            subtitle = if (vm.folder == null) {
                stringResource(R.string.no_folder)
            } else {
                vm.folderName
            },
            onClick = { pickFolder.launch(null) },
            enabled = !vm.running,
        ) { IconFolder() }

        // 3. Crop slider
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.crop_label),
                    color = FG.Text,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                val pct = vm.cropPct.toInt()
                Text(
                    if (pct == 0) stringResource(R.string.crop_off) else "$pct%",
                    color = FG.Teal,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Slider(
                value = vm.cropPct,
                onValueChange = { vm.cropPct = it },
                valueRange = 0f..50f,
                enabled = !vm.running,
                colors = SliderDefaults.colors(
                    thumbColor = FG.Teal,
                    activeTrackColor = FG.Teal,
                    inactiveTrackColor = FG.Track,
                    disabledThumbColor = FG.TextDim,
                    disabledActiveTrackColor = FG.TextDim,
                    disabledInactiveTrackColor = FG.Track,
                ),
            )
        }

        // 4. Interval + sub-folders
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillField(
                label = stringResource(R.string.every_label),
                value = vm.everySec,
                onChange = { vm.everySec = it },
                modifier = Modifier.weight(1f),
                enabled = !vm.running,
            )
            PillField(
                label = stringResource(R.string.split_label),
                value = vm.perFolder,
                onChange = { vm.perFolder = it },
                modifier = Modifier.weight(1f),
                enabled = !vm.running,
            )
        }

        // 5. What to produce
        SwitchRow(
            label = stringResource(R.string.switch_images),
            checked = vm.saveImages,
            onChange = { vm.saveImages = it },
            enabled = !vm.running,
        )
        SwitchRow(
            label = stringResource(R.string.switch_ocr),
            checked = vm.doOcr,
            onChange = { vm.doOcr = it },
            enabled = !vm.running,
        )

        Spacer(Modifier.height(2.dp))

        // 6. Start / progress / stop
        if (!vm.running) {
            BarButton(
                text = stringResource(R.string.start),
                color = FG.Teal,
                onClick = vm::start,
                enabled = vm.video != null && vm.folder != null && (vm.saveImages || vm.doOcr),
                icon = { IconPlay() },
            )
        } else {
            val frac = if (vm.total > 0) (vm.done.toFloat() / vm.total).coerceIn(0f, 1f) else 0f
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(FG.Track),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(frac)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(FG.Teal),
                )
            }
            Text(
                stringResource(R.string.progress, vm.done, vm.total),
                color = FG.Text,
                fontSize = 15.sp,
            )
            if (vm.doOcr) {
                Text(
                    stringResource(R.string.lines_found, vm.lines),
                    color = FG.Teal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (vm.lastText.isNotBlank()) {
                    // Live preview of the last line that was read, to judge the quality.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FG.BgDeep)
                            .padding(12.dp),
                    ) {
                        Text(vm.lastText, color = FG.Text, fontSize = 16.sp)
                    }
                }
            }
            Text(
                stringResource(R.string.keep_open),
                color = FG.TextDim,
                fontSize = 12.sp,
            )
            BarButton(
                text = stringResource(R.string.stop),
                color = FG.Red,
                onClick = vm::stop,
                textColor = Color.White,
                icon = { IconStop() },
            )
        }

        // 7. Result
        val err = vm.errorMsg
        val dir = vm.outDir
        if (err != null) {
            Text(stringResource(R.string.error, err), color = FG.Red, fontSize = 15.sp)
        } else if (vm.finishedCount >= 0 && !vm.running) {
            val msg = if (vm.ocrUsed) {
                if (vm.stopped) {
                    stringResource(R.string.stopped_ocr, vm.finishedCount, vm.lines)
                } else {
                    stringResource(R.string.done_ocr, vm.finishedCount, vm.lines)
                }
            } else {
                if (vm.stopped) {
                    stringResource(R.string.stopped, vm.finishedCount)
                } else {
                    stringResource(R.string.done, vm.finishedCount)
                }
            }
            Text(
                msg,
                color = if (vm.stopped) FG.Olive else FG.Green,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (dir != null) {
                TileCard(
                    title = stringResource(R.string.open_folder),
                    subtitle = null,
                    onClick = { openDir(ctx, dir) },
                ) { IconFolder() }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

private fun openDir(ctx: Context, uri: Uri) {
    try {
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: Exception) {
        Toast.makeText(ctx, R.string.cant_open, Toast.LENGTH_LONG).show()
    }
}

private fun fmtDuration(sec: Int): String {
    val h = sec / 3600
    val m = sec % 3600 / 60
    val s = sec % 60
    return if (h > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.ROOT, "%d:%02d", m, s)
    }
}
