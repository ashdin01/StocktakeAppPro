package au.com.harcourtapples.stocktake.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

class BarcodeAnalyzer(
    private val onDetected: (String) -> Unit,
    private val onDebug: ((String) -> Unit)? = null,
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.EAN_13, BarcodeFormat.EAN_8),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    private var lastScanTime   = 0L
    private var pendingBarcode: String? = null
    private var confirmCount   = 0
    private var frameCount     = 0
    private var lastRaw        = "—"

    override fun analyze(image: ImageProxy) {
        try {
            frameCount++
            val now = System.currentTimeMillis()
            if (now - lastScanTime < COOLDOWN_MS) {
                onDebug?.invoke("frames=$frameCount [cooldown] last=$lastRaw")
                return
            }

            val rotation = image.imageInfo.rotationDegrees
            val (data, w, h) = extractLuminance(image, rotation)

            val source = PlanarYUVLuminanceSource(
                data, w, h, 0, 0, w, h, false
            )

            try {
                val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
                val barcode = result.text
                lastRaw = barcode

                if (barcode == pendingBarcode) {
                    confirmCount++
                } else {
                    pendingBarcode = barcode
                    confirmCount   = 1
                }

                onDebug?.invoke("frames=$frameCount rot=$rotation found=$barcode confirm=$confirmCount/${CONFIRM_NEEDED}")

                if (confirmCount >= CONFIRM_NEEDED) {
                    lastScanTime   = now
                    pendingBarcode = null
                    confirmCount   = 0
                    onDetected(barcode)
                }
            } catch (_: NotFoundException) {
                onDebug?.invoke("frames=$frameCount rot=$rotation no-barcode pending=$pendingBarcode/$confirmCount")
            } finally {
                reader.reset()
            }
        } finally {
            image.close()
        }
    }

    private data class LumaImage(val data: ByteArray, val width: Int, val height: Int)

    private fun extractLuminance(image: ImageProxy, rotationDegrees: Int): LumaImage {
        val yPlane    = image.planes[0]
        val rowStride = yPlane.rowStride
        val width     = image.width
        val height    = image.height
        val buffer    = yPlane.buffer

        // Strip row padding to produce a clean packed Y array.
        val luma = ByteArray(width * height)
        if (rowStride == width) {
            val avail = minOf(buffer.remaining(), luma.size)
            buffer.get(luma, 0, avail)
        } else {
            val row = ByteArray(rowStride)
            for (y in 0 until height) {
                val toCopy = minOf(rowStride, buffer.remaining())
                if (toCopy <= 0) break
                buffer.get(row, 0, toCopy)
                row.copyInto(luma, y * width, 0, minOf(width, toCopy))
            }
        }

        return when (rotationDegrees) {
            90  -> LumaImage(rotate90cw(luma, width, height),  height, width)
            180 -> LumaImage(rotate180(luma, width, height),   width,  height)
            270 -> LumaImage(rotate90ccw(luma, width, height), height, width)
            else -> LumaImage(luma, width, height)
        }
    }

    // 90° clockwise: (r, c) → (c, H-1-r), new dims H×W
    private fun rotate90cw(src: ByteArray, srcW: Int, srcH: Int): ByteArray {
        val dst = ByteArray(srcW * srcH)
        for (r in 0 until srcH) {
            for (c in 0 until srcW) {
                dst[c * srcH + (srcH - 1 - r)] = src[r * srcW + c]
            }
        }
        return dst
    }

    // 90° counter-clockwise: (r, c) → (W-1-c, r), new dims H×W
    private fun rotate90ccw(src: ByteArray, srcW: Int, srcH: Int): ByteArray {
        val dst = ByteArray(srcW * srcH)
        for (r in 0 until srcH) {
            for (c in 0 until srcW) {
                dst[(srcW - 1 - c) * srcH + r] = src[r * srcW + c]
            }
        }
        return dst
    }

    private fun rotate180(src: ByteArray, srcW: Int, srcH: Int): ByteArray {
        val dst = ByteArray(srcW * srcH)
        for (i in src.indices) dst[src.size - 1 - i] = src[i]
        return dst
    }

    companion object {
        private const val COOLDOWN_MS    = 1500L
        private const val CONFIRM_NEEDED = 2
    }
}
