package com.framegrab.app

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Pulls one frame every N seconds out of a video using Android's built-in
 * MediaMetadataRetriever (no ffmpeg), optionally keeping only the bottom part of each frame
 * (where subtitles usually are).
 *
 * Images are named by timestamp: 00h12m05s.jpg
 * With OCR on, each frame's subtitle strip is compared with the previous one and only read when it
 * changed; the result is written as subtitles.srt and subtitles.txt.
 */
object Extractor {

    class Options(
        val everySec: Int,
        val cropPct: Int,
        val perFolder: Int,
        val saveImages: Boolean,
        val ocr: Boolean,
    )

    private class Seg(val start: Int, var end: Int, var text: String)

    private class Tracker {
        val segs = ArrayList<Seg>()
        var cur: Seg? = null

        fun close(t: Int) {
            cur?.let {
                it.end = max(t, it.start + 1)
                segs.add(it)
            }
            cur = null
        }

        fun open(t: Int, text: String) {
            cur = Seg(t, t, text)
        }
    }

    // Subtitle strip analysis
    private const val COLS = 48
    private const val ROWS = 8
    private const val EMPTY_BELOW = 0.003f   // almost no bright pixels = no subtitle on screen
    private const val CHANGED_ABOVE = 0.20f  // how different the strip must look to be re-read

    suspend fun run(
        ctx: Context,
        video: Uri,
        tree: Uri,
        videoName: String,
        o: Options,
        onDir: (Uri) -> Unit,
        onProgress: (done: Int, total: Int) -> Unit,
        onText: (count: Int, last: String) -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            val r = MediaMetadataRetriever()
            var ocr: Ocr? = null
            var base: DocumentFile? = null
            val tracker = Tracker()
            var lastT = 0
            try {
                r.setDataSource(ctx, video)
                val ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                val secs = (ms / 1000L).toInt()
                val every = o.everySec
                val total = secs / every + 1

                val root = DocumentFile.fromTreeUri(ctx, tree)
                    ?: throw IOException("Cannot open the chosen folder")
                val b = root.createDirectory(safeName(videoName))
                    ?: throw IOException("Cannot create a folder here")
                base = b
                onDir(b.uri)

                if (o.ocr) ocr = Ocr(ctx)
                val engine = ocr
                val ocrPct = if (o.cropPct > 0) o.cropPct else 30

                var dir = b
                var idx = 0
                var t = 0
                var prev: FloatArray? = null
                while (t <= secs) {
                    ensureActive()
                    lastT = t

                    if (o.saveImages && o.perFolder > 0 && idx > 0 && idx % o.perFolder == 0) {
                        val n = idx / o.perFolder + 1
                        dir = b.createDirectory(String.format(Locale.ROOT, "part_%03d", n))
                            ?: throw IOException("Cannot create a sub-folder")
                    }

                    val bmp = r.getFrameAtTime(
                        t * 1_000_000L,
                        MediaMetadataRetriever.OPTION_CLOSEST,
                    )
                    if (bmp != null) {
                        if (o.saveImages) {
                            val out = if (o.cropPct > 0) bottom(bmp, o.cropPct) else bmp
                            val file = dir.createFile("image/jpeg", stamp(t))
                                ?: throw IOException("Cannot create an image file")
                            ctx.contentResolver.openOutputStream(file.uri)?.use {
                                out.compress(Bitmap.CompressFormat.JPEG, 92, it)
                            } ?: throw IOException("Cannot write an image file")
                            if (out !== bmp) out.recycle()
                        }

                        if (engine != null) {
                            val strip = bottom(bmp, ocrPct)
                            val sig = signature(strip)
                            val avg = sig.sum() / sig.size
                            if (avg < EMPTY_BELOW) {
                                // No subtitle on screen right now.
                                tracker.close(t)
                                prev = null
                            } else if (prev == null || diff(prev, sig) > CHANGED_ABOVE) {
                                val text = engine.read(strip)
                                prev = sig
                                val c = tracker.cur
                                if (text.isBlank()) {
                                    tracker.close(t)
                                } else if (c != null && similar(c.text, text)) {
                                    // Same subtitle as before: keep the more complete reading.
                                    if (text.length > c.text.length) {
                                        c.text = text
                                        onText(tracker.segs.size + 1, text)
                                    }
                                } else {
                                    tracker.close(t)
                                    tracker.open(t, text)
                                    onText(tracker.segs.size + 1, text)
                                }
                            }
                            strip.recycle()
                        }
                        bmp.recycle()
                    }

                    idx++
                    onProgress(idx, total)
                    t += every
                }
            } finally {
                // Runs on normal finish, on Stop, and on errors: keep whatever was read so far.
                withContext(NonCancellable) {
                    tracker.close(lastT + o.everySec)
                    ocr?.close()
                    try {
                        r.release()
                    } catch (_: Exception) {
                    }
                    val bb = base
                    if (o.ocr && bb != null && tracker.segs.isNotEmpty()) {
                        runCatching { writeSubs(ctx, bb, tracker.segs) }
                    }
                }
            }
        }
    }

    // ---- helpers ----

    private fun bottom(bmp: Bitmap, pct: Int): Bitmap {
        val h = (bmp.height * pct / 100).coerceAtLeast(1)
        return Bitmap.createBitmap(bmp, 0, bmp.height - h, bmp.width, h)
    }

    /** Where the bright (text) pixels are, as a small grid of fractions. */
    private fun signature(b: Bitmap): FloatArray {
        val w = b.width
        val h = b.height
        val px = IntArray(w * h)
        b.getPixels(px, 0, w, 0, 0, w, h)
        val cnt = IntArray(COLS * ROWS)
        for (y in 0 until h) {
            val cy = y * ROWS / h
            val row = y * w
            for (x in 0 until w) {
                val c = px[row + x]
                val l = (((c shr 16) and 255) * 77 + ((c shr 8) and 255) * 150 + (c and 255) * 29) shr 8
                if (l > 205) cnt[cy * COLS + x * COLS / w]++
            }
        }
        val cell = ((w / COLS) + 1) * ((h / ROWS) + 1)
        return FloatArray(COLS * ROWS) { cnt[it].toFloat() / cell }
    }

    private fun diff(a: FloatArray, b: FloatArray): Float {
        var s = 0f
        var m = 0f
        for (i in a.indices) {
            s += abs(a[i] - b[i])
            m += max(a[i], b[i])
        }
        return if (m < 1e-6f) 0f else s / m
    }

    private fun norm(s: String): String = s.filter { it.isLetterOrDigit() }

    private fun similar(a: String, b: String): Boolean {
        val x = norm(a)
        val y = norm(b)
        if (x.isEmpty() || y.isEmpty()) return false
        val d = lev(x, y)
        return 1f - d.toFloat() / max(x.length, y.length) >= 0.75f
    }

    private fun lev(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            val tmp = prev
            prev = cur
            cur = tmp
        }
        return prev[b.length]
    }

    private fun writeSubs(ctx: Context, dir: DocumentFile, segs: List<Seg>) {
        val srt = StringBuilder()
        val txt = StringBuilder()
        segs.forEachIndexed { i, s ->
            srt.append(i + 1).append('\n')
                .append(srtTime(s.start)).append(" --> ").append(srtTime(s.end)).append('\n')
                .append(s.text).append("\n\n")
            txt.append(s.text.replace('\n', ' ')).append('\n')
        }
        put(ctx, dir, "subtitles.srt", "application/x-subrip", srt.toString())
        put(ctx, dir, "subtitles.txt", "text/plain", txt.toString())
    }

    private fun put(ctx: Context, dir: DocumentFile, name: String, mime: String, content: String) {
        val f = dir.createFile(mime, name.substringBeforeLast('.'))
            ?: throw IOException("Cannot create $name")
        ctx.contentResolver.openOutputStream(f.uri, "wt")?.use {
            it.write(content.toByteArray(Charsets.UTF_8))
        } ?: throw IOException("Cannot write $name")
        // Some providers add or skip an extension on their own: make sure the name is exact.
        if (f.name != name) f.renameTo(name)
    }

    private fun srtTime(t: Int): String =
        String.format(Locale.ROOT, "%02d:%02d:%02d,000", t / 3600, t % 3600 / 60, t % 60)

    private fun stamp(t: Int): String =
        String.format(Locale.ROOT, "%02dh%02dm%02ds", t / 3600, t % 3600 / 60, t % 60)

    private fun safeName(name: String): String =
        name.substringBeforeLast('.')
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(60)
            .ifBlank { "frames" }
}
