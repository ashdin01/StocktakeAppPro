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

class BarcodeAnalyzer(private val onDetected: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.EAN_13, BarcodeFormat.EAN_8),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    private var lastScanTime = 0L

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < COOLDOWN_MS) {
            image.close()
            return
        }

        // Use the Y (luminance) plane. Pass rowStride as dataWidth so ZXing
        // correctly handles row padding (rowStride is often > image.width).
        val yPlane = image.planes[0]
        val buffer = yPlane.buffer
        val data = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val rowStride = yPlane.rowStride

        val source = PlanarYUVLuminanceSource(
            data, rowStride, image.height,
            0, 0, image.width, image.height, false
        )

        try {
            val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            lastScanTime = now
            onDetected(result.text)
        } catch (_: NotFoundException) {
            // no barcode in this frame
        } finally {
            reader.reset()
            image.close()
        }
    }

    companion object {
        private const val COOLDOWN_MS = 1500L
    }
}
