package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BoardElement
import com.example.data.model.BoardItem
import com.example.data.model.FocusSession
import com.example.data.model.NoteItem
import com.example.data.model.RoutineTask
import com.example.data.model.ScheduledScript
import com.example.data.model.TaskCompletionLog
import java.util.concurrent.ConcurrentHashMap

@Database(
    entities = [
        RoutineTask::class,
        TaskCompletionLog::class,
        FocusSession::class,
        ScheduledScript::class,
        NoteItem::class,
        BoardItem::class,
        BoardElement::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val userDbInstances = ConcurrentHashMap<String, AppDatabase>()

        fun sanitizeAccountKey(raw: String): String {
            val clean = raw.trim().lowercase().replace(Regex("[^a-zA-Z0-9_]"), "_")
            return if (clean.isBlank()) "guest_user" else clean
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_routine_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabaseForUser(context: Context, userAccountKey: String): AppDatabase {
            val safeKey = sanitizeAccountKey(userAccountKey)
            return userDbInstances.computeIfAbsent(safeKey) {
                val dbName = if (safeKey == "guest_user" || safeKey == "default") {
                    "daily_routine_database"
                } else {
                    "routine_db_${safeKey}"
                }
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }
        }
    }
}

