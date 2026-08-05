package com.example.data

import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanDao: ScanDao) {
    val allScans: Flow<List<ScanResult>> = scanDao.getAllScans()
    val favoriteScans: Flow<List<ScanResult>> = scanDao.getFavoriteScans()

    suspend fun getScanById(id: Long): ScanResult? = scanDao.getScanById(id)

    suspend fun insertScan(scan: ScanResult): Long = scanDao.insertScan(scan)

    suspend fun updateScan(scan: ScanResult) = scanDao.updateScan(scan)

    suspend fun deleteScan(scan: ScanResult) = scanDao.deleteScan(scan)

    suspend fun deleteScanById(id: Long) = scanDao.deleteScanById(id)

    suspend fun clearAllScans() = scanDao.clearAllScans()
}
