package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.entities.AiInsightEntity
import com.example.data.local.entities.DailyMetricEntity
import com.example.data.local.entities.FoodEntryEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HabitLogEntity
import com.example.data.local.entities.KinesitherapySessionEntity
import com.example.data.local.entities.NoteEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.UserProfileEntity
import com.example.data.local.entities.WaterLogEntity
import com.example.data.local.entities.WorkoutEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        TaskEntity::class,
        NoteEntity::class,
        WorkoutEntity::class,
        FoodEntryEntity::class,
        WaterLogEntity::class,
        DailyMetricEntity::class,
        AiInsightEntity::class,
        UserProfileEntity::class,
        KinesitherapySessionEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pulseDao(): PulseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `water_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amountMl` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_profile` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `age` INTEGER NOT NULL,
                        `weightKg` REAL NOT NULL,
                        `heightCm` REAL NOT NULL,
                        `gender` TEXT NOT NULL,
                        `activityLevel` TEXT NOT NULL,
                        `goal` TEXT NOT NULL,
                        `bmr` INTEGER NOT NULL,
                        `tdee` INTEGER NOT NULL,
                        `targetCalories` INTEGER NOT NULL,
                        `targetWaterMl` INTEGER NOT NULL,
                        `targetProteinGrams` INTEGER NOT NULL,
                        `targetFatsGrams` INTEGER NOT NULL,
                        `targetCarbsGrams` INTEGER NOT NULL,
                        `isOnboarded` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `kinesitherapy_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `primaryGoal` TEXT NOT NULL,
                        `availableExercisesPhotoBase64` TEXT,
                        `performedExercisesPhotoBase64` TEXT,
                        `exercisesJson` TEXT NOT NULL,
                        `totalTonnageKg` REAL NOT NULL,
                        `exercisesCompletedCount` INTEGER NOT NULL,
                        `exercisesTotalCount` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `painLevelBefore` INTEGER NOT NULL,
                        `painLevelAfter` INTEGER NOT NULL,
                        `aiScore` INTEGER NOT NULL,
                        `aiEvaluation` TEXT NOT NULL,
                        `aiLoadAnalysis` TEXT NOT NULL,
                        `aiGoalRecommendations` TEXT NOT NULL,
                        `aiNextSessionPlan` TEXT NOT NULL,
                        `userNotes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_logs_habitId_date` ON `habit_logs` (`habitId`, `date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_parentTaskId` ON `tasks` (`parentTaskId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColIfNotExists(db, "kinesitherapy_sessions", "userNotes", "TEXT NOT NULL DEFAULT ''")
                addColIfNotExists(db, "user_profile", "isOnboarded", "INTEGER NOT NULL DEFAULT 0")
            }

            private fun addColIfNotExists(db: SupportSQLiteDatabase, table: String, column: String, def: String) {
                var found = false
                try {
                    val cursor = db.query("PRAGMA table_info(`$table`)")
                    try {
                        val nameIndex = cursor.getColumnIndex("name")
                        while (cursor.moveToNext()) {
                            if (nameIndex != -1 && cursor.getString(nameIndex) == column) {
                                found = true
                                break
                            }
                        }
                    } finally {
                        cursor.close()
                    }
                } catch (e: Exception) {
                    // Ignore pragma inspection errors
                }

                if (!found) {
                    try {
                        db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $def")
                    } catch (e: Exception) {
                        // In case column was already present
                    }
                }
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6
        )

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pulsetrack.db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
