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

            val data = extractLuminance(image)
            val source = PlanarYUVLuminanceSource(
                data, image.width, image.height,
                0, 0, image.width, image.height, false
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

                onDebug?.invoke("frames=$frameCount found=$barcode confirm=$confirmCount/${CONFIRM_NEEDED}")

                if (confirmCount >= CONFIRM_NEEDED) {
                    lastScanTime   = now
                    pendingBarcode = null
                    confirmCount   = 0
                    onDetected(barcode)
                }
            } catch (_: NotFoundException) {
                onDebug?.invoke("frames=$frameCount no-barcode pending=$pendingBarcode/$confirmCount")
            } finally {
                reader.reset()
            }
        } finally {
            image.close()
        }
    }

    private fun extractLuminance(image: ImageProxy): ByteArray {
        val yPlane    = image.planes[0]
        val rowStride = yPlane.rowStride
        val width     = image.width
        val height    = image.height
        val buffer    = yPlane.buffer

        // If there's no row padding, copy the buffer directly.
        if (rowStride == width) {
            return ByteArray(buffer.remaining()).also { buffer.get(it) }
        }

        // Otherwise strip the padding from each row to give ZXing a clean packed array.
        val data = ByteArray(width * height)
        val row  = ByteArray(rowStride)
        for (y in 0 until height) {
            val toCopy = minOf(rowStride, buffer.remaining())
            if (toCopy <= 0) break
            buffer.get(row, 0, toCopy)
            row.copyInto(data, y * width, 0, minOf(width, toCopy))
        }
        return data
    }

    companion object {
        private const val COOLDOWN_MS    = 1500L
        private const val CONFIRM_NEEDED = 2
    }
}
