package com.example.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ScanRepository
import com.example.data.ScanResult
import com.example.utils.QrGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ScanTab {
    SCAN,
    HISTORY,
    GENERATE
}

enum class ScanMode {
    BARCODE,
    WINDOWS_KEY
}

class ScanViewModel(private val repository: ScanRepository) : ViewModel() {

    // Scan Mode State (Barcode Scanner vs Windows Product Key OCR)
    private val _scanMode = MutableStateFlow(ScanMode.BARCODE)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    // Tab Navigation State
    private val _activeTab = MutableStateFlow(ScanTab.SCAN)
    val activeTab: StateFlow<ScanTab> = _activeTab.asStateFlow()

    // Search Query State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Show Favorites Only Filter
    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    // History Scans List
    val scanHistory: StateFlow<List<ScanResult>> = combine(
        repository.allScans,
        _searchQuery,
        _showFavoritesOnly
    ) { scans, query, favoritesOnly ->
        scans.filter { scan ->
            val matchesQuery = scan.content.contains(query, ignoreCase = true) ||
                    scan.title.contains(query, ignoreCase = true) ||
                    scan.notes.contains(query, ignoreCase = true)
            val matchesFavorite = !favoritesOnly || scan.isFavorite
            matchesQuery && matchesFavorite
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Scanner Lock / Dialog states
    private val _activeScanResult = MutableStateFlow<ScanResult?>(null)
    val activeScanResult: StateFlow<ScanResult?> = _activeScanResult.asStateFlow()

    private val _isScanningPaused = MutableStateFlow(false)
    val isScanningPaused: StateFlow<Boolean> = _isScanningPaused.asStateFlow()

    // Preferences
    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val _isBeepEnabled = MutableStateFlow(true)
    val isBeepEnabled: StateFlow<Boolean> = _isBeepEnabled.asStateFlow()

    private val _isVibrationEnabled = MutableStateFlow(true)
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

    // QR Code Generator States
    private val _generatorText = MutableStateFlow("")
    val generatorText: StateFlow<String> = _generatorText.asStateFlow()

    private val _generatorType = MutableStateFlow("TEXT") // TEXT, URL, WIFI, CONTACT
    val generatorType: StateFlow<String> = _generatorType.asStateFlow()

    // Wifi Generator Sub-fields
    private val _wifiSsid = MutableStateFlow("")
    val wifiSsid: StateFlow<String> = _wifiSsid.asStateFlow()
    private val _wifiPassword = MutableStateFlow("")
    val wifiPassword: StateFlow<String> = _wifiPassword.asStateFlow()
    private val _wifiEncryption = MutableStateFlow("WPA") // WPA, WEP, nopass
    val wifiEncryption: StateFlow<String> = _wifiEncryption.asStateFlow()

    // Contact Generator Sub-fields
    private val _contactName = MutableStateFlow("")
    val contactName: StateFlow<String> = _contactName.asStateFlow()
    private val _contactPhone = MutableStateFlow("")
    val contactPhone: StateFlow<String> = _contactPhone.asStateFlow()
    private val _contactEmail = MutableStateFlow("")
    val contactEmail: StateFlow<String> = _contactEmail.asStateFlow()

    // Generated QR Output Bitmap
    private val _generatedQrBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedQrBitmap: StateFlow<Bitmap?> = _generatedQrBitmap.asStateFlow()

    fun setScanMode(mode: ScanMode) {
        _scanMode.value = mode
    }

    fun setActiveTab(tab: ScanTab) {
        _activeTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleShowFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun setFlashlightOn(isOn: Boolean) {
        _isFlashlightOn.value = isOn
    }

    fun toggleFlashlight() {
        _isFlashlightOn.value = !_isFlashlightOn.value
    }

    fun toggleBeepEnabled() {
        _isBeepEnabled.value = !_isBeepEnabled.value
    }

    fun toggleVibrationEnabled() {
        _isVibrationEnabled.value = !_isVibrationEnabled.value
    }

    // QR Generator Inputs
    fun setGeneratorText(text: String) {
        _generatorText.value = text
        generateQr()
    }

    fun setGeneratorType(type: String) {
        _generatorType.value = type
        generateQr()
    }

    fun setWifiFields(ssid: String, pass: String, enc: String) {
        _wifiSsid.value = ssid
        _wifiPassword.value = pass
        _wifiEncryption.value = enc
        generateQr()
    }

    fun setContactFields(name: String, phone: String, email: String) {
        _contactName.value = name
        _contactPhone.value = phone
        _contactEmail.value = email
        generateQr()
    }

    private fun generateQr() {
        val payload = when (_generatorType.value) {
            "URL" -> {
                val url = _generatorText.value.trim()
                if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
            }
            "WIFI" -> {
                "WIFI:S:${_wifiSsid.value};T:${_wifiEncryption.value};P:${_wifiPassword.value};;"
            }
            "CONTACT" -> {
                "MCard:N:${_contactName.value};TEL:${_contactPhone.value};EMAIL:${_contactEmail.value};;"
            }
            else -> _generatorText.value
        }

        if (payload.isNotBlank()) {
            viewModelScope.launch {
                val bitmap = QrGenerator.generateQrCode(payload, 512)
                _generatedQrBitmap.value = bitmap
            }
        } else {
            _generatedQrBitmap.value = null
        }
    }

    // Camera Scan Handling
    fun onBarcodeScanned(scanResult: ScanResult) {
        if (_isScanningPaused.value) return

        // Pause scanner & trigger feedback callback in UI (played by FeedbackHelper)
        _isScanningPaused.value = true
        _activeScanResult.value = scanResult

        // Save scan result to history database automatically
        viewModelScope.launch {
            repository.insertScan(scanResult)
        }
    }

    fun dismissScanResult() {
        _activeScanResult.value = null
        _isScanningPaused.value = false
    }

    fun updateScanNotes(scanId: Long, notes: String) {
        viewModelScope.launch {
            repository.getScanById(scanId)?.let { currentScan ->
                repository.updateScan(currentScan.copy(notes = notes))
                // Update active scan result too if it matches
                val active = _activeScanResult.value
                if (active != null && active.id == scanId) {
                    _activeScanResult.value = active.copy(notes = notes)
                }
            }
        }
    }

    fun toggleFavorite(scanResult: ScanResult) {
        viewModelScope.launch {
            val updated = scanResult.copy(isFavorite = !scanResult.isFavorite)
            repository.updateScan(updated)
            val active = _activeScanResult.value
            if (active != null && active.id == scanResult.id) {
                _activeScanResult.value = updated
            }
        }
    }

    fun deleteScan(scanResult: ScanResult) {
        viewModelScope.launch {
            repository.deleteScan(scanResult)
            if (_activeScanResult.value?.id == scanResult.id) {
                _activeScanResult.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllScans()
        }
    }
}

class ScanViewModelFactory(private val repository: ScanRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
