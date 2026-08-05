package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ScanResult
import com.example.ui.components.ScanResultDetailDialog
import com.example.utils.FeedbackHelper
import com.example.viewmodel.ScanTab
import com.example.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ScanViewModel) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsState()
    val activeScanResult by viewModel.activeScanResult.collectAsState()

    // Instantiate feedback helper once, tied to screen lifecycle
    val feedbackHelper = remember { FeedbackHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            feedbackHelper.release()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "QR & Barcode Scanner",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("app_bottom_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Scan Tab
                NavigationBarItem(
                    selected = activeTab == ScanTab.SCAN,
                    onClick = { viewModel.setActiveTab(ScanTab.SCAN) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == ScanTab.SCAN) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner,
                            contentDescription = "Scan Tab"
                        )
                    },
                    label = { Text("Scan", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_scan")
                )

                // History Tab
                NavigationBarItem(
                    selected = activeTab == ScanTab.HISTORY,
                    onClick = { viewModel.setActiveTab(ScanTab.HISTORY) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == ScanTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "History Tab"
                        )
                    },
                    label = { Text("History", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_history")
                )

                // Generate Tab
                NavigationBarItem(
                    selected = activeTab == ScanTab.GENERATE,
                    onClick = { viewModel.setActiveTab(ScanTab.GENERATE) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == ScanTab.GENERATE) Icons.Filled.QrCode else Icons.Outlined.QrCode,
                            contentDescription = "Generate Tab"
                        )
                    },
                    label = { Text("Generate", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_generate")
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Animated Transition of Tab Content
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "tab_animation"
            ) { tab ->
                when (tab) {
                    ScanTab.SCAN -> ScanTabContent(
                        viewModel = viewModel,
                        feedbackHelper = feedbackHelper
                    )
                    ScanTab.HISTORY -> HistoryTabContent(
                        viewModel = viewModel,
                        onItemSelected = { scan ->
                            viewModel.onBarcodeScanned(scan)
                        }
                    )
                    ScanTab.GENERATE -> GenerateTabContent(
                        viewModel = viewModel
                    )
                }
            }

            // Floating Active Scan Result / Dialog Detail Overlay
            if (activeScanResult != null) {
                ScanResultDetailDialog(
                    scanResult = activeScanResult!!,
                    onDismiss = { viewModel.dismissScanResult() },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onNotesUpdated = { id, notes -> viewModel.updateScanNotes(id, notes) },
                    onDelete = { viewModel.deleteScan(it) }
                )
            }
        }
    }
}
