package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.FocusSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        try {
            val action = intent?.action
            if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
                val prefs = context.getSharedPreferences(FocusTimerService.PREFS_NAME, Context.MODE_PRIVATE)
                val isRunning = prefs.getBoolean(FocusTimerService.KEY_IS_RUNNING, false)
                if (!isRunning) return

                val endTime = prefs.getLong(FocusTimerService.KEY_END_TIME, 0L)
                val now = System.currentTimeMillis()
                val totalSecs = prefs.getInt(FocusTimerService.KEY_TOTAL_SECONDS, 25 * 60)
                val taskTitle = prefs.getString(FocusTimerService.KEY_TASK_TITLE, "Deep Focus") ?: "Deep Focus"
                val taskId = prefs.getLong(FocusTimerService.KEY_TASK_ID, -1L)
                val mode = prefs.getString(FocusTimerService.KEY_MODE, "Focus") ?: "Focus"

                if (endTime > now) {
                    // Resume ongoing focus timer in background service!
                    val remainingSeconds = ((endTime - now) / 1000L).toInt()
                    FocusTimerService.start(
                        context = context,
                        totalSeconds = totalSecs,
                        remainingSeconds = remainingSeconds,
                        taskTitle = taskTitle,
                        taskId = if (taskId > 0) taskId else null,
                        mode = mode
                    )
                } else {
                    // Timer finished while device was powered off! Record session and notify user.
                    prefs.edit().clear().apply()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context.applicationContext)
                            val session = FocusSession(
                                taskId = if (taskId > 0) taskId else null,
                                taskTitle = taskTitle,
                                durationSeconds = totalSecs,
                                mode = mode,
                                completedAt = endTime,
                                dateEpochDay = LocalDate.now().toEpochDay()
                            )
                            db.routineDao().insertFocusSession(session)
                        } catch (ignored: Exception) {}
                    }

                    showSessionFinishedNotification(context, taskTitle)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSessionFinishedNotification(context: Context, taskTitle: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FocusTimerService.CHANNEL_ID,
                "Focus Session Timer",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FocusTimerService.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Focus Session Complete")
            .setContentText("Your session '$taskTitle' completed while phone was restarted.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(FocusTimerService.NOTIFICATION_ID + 1, notification)
    }
}
