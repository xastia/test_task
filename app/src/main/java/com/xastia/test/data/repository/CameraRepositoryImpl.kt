package com.xastia.test.data.repository

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.xastia.test.domain.pulse.RgbChannels
import com.xastia.test.domain.repository.CameraRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ExecutorService

class CameraRepositoryImpl(
    private val context: android.content.Context,
    private val cameraExecutor: ExecutorService
) : CameraRepository {

    // Reference на камеру потрібен для cameraControl.enableTorch().
    @Volatile private var camera: Camera? = null

    override fun startCamera(previewView: PreviewView): Flow<ImageProxy> = callbackFlow {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        trySend(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        awaitClose {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider.unbindAll()
            camera = null
        }
    }

    /**
     * Усереднена яскравість (Y/luma канал, plane 0). Швидко і достатньо
     * для детекції "палець на камері" (низька яскравість → палець закрив).
     */
    override fun analyzeImage(image: ImageProxy): Double {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val width = image.width
        val height = image.height

        // ROI 50% × 50% по центру (відсікаємо краї — там віньєтування і шум)
        val left = width / 4
        val top = height / 4
        val right = left + width / 2
        val bottom = top + height / 2

        var sum = 0L
        var count = 0
        for (row in top until bottom step 2) {
            for (col in left until right step 2) {
                val index = row * rowStride + col * pixelStride
                if (index < 0 || index >= buffer.limit()) continue
                sum += (buffer.get(index).toInt() and 0xFF)
                count++
            }
        }
        return if (count > 0) sum.toDouble() / count else 0.0
    }

    /**
     * Усереднення R-каналу через YUV → R конверсію за формулою BT.601:
     *   R = Y + 1.402 × (V − 128)
     *
     * Чому саме R: гемоглобін поглинає зелене світло сильніше за червоне.
     * При натисканні пальця на камеру (з ввімкненим torch) червоний канал
     * пульсує найбільш виразно — кров періодично змінює оптичну густину тканини.
     *
     * Семпли беруться по центральній ROI 50% × 50% з кроком 2 пікселі
     * (~6000-8000 точок усереднення для типового 480p — більш ніж достатньо
     * щоб подавити шум).
     */
    override fun analyzeRedChannel(image: ImageProxy): Double {
        val yPlane = image.planes[0]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        val width = image.width
        val height = image.height

        val left = width / 4
        val top = height / 4
        val right = left + width / 2
        val bottom = top + height / 2

        var sumR = 0.0
        var count = 0
        for (row in top until bottom step 2) {
            for (col in left until right step 2) {
                val yIdx = row * yRowStride + col
                // V plane має у 2 рази меншу роздільну здатність (4:2:0 subsampling)
                val vIdx = (row / 2) * vRowStride + (col / 2) * vPixelStride
                if (yIdx < 0 || yIdx >= yBuffer.limit()) continue
                if (vIdx < 0 || vIdx >= vBuffer.limit()) continue

                val y = (yBuffer.get(yIdx).toInt() and 0xFF).toDouble()
                val v = (vBuffer.get(vIdx).toInt() and 0xFF).toDouble()
                // BT.601: R = Y + 1.402 * (V - 128)
                val r = (y + 1.402 * (v - 128.0)).coerceIn(0.0, 255.0)
                sumR += r
                count++
            }
        }
        return if (count > 0) sumR / count else 0.0
    }

    /**
     * Усереднює всі три канали RGB у центральній ROI через повну BT.601 конверсію:
     *   R = Y + 1.402 × (V − 128)
     *   G = Y − 0.344 × (U − 128) − 0.714 × (V − 128)
     *   B = Y + 1.772 × (U − 128)
     *
     * Витратніше за лише R (читаємо ще U-площину), але дає змогу детектувати
     * палець не лише по абсолютному R, а й по його співвідношенню з G і B —
     * це специфічна сигнатура пропускання гемоглобіну.
     */
    override fun analyzeRgb(image: ImageProxy): RgbChannels {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        val width = image.width
        val height = image.height
        val left = width / 4
        val top = height / 4
        val right = left + width / 2
        val bottom = top + height / 2

        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var count = 0

        for (row in top until bottom step 2) {
            for (col in left until right step 2) {
                val yIdx = row * yRowStride + col
                val uIdx = (row / 2) * uRowStride + (col / 2) * uPixelStride
                val vIdx = (row / 2) * vRowStride + (col / 2) * vPixelStride
                if (yIdx < 0 || yIdx >= yBuffer.limit()) continue
                if (uIdx < 0 || uIdx >= uBuffer.limit()) continue
                if (vIdx < 0 || vIdx >= vBuffer.limit()) continue

                val y = (yBuffer.get(yIdx).toInt() and 0xFF).toDouble()
                val u = (uBuffer.get(uIdx).toInt() and 0xFF).toDouble()
                val v = (vBuffer.get(vIdx).toInt() and 0xFF).toDouble()

                val r = (y + 1.402 * (v - 128.0)).coerceIn(0.0, 255.0)
                val g = (y - 0.344 * (u - 128.0) - 0.714 * (v - 128.0)).coerceIn(0.0, 255.0)
                val b = (y + 1.772 * (u - 128.0)).coerceIn(0.0, 255.0)

                sumR += r
                sumG += g
                sumB += b
                count++
            }
        }
        return if (count > 0) {
            RgbChannels(sumR / count, sumG / count, sumB / count)
        } else {
            RgbChannels(0.0, 0.0, 0.0)
        }
    }

    override fun enableTorch(enable: Boolean) {
        camera?.cameraControl?.enableTorch(enable)
    }
}
