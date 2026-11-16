package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.FocusSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

class FocusTimerService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    companion object {
        const val CHANNEL_ID = "wave_focus_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.ACTION_START"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_STOP = "com.example.ACTION_STOP"

        const val PREFS_NAME = "focus_timer_prefs"
        const val KEY_END_TIME = "target_end_time"
        const val KEY_TOTAL_SECONDS = "total_seconds"
        const val KEY_IS_RUNNING = "is_running"
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_TASK_ID = "task_id"
        const val KEY_MODE = "timer_mode"

        fun start(context: Context, totalSeconds: Int, remainingSeconds: Int, taskTitle: String, taskId: Long?, mode: String) {
            val endTime = System.currentTimeMillis() + (remainingSeconds * 1000L)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_END_TIME, endTime)
                .putInt(KEY_TOTAL_SECONDS, totalSeconds)
                .putBoolean(KEY_IS_RUNNING, true)
                .putString(KEY_TASK_TITLE, taskTitle)
                .putLong(KEY_TASK_ID, taskId ?: -1L)
                .putString(KEY_MODE, mode)
                .apply()

            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        acquireWakeLock()
        try {
            when (intent?.action) {
                ACTION_START -> {
                    startForegroundCompat("Focusing...", 0)
                    startTimerTicker()
                }
                ACTION_PAUSE -> {
                    pauseTimer()
                }
                ACTION_STOP -> {
                    stopTimer()
                }
                else -> {
                    startForegroundCompat("Focusing...", 0)
                    startTimerTicker()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat(statusText: String, remainingSeconds: Int) {
        val notification = buildNotification(statusText, remainingSeconds)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Exception) {}
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WaveFocus::TimerWakeLock"
            )?.apply {
                setReferenceCounted(false)
            }
        }
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24h safety timeout, stays awake while screen is off
            }
        } catch (ignored: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (ignored: Exception) {}
        wakeLock = null
    }

    private fun startTimerTicker() {
        serviceJob?.cancel()
        serviceJob = serviceScope.launch {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            while (isActive) {
                val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
                if (!isRunning) {
                    break
                }
                val endTime = prefs.getLong(KEY_END_TIME, 0L)
                val now = System.currentTimeMillis()
                val remainingSeconds = ((endTime - now) / 1000L).coerceAtLeast(0L).toInt()

                val taskTitle = prefs.getString(KEY_TASK_TITLE, "Deep Focus") ?: "Deep Focus"

                if (remainingSeconds <= 0) {
                    onTimerFinished(taskTitle)
                    break
                }

                updateNotification(taskTitle, remainingSeconds)
                delay(1000)
            }
        }
    }

    private fun pauseTimer() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        val remaining = ((endTime - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()

        prefs.edit()
            .putBoolean(KEY_IS_RUNNING, false)
            .putLong(KEY_END_TIME, 0L)
            .putInt("saved_remaining_seconds", remaining)
            .apply()

        serviceJob?.cancel()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification("Timer Paused (${formatTime(remaining)})", remaining, isPaused = true))
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_DETACH)
    }

    private fun stopTimer() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        serviceJob?.cancel()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerFinished(taskTitle: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalSecs = prefs.getInt(KEY_TOTAL_SECONDS, 25 * 60)
        val taskId = prefs.getLong(KEY_TASK_ID, -1L)
        val mode = prefs.getString(KEY_MODE, "Focus") ?: "Focus"
        prefs.edit().clear().apply()

        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val session = FocusSession(
                    taskId = if (taskId > 0) taskId else null,
                    taskTitle = taskTitle,
                    durationSeconds = totalSecs,
                    mode = mode,
                    completedAt = System.currentTimeMillis(),
                    dateEpochDay = LocalDate.now().toEpochDay()
                )
                db.routineDao().insertFocusSession(session)
            } catch (ignored: Exception) {}
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val finishedNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Focus Session Complete!")
            .setContentText("Great job on '$taskTitle'!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent())
            .build()
        notificationManager.notify(NOTIFICATION_ID, finishedNotification)

        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun updateNotification(taskTitle: String, remainingSeconds: Int) {
        val notification = buildNotification("$taskTitle - ${formatTime(remainingSeconds)} remaining", remainingSeconds)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(statusText: String, remainingSeconds: Int, isPaused: Boolean = false): Notification {
        val openAppPendingIntent = createOpenAppIntent()

        val pauseIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isPaused) "Focus Paused" else "Focus Active in Background")
            .setContentText(statusText)
            .setOngoing(!isPaused)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (!isPaused) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        }
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        return builder.build()
    }

    private fun createOpenAppIntent(): PendingIntent {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Focus Session Timer"
            val descriptionText = "Displays ongoing focus countdown while screen is off or in background"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        serviceJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
