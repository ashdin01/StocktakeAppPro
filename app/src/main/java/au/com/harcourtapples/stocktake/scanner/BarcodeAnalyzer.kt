package au.com.harcourtapples.stocktake.scanner

import android.graphics.ImageFormat
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
    private var lastScannedBarcode: String? = null

    fun resetCooldown() {
        lastScanTime = 0L
        lastScannedBarcode = null
    }

    override fun analyze(image: ImageProxy) {
        if (image.format != ImageFormat.YUV_420_888) {
            image.close()
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastScanTime < COOLDOWN_MS) {
            image.close()
            return
        }

        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining()).also { buffer.get(it) }

        val source = PlanarYUVLuminanceSource(
            data, image.width, image.height,
            0, 0, image.width, image.height, false
        )

        try {
            val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            val barcode = result.text
            if (barcode != lastScannedBarcode) {
                lastScanTime = now
                lastScannedBarcode = barcode
                onDetected(barcode)
            }
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
