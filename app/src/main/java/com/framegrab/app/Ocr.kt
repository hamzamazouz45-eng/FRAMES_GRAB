package com.framegrab.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Offline Arabic OCR (Tesseract). The language data (ara.traineddata) is bundled inside the app
 * under assets/tessdata/ - the GitHub build downloads it automatically.
 *
 * Subtitles are white text with a dark outline, so each strip is turned into black-on-white
 * (Otsu threshold) and enlarged before being read.
 */
class Ocr(ctx: Context) {

    private val api = TessBaseAPI()

    init {
        // Bump the folder name when the bundled language data changes.
        val root = File(ctx.filesDir, "ocr_v1")
        val dir = File(root, "tessdata")
        dir.mkdirs()
        val f = File(dir, "ara.traineddata")
        if (!f.exists() || f.length() < 1_000_000L) {
            try {
                ctx.assets.open("tessdata/ara.traineddata").use { i ->
                    f.outputStream().use { o -> i.copyTo(o) }
                }
            } catch (e: FileNotFoundException) {
                throw IOException("Arabic OCR data is missing from the app")
            }
        }
        if (!api.init(root.absolutePath, "ara")) {
            api.recycle()
            throw IOException("OCR engine failed to start")
        }
        // 6 = assume a single uniform block of text (the subtitle lines)
        api.setPageSegMode(6)
    }

    fun read(strip: Bitmap): String {
        val prepared = prepare(strip)
        return try {
            api.setImage(prepared)
            clean(api.getUTF8Text() ?: "")
        } finally {
            api.clear()
            prepared.recycle()
        }
    }

    fun close() {
        api.recycle()
    }

    private fun prepare(src: Bitmap): Bitmap {
        val scale = if (src.width < 1400) 2 else 1
        val w = src.width * scale
        val h = src.height * scale
        val big = if (scale == 1) src else Bitmap.createScaledBitmap(src, w, h, true)
        val px = IntArray(w * h)
        big.getPixels(px, 0, w, 0, 0, w, h)
        if (big !== src) big.recycle()

        // Brightness (0..255) of every pixel, plus a histogram for Otsu's threshold.
        val hist = IntArray(256)
        for (i in px.indices) {
            val c = px[i]
            val l = (((c shr 16) and 255) * 77 + ((c shr 8) and 255) * 150 + (c and 255) * 29) shr 8
            px[i] = l
            hist[l]++
        }
        val total = px.size
        var sum = 0L
        for (i in 0..255) sum += i.toLong() * hist[i]
        var sumB = 0L
        var wB = 0
        var best = 0.0
        var thr = 160
        for (t in 0..255) {
            wB += hist[t]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break
            sumB += t.toLong() * hist[t]
            val mB = sumB.toDouble() / wB
            val mF = (sum - sumB).toDouble() / wF
            val between = wB.toDouble() * wF * (mB - mF) * (mB - mF)
            if (between > best) {
                best = between
                thr = t
            }
        }
        thr = thr.coerceIn(120, 215)

        // Black text on a white page, with a white margin around it.
        val pad = 20
        val ow = w + pad * 2
        val oh = h + pad * 2
        val out = IntArray(ow * oh)
        java.util.Arrays.fill(out, Color.WHITE)
        for (y in 0 until h) {
            val src0 = y * w
            val dst0 = (y + pad) * ow + pad
            for (x in 0 until w) {
                if (px[src0 + x] > thr) out[dst0 + x] = Color.BLACK
            }
        }
        return Bitmap.createBitmap(out, ow, oh, Bitmap.Config.ARGB_8888)
    }

    private val junk = Regex("[^\\u0600-\\u06FFA-Za-z0-9 .,!?؟،:\\-\"'\\n]")
    private val spaces = Regex("\\s+")

    private fun clean(raw: String): String =
        raw.replace(junk, " ")
            .lines()
            .map { it.replace(spaces, " ").trim() }
            .filter { line -> line.count { it.isLetter() } >= 2 }
            .joinToString("\n")
}
