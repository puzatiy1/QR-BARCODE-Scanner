package com.example.ui.components

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.utils.BarcodeAnalyzer
import com.example.utils.ProductKeyParser
import com.example.utils.TextAnalyzer
import com.example.utils.KeyStabilizer
import com.example.viewmodel.ScanMode
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.Executors
import kotlinx.coroutines.awaitCancellation

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    isFlashlightOn: Boolean = false,
    isScanningPaused: Boolean = false,
    scanMode: ScanMode = ScanMode.BARCODE,
    onBarcodeDetected: (Barcode) -> Unit,
    onTextDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera: Camera? by remember { mutableStateOf(null) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var isCameraProviderReady by remember { mutableStateOf(false) }

    // Use rememberUpdatedState to avoid camera re-binding when paused or callback states change
    val currentIsScanningPaused by rememberUpdatedState(isScanningPaused)
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)
    val currentOnTextDetected by rememberUpdatedState(onTextDetected)

    val keyStabilizer = remember { KeyStabilizer(windowSize = 3, threshold = 2) }

    LaunchedEffect(cameraProviderFuture) {
        cameraProviderFuture.addListener({
            isCameraProviderReady = true
        }, ContextCompat.getMainExecutor(context))
    }

    // React to flashlight changes
    LaunchedEffect(isFlashlightOn) {
        camera?.cameraControl?.enableTorch(isFlashlightOn)
    }

    // Clear stabilizer when scanning state or mode changes
    LaunchedEffect(isScanningPaused, scanMode) {
        if (isScanningPaused) {
            keyStabilizer.clear()
        }
    }

    // Handle binding and analysis switching - DO NOT put isScanningPaused here to prevent camera flicker/re-focusing
    LaunchedEffect(isCameraProviderReady, scanMode) {
        if (!isCameraProviderReady) return@LaunchedEffect

        val cameraProvider = cameraProviderFuture.get()
        val analyzerExecutor = Executors.newSingleThreadExecutor()

        try {
            cameraProvider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            if (scanMode == ScanMode.BARCODE) {
                imageAnalysis.setAnalyzer(
                    analyzerExecutor,
                    BarcodeAnalyzer { barcodes ->
                        if (!currentIsScanningPaused && barcodes.isNotEmpty()) {
                            currentOnBarcodeDetected(barcodes.first())
                        }
                    }
                )
            } else {
                keyStabilizer.clear()
                imageAnalysis.setAnalyzer(
                    analyzerExecutor,
                    TextAnalyzer { text ->
                        if (!currentIsScanningPaused) {
                            val recognizedText = text.text
                            val productKey = ProductKeyParser.extractWindowsProductKey(recognizedText)
                            if (productKey != null) {
                                val stableKey = keyStabilizer.feed(productKey)
                                if (stableKey != null) {
                                    currentOnTextDetected(stableKey)
                                }
                            }
                        }
                    }
                )
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            // Ensure torch matches state after bind
            camera?.cameraControl?.enableTorch(isFlashlightOn)

            // Keep suspended until this coroutine is cancelled/disposed
            awaitCancellation()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            analyzerExecutor.shutdown()
            try {
                cameraProvider.unbindAll()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

