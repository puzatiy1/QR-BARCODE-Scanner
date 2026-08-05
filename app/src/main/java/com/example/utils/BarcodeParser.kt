package com.example.utils

import com.example.data.ScanResult
import com.google.mlkit.vision.barcode.common.Barcode

object BarcodeParser {

    fun parseBarcode(barcode: Barcode): ScanResult {
        val rawValue = barcode.rawValue ?: ""
        val displayValue = barcode.displayValue ?: rawValue

        val formatName = when (barcode.format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_EAN_13 -> "EAN 13"
            Barcode.FORMAT_EAN_8 -> "EAN 8"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_UPC_A -> "UPC A"
            Barcode.FORMAT_UPC_E -> "UPC E"
            Barcode.FORMAT_PDF417 -> "PDF 417"
            Barcode.FORMAT_AZTEC -> "Aztec"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            else -> "Barcode"
        }

        var type = "TEXT"
        var title = "Text Content"

        when (barcode.valueType) {
            Barcode.TYPE_URL -> {
                type = "URL"
                val titleStr = barcode.url?.title ?: ""
                title = if (titleStr.isNotBlank()) titleStr else "Website Link"
            }
            Barcode.TYPE_WIFI -> {
                type = "WIFI"
                val ssid = barcode.wifi?.ssid ?: ""
                title = "Wi-Fi: $ssid"
            }
            Barcode.TYPE_CONTACT_INFO -> {
                type = "CONTACT"
                val name = barcode.contactInfo?.name?.formattedName ?: ""
                title = if (name.isNotBlank()) "Contact: $name" else "Contact Card"
            }
            Barcode.TYPE_EMAIL -> {
                type = "EMAIL"
                val address = barcode.email?.address ?: ""
                title = if (address.isNotBlank()) "Email to $address" else "Email Draft"
            }
            Barcode.TYPE_SMS -> {
                type = "SMS"
                val phone = barcode.sms?.phoneNumber ?: ""
                title = if (phone.isNotBlank()) "SMS to $phone" else "SMS Message"
            }
            Barcode.TYPE_PHONE -> {
                type = "PHONE"
                val phone = barcode.phone?.number ?: ""
                title = if (phone.isNotBlank()) "Call $phone" else "Phone Call"
            }
            Barcode.TYPE_GEO -> {
                type = "GEO"
                val lat = barcode.geoPoint?.lat ?: 0.0
                val lng = barcode.geoPoint?.lng ?: 0.0
                title = "Location ($lat, $lng)"
            }
            Barcode.TYPE_PRODUCT -> {
                type = "PRODUCT"
                title = "Product: $displayValue"
            }
            else -> {
                type = "TEXT"
                title = if (displayValue.length > 28) {
                    displayValue.take(25) + "..."
                } else {
                    displayValue
                }
            }
        }

        return ScanResult(
            content = rawValue,
            formatName = formatName,
            type = type,
            title = title,
            timestamp = System.currentTimeMillis()
        )
    }
}
