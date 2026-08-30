package com.praytracker.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val tasbihDao: TasbihDao,
    val settingsManager: SettingsManager
) {
    val allTasbihItems: Flow<List<TasbihItem>> = tasbihDao.getAllTasbihItems()

    suspend fun getTasbihById(id: Int): TasbihItem? {
        return tasbihDao.getTasbihItemById(id)
    }

    suspend fun insertTasbih(item: TasbihItem): Long {
        return tasbihDao.insertTasbihItem(item)
    }

    suspend fun updateTasbih(item: TasbihItem) {
        tasbihDao.updateTasbihItem(item)
    }

    suspend fun deleteTasbih(item: TasbihItem) {
        tasbihDao.deleteTasbihItem(item)
    }
}
