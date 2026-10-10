package com.framegrab.app

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

/**
 * Pulls one frame every [everySec] seconds out of a video using Android's built-in
 * MediaMetadataRetriever (no ffmpeg, no extra libraries), optionally keeping only the
 * bottom [cropPct] percent of each frame (where subtitles usually are).
 *
 * Files are named by their timestamp: 00h12m05s.jpg
 */
object Extractor {

    suspend fun run(
        ctx: Context,
        video: Uri,
        tree: Uri,
        videoName: String,
        everySec: Int,
        cropPct: Int,
        perFolder: Int,
        onDir: (Uri) -> Unit,
        onProgress: (done: Int, total: Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(ctx, video)
            val ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val secs = (ms / 1000L).toInt()
            val total = secs / everySec + 1

            val root = DocumentFile.fromTreeUri(ctx, tree)
                ?: throw IOException("Cannot open the chosen folder")
            val base = root.createDirectory(safeName(videoName))
                ?: throw IOException("Cannot create a folder here")
            onDir(base.uri)

            var dir = base
            var idx = 0
            var t = 0
            while (t <= secs) {
                ensureActive()

                if (perFolder > 0 && idx % perFolder == 0 && idx > 0) {
                    val n = idx / perFolder + 1
                    dir = base.createDirectory(String.format(Locale.ROOT, "part_%03d", n))
                        ?: throw IOException("Cannot create a sub-folder")
                }

                val bmp = r.getFrameAtTime(
                    t * 1_000_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                )
                if (bmp != null) {
                    val out = if (cropPct > 0) {
                        val h = (bmp.height * cropPct / 100).coerceAtLeast(1)
                        Bitmap.createBitmap(bmp, 0, bmp.height - h, bmp.width, h)
                    } else {
                        bmp
                    }
                    val file = dir.createFile("image/jpeg", stamp(t))
                        ?: throw IOException("Cannot create an image file")
                    ctx.contentResolver.openOutputStream(file.uri)?.use {
                        out.compress(Bitmap.CompressFormat.JPEG, 92, it)
                    } ?: throw IOException("Cannot write an image file")
                    if (out !== bmp) out.recycle()
                    bmp.recycle()
                }

                idx++
                onProgress(idx, total)
                t += everySec
            }
        } finally {
            r.release()
        }
    }

    private fun stamp(t: Int): String =
        String.format(Locale.ROOT, "%02dh%02dm%02ds", t / 3600, t % 3600 / 60, t % 60)

    private fun safeName(name: String): String =
        name.substringBeforeLast('.')
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(60)
            .ifBlank { "frames" }
}
