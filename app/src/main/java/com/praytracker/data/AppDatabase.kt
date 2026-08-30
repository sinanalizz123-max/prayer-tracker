package com.praytracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [TasbihItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tasbihDao(): TasbihDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prayer_times_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.tasbihDao())
                }
            }
        }

        suspend fun populateDatabase(tasbihDao: TasbihDao) {
            val items = listOf(
                TasbihItem(id = 1, arabicText = "سُبْحَانَ اللَّهِ", englishText = "Subhan Allah (Glory be to Allah)", currentCount = 0, targetCount = 33, isCustom = false),
                TasbihItem(id = 2, arabicText = "الْحَمْدُ لِلَّهِ", englishText = "Alhamdulillah (Praise be to Allah)", currentCount = 0, targetCount = 33, isCustom = false),
                TasbihItem(id = 3, arabicText = "اللَّهُ أَكْبَرُ", englishText = "Allahu Akbar (Allah is the Greatest)", currentCount = 0, targetCount = 34, isCustom = false),
                TasbihItem(id = 4, arabicText = "لَا إِلَٰهَ إِلَّا اللَّهُ", englishText = "La ilaha illallah (There is no deity)", currentCount = 0, targetCount = 100, isCustom = false),
                TasbihItem(id = 5, arabicText = "أَسْتَغْفِرُ اللَّهَ", englishText = "Astaghfirullah (I seek forgiveness)", currentCount = 0, targetCount = 100, isCustom = false),
                TasbihItem(id = 6, arabicText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", englishText = "Subhanallahi wa bihamdihi (Glory & Praise)", currentCount = 0, targetCount = 100, isCustom = false),
                TasbihItem(id = 7, arabicText = "سُبْحَانَ اللَّهِ الْعَظِيمِ", englishText = "Subhanallahil Azheem (Glory, Supreme)", currentCount = 0, targetCount = 100, isCustom = false),
                TasbihItem(id = 8, arabicText = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", englishText = "La hawla wa la quwwata (No power except Allah)", currentCount = 0, targetCount = 100, isCustom = false),
                TasbihItem(id = 9, arabicText = "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ", englishText = "Allahumma salli ala Muhammad (Blessings on Prophet)", currentCount = 0, targetCount = 100, isCustom = false)
            )
            for (item in items) {
                tasbihDao.insertTasbihItem(item)
            }
        }
    }
}
