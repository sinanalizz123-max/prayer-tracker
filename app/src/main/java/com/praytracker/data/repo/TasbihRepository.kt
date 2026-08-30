package com.praytracker.data.repo

import com.praytracker.data.db.TasbihDao
import com.praytracker.data.db.TasbihEntity
import kotlinx.coroutines.flow.Flow

class TasbihRepository(private val dao: TasbihDao) {

    fun observeAll(): Flow<List<TasbihEntity>> = dao.observeAll()

    suspend fun getAll(): List<TasbihEntity> = dao.getAll()

    /** First phrase in order, or null if none (used by widgets). */
    suspend fun getPrimaryOrDefault(): TasbihEntity? = dao.getAll().firstOrNull()

    suspend fun upsert(entity: TasbihEntity): Long {
        val id = if (entity.id <= 0) dao.maxId() + 1 else entity.id
        dao.upsert(entity.copy(id = id))
        return id
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun bump(id: Long): Long? {
        val entity = dao.get(id) ?: return null
        val newCount = (entity.count + 1).let { if (it > entity.target) entity.target else it }
        dao.upsert(entity.copy(count = newCount))
        return newCount.toLong()
    }

    suspend fun reset(id: Long) {
        dao.get(id)?.let { dao.upsert(it.copy(count = 0)) }
    }

    suspend fun clearAll() = dao.clearAll()

    suspend fun seedDefaultsIfEmpty() {
        if (dao.getAll().isNotEmpty()) return
        DEFAULT_PHRASES.forEachIndexed { index, (arabic, translation, target) ->
            dao.upsert(
                TasbihEntity(
                    id = (index + 1).toLong(),
                    arabic = arabic,
                    translation = translation,
                    target = target,
                    orderIndex = index,
                )
            )
        }
    }

    companion object {
        val DEFAULT_PHRASES: List<Triple<String, String?, Int>> = listOf(
            Triple("سُبْحَانَ اللّٰه", "Glory be to Allah", 33),
            Triple("الْحَمْدُ لِلّٰه", "Praise be to Allah", 33),
            Triple("اللّٰهُ أَكْبَر", "Allah is the Greatest", 33),
            Triple("أَسْتَغْفِرُ اللّٰه", "I seek Allah's forgiveness", 100),
            Triple("لَا إِلٰهَ إِلَّا اللّٰه", "There is no god but Allah", 100),
            Triple("سُبْحَانَ اللّٰهِ وَبِحَمْدِه", "Glory and praise be to Allah", 100),
            Triple("اللّٰهُمَّ صَلِّ عَلَى مُحَمَّد", "O Allah, send blessings upon Muhammad", 100),
            Triple("سُبْحَانَ اللّٰهِ وَالْحَمْدُ لِلّٰهِ وَلَا إِلٰهَ إِلَّا اللّٰهُ وَاللّٰهُ أَكْبَر", "Glory to Allah, praise to Allah, none has the right to be worshipped but Allah, and Allah is the Greatest", 33),
            Triple("حَسْبُنَا اللّٰهُ وَنِعْمَ الْوَكِيل", "Allah is sufficient for us, and He is the best disposer of affairs", 33),
        )
    }
}