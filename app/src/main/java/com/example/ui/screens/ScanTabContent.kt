package com.example.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.CameraPreview
import com.example.ui.components.ScannerOverlay
import com.example.utils.BarcodeParser
import com.example.utils.FeedbackHelper
import com.example.viewmodel.ScanViewModel
import com.example.viewmodel.ScanMode
import com.example.utils.ProductKeyParser
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@Composable
fun ScanTabContent(
    viewModel: ScanViewModel,
    feedbackHelper: FeedbackHelper
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required to scan codes", Toast.LENGTH_LONG).show()
        }
    }

    val isFlashlightOn by viewModel.isFlashlightOn.collectAsState()
    val isScanningPaused by viewModel.isScanningPaused.collectAsState()
    val isBeepEnabled by viewModel.isBeepEnabled.collectAsState()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()
    val scanMode by viewModel.scanMode.collectAsState()

    // Gallery Image Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scanImageFromUri(context, uri, viewModel, feedbackHelper, scanMode)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("scan_tab_content")
    ) {
        if (hasCameraPermission) {
            // Camera viewport fills the background
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                isFlashlightOn = isFlashlightOn,
                isScanningPaused = isScanningPaused,
                scanMode = scanMode,
                onBarcodeDetected = { barcode ->
                    if (isBeepEnabled) feedbackHelper.playBeep()
                    if (isVibrationEnabled) feedbackHelper.vibrate()

                    val result = BarcodeParser.parseBarcode(barcode)
                    viewModel.onBarcodeScanned(result)
                },
                onTextDetected = { productKey ->
                    if (isBeepEnabled) feedbackHelper.playBeep()
                    if (isVibrationEnabled) feedbackHelper.vibrate()

                    val result = com.example.data.ScanResult(
                        content = productKey,
                        formatName = "Windows Product Key",
                        type = "WINDOWS_KEY",
                        title = "Windows License Key",
                        timestamp = System.currentTimeMillis()
                    )
                    viewModel.onBarcodeScanned(result)
                }
            )

            // Scanning overlays
            ScannerOverlay(modifier = Modifier.fillMaxSize())

            // Top Floating Controls: Flash, Audio, Vibration, Gallery Pick
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flashlight controller
                    FloatingControlButton(
                        onClick = { viewModel.toggleFlashlight() },
                        icon = if (isFlashlightOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Toggle Flashlight",
                        isActive = isFlashlightOn,
                        testTag = "flashlight_button"
                    )

                    // Center Guide Text
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (scanMode == ScanMode.BARCODE) {
                                "Align code inside guide"
                            } else {
                                "Align Windows key inside guide"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    // Gallery image scan trigger
                    FloatingControlButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        icon = Icons.Default.PhotoLibrary,
                        contentDescription = "Scan Image from Gallery",
                        isActive = false,
                        testTag = "gallery_scan_button"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Sub-Controls: Beep and Vibration states
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Beep Indicator
                    IconButton(
                        onClick = { viewModel.toggleBeepEnabled() },
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isBeepEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Sound Beep",
                            tint = if (isBeepEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Vibration Indicator
                    IconButton(
                        onClick = { viewModel.toggleVibrationEnabled() },
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isVibrationEnabled) Icons.Default.Vibration else Icons.Default.CloudQueue, // Fallback for vibration off
                            contentDescription = "Toggle Vibration Feedback",
                            tint = if (isVibrationEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Bottom Scan Mode Switcher (Floating Card)
            if (scanMode == ScanMode.WINDOWS_KEY) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp)
                ) {
                    Text(
                        text = "Держите камеру ровно, чтобы сфокусироваться",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Barcode Mode
                    Button(
                        onClick = { viewModel.setScanMode(ScanMode.BARCODE) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (scanMode == ScanMode.BARCODE) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            contentColor = if (scanMode == ScanMode.BARCODE) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                Color.White.copy(alpha = 0.8f)
                            }
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Barcode / QR",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Windows Key Mode
                    Button(
                        onClick = { viewModel.setScanMode(ScanMode.WINDOWS_KEY) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (scanMode == ScanMode.WINDOWS_KEY) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            contentColor = if (scanMode == com.example.viewmodel.ScanMode.WINDOWS_KEY) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                Color.White.copy(alpha = 0.8f)
                            }
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Windows Key",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Premium Onboarding & Permission Grant View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Giant Graphic
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(140.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraEnhance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Camera Access Required",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "We need access to your device's camera to scan QR codes and barcodes in real-time. Everything is parsed completely offline for maximum security and speed.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("grant_permission_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Grant Camera Permission",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        // Let the user scan gallery images even if camera is denied
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Or Scan From Gallery Image", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun FloatingControlButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    testTag: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.55f),
                shape = CircleShape
            )
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) MaterialTheme.colorScheme.onPrimary else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun scanImageFromUri(
    context: Context,
    uri: Uri,
    viewModel: ScanViewModel,
    feedbackHelper: FeedbackHelper,
    scanMode: ScanMode
) {
    try {
        val image = InputImage.fromFilePath(context, uri)
        if (scanMode == ScanMode.BARCODE) {
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)
                .build()
            val scanner = BarcodeScanning.getClient(options)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        feedbackHelper.playBeep()
                        feedbackHelper.vibrate()

                        val result = BarcodeParser.parseBarcode(barcodes.first())
                        viewModel.onBarcodeScanned(result)
                    } else {
                        Toast.makeText(context, "No QR codes or barcodes found in selected photo.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to analyze photo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        } else {
            val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    var productKey = ProductKeyParser.extractWindowsProductKey(text.text)
                    if (productKey == null) {
                        productKey = ProductKeyParser.extractWindowsProductKeyLenient(text.text)
                    }
                    if (productKey != null) {
                        feedbackHelper.playBeep()
                        feedbackHelper.vibrate()

                        val result = com.example.data.ScanResult(
                            content = productKey,
                            formatName = "Windows Product Key",
                            type = "WINDOWS_KEY",
                            title = "Windows License Key",
                            timestamp = System.currentTimeMillis()
                        )
                        viewModel.onBarcodeScanned(result)
                    } else {
                        Toast.makeText(context, "No Windows Product Key found in selected photo. Try to align the printed key.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to analyze photo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to load photo", Toast.LENGTH_SHORT).show()
    }
}
