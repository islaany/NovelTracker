package com.huqi.noveltracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.huqi.noveltracker.data.local.entity.NovelEntity
import com.huqi.noveltracker.data.local.entity.TagEntity

@Database(
    entities = [NovelEntity::class, TagEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun novelDao(): NovelDao
    abstract fun tagDao(): TagDao

    companion object {
        /** v1 -> v2: split the flat `tags` column into `mainTags` + `subTags`. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE novels ADD COLUMN mainTags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE novels ADD COLUMN subTags TEXT NOT NULL DEFAULT ''")
                // Preserve existing data: old flat tags become the novel's main tags.
                db.execSQL("UPDATE novels SET mainTags = tags WHERE tags IS NOT NULL AND tags <> ''")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "novel_tracker_db"
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
    }
}
