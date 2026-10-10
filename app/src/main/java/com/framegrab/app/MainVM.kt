package com.framegrab.app

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainVM(app: Application) : AndroidViewModel(app) {

    private val ctx: Application get() = getApplication()
    private val prefs = app.getSharedPreferences("p", 0)

    // Inputs
    var video by mutableStateOf<Uri?>(null); private set
    var videoName by mutableStateOf(""); private set
    var durationSec by mutableIntStateOf(0); private set
    var thumb by mutableStateOf<Bitmap?>(null); private set
    var folder by mutableStateOf<Uri?>(null); private set
    var folderName by mutableStateOf(""); private set

    // Options
    var cropPct by mutableFloatStateOf(25f)
    var everySec by mutableStateOf("1")
    var perFolder by mutableStateOf("0")

    // Run state
    var running by mutableStateOf(false); private set
    var done by mutableIntStateOf(0); private set
    var total by mutableIntStateOf(0); private set
    var finishedCount by mutableIntStateOf(-1); private set
    var stopped by mutableStateOf(false); private set
    var errorMsg by mutableStateOf<String?>(null); private set
    var outDir by mutableStateOf<Uri?>(null); private set

    private var job: Job? = null

    init {
        // Re-use the last chosen folder if we still have permission for it.
        prefs.getString("tree", null)?.let { s ->
            val u = Uri.parse(s)
            val ok = ctx.contentResolver.persistedUriPermissions
                .any { it.uri == u && it.isWritePermission }
            if (ok) applyFolder(u)
        }
    }

    fun pickVideo(uri: Uri) {
        video = uri
        thumb = null
        durationSec = 0
        videoName = ""
        clearResult()
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) { readInfo(uri) }
            videoName = info.name
            durationSec = info.sec
            thumb = info.thumb
        }
    }

    fun pickFolder(uri: Uri) {
        try {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: Exception) {
        }
        prefs.edit().putString("tree", uri.toString()).apply()
        applyFolder(uri)
    }

    private fun applyFolder(uri: Uri) {
        folder = uri
        folderName = DocumentFile.fromTreeUri(ctx, uri)?.name ?: ""
        clearResult()
    }

    fun start() {
        val v = video ?: return
        val f = folder ?: return
        val every = (everySec.toIntOrNull() ?: 1).coerceAtLeast(1)
        val per = (perFolder.toIntOrNull() ?: 0).coerceAtLeast(0)
        val crop = cropPct.toInt()
        val name = videoName.ifBlank { "video" }

        clearResult()
        running = true
        done = 0
        total = 0
        job = viewModelScope.launch {
            try {
                Extractor.run(
                    ctx, v, f, name, every, crop, per,
                    onDir = { outDir = it },
                    onProgress = { d, t -> done = d; total = t },
                )
                finishedCount = done
            } catch (e: CancellationException) {
                finishedCount = done
                stopped = true
                throw e
            } catch (e: Exception) {
                errorMsg = e.message ?: e.javaClass.simpleName
            } finally {
                running = false
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    private fun clearResult() {
        finishedCount = -1
        stopped = false
        errorMsg = null
        outDir = null
    }

    private class Info(val name: String, val sec: Int, val thumb: Bitmap?)

    private fun readInfo(uri: Uri): Info {
        var name = "video"
        try {
            ctx.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) name = c.getString(0) ?: name
            }
        } catch (_: Exception) {
        }
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, uri)
            val ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val atUs = minOf(1_000_000L, ms * 500L)
            val f = r.getFrameAtTime(atUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val t = f?.let {
                if (it.width > 720) {
                    Bitmap.createScaledBitmap(it, 720, it.height * 720 / it.width, true)
                } else it
            }
            Info(name, (ms / 1000L).toInt(), t)
        } catch (e: Exception) {
            Info(name, 0, null)
        } finally {
            r.release()
        }
    }
}
