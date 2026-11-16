package com.example.data.sync

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.example.data.firebase.FirebaseConfig
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.GoogleAccount
import com.example.data.firebase.GoogleAuthManager
import com.example.data.model.BoardElement
import com.example.data.model.BoardItem
import com.example.data.model.FocusSession
import com.example.data.model.NoteItem
import com.example.data.model.RoutineTask
import com.example.data.model.ScheduledScript
import com.example.data.model.TaskCompletionLog
import com.example.data.repository.RoutineRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlin.math.abs

enum class SyncState {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    ERROR
}

/**
 * Cloud sync compatible with Wave Focus PWA.
 *
 * Google Sign-In → Firebase Auth → Firestore document
 * `users/{uid}/apps/wavefocus` with `{ updatedAt, state, device }`, where
 * `state` is the PWA schema-v2 object. Newest copy wins, exactly like
 * `pwa/js/sync.js`.
 */
class CloudSyncManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("wave_focus_sync_prefs", Context.MODE_PRIVATE)

    private val _account = MutableStateFlow(GoogleAuthManager.currentUser(appContext))
    val account: StateFlow<GoogleAccount?> = _account.asStateFlow()

    private val _syncKey = MutableStateFlow<String?>(_account.value?.uid)
    val syncKey: StateFlow<String?> = _syncKey.asStateFlow()

    private val _username = MutableStateFlow<String?>(_account.value?.email)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(prefs.getLong(KEY_LAST_SYNC, 0L))
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private var pendingChanges: Boolean = prefs.getBoolean(KEY_PENDING, false)

    companion object {
        private const val TAG = "CloudSyncManager"
        private const val KEY_LAST_SYNC = "cloud_last_sync_timestamp"
        private const val KEY_PENDING = "cloud_pending_changes"
        private const val KEY_MAP_TASKS = "cloud_id_map_tasks"
        private const val KEY_MAP_NOTES = "cloud_id_map_notes"
        private const val KEY_MAP_BOARDS = "cloud_id_map_boards"
        private const val KEY_MAP_QUESTS = "cloud_id_map_quests"

        @Volatile
        private var instance: CloudSyncManager? = null

        fun getInstance(context: Context): CloudSyncManager =
            instance ?: synchronized(this) {
                instance ?: CloudSyncManager(context.applicationContext).also { instance = it }
            }
    }

    // ---------------------------------------------------------------------
    // Auth
    // ---------------------------------------------------------------------

    fun isConfigured(): Boolean = GoogleAuthManager.isConfigured(appContext)

    fun isConnected(): Boolean = GoogleAuthManager.isSignedIn(appContext)

    suspend fun signIn(activity: Activity): Result<GoogleAccount> {
        _syncState.value = SyncState.SYNCING
        _syncMessage.value = "Signing in with Google…"
        val result = GoogleAuthManager.signIn(activity)
        result.onSuccess { user ->
            _account.value = user
            _syncKey.value = user.uid
            _username.value = user.email
            prefs.edit().putBoolean(KEY_PENDING, true).apply()
            pendingChanges = true
            _syncMessage.value = "Signed in as ${user.email}"
            _syncState.value = SyncState.IDLE
        }.onFailure { e ->
            _syncState.value = SyncState.ERROR
            _syncMessage.value = e.localizedMessage ?: "Google Sign-In failed"
        }
        return result
    }

    fun refreshAccount() {
        val user = GoogleAuthManager.currentUser(appContext)
        _account.value = user
        _syncKey.value = user?.uid
        _username.value = user?.email
    }

    fun disconnect() {
        GoogleAuthManager.signOut(appContext)
        _account.value = null
        _syncKey.value = null
        _username.value = null
        _syncState.value = SyncState.IDLE
        _syncMessage.value = "Signed out — local data stays on this device"
        _lastSyncTimestamp.value = 0L
        pendingChanges = false
        prefs.edit()
            .remove(KEY_LAST_SYNC)
            .putBoolean(KEY_PENDING, false)
            .remove(KEY_MAP_TASKS)
            .remove(KEY_MAP_NOTES)
            .remove(KEY_MAP_BOARDS)
            .remove(KEY_MAP_QUESTS)
            .apply()
    }

    /** Call after any local mutation so the next sync pushes instead of pulling. */
    fun markLocalChanged() {
        if (!pendingChanges) {
            pendingChanges = true
            prefs.edit().putBoolean(KEY_PENDING, true).apply()
        }
    }

    // ---------------------------------------------------------------------
    // Firestore plumbing
    // ---------------------------------------------------------------------

    private fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance(FirebaseManager.app(appContext))

    private fun docRef(): com.google.firebase.firestore.DocumentReference? {
        val uid = GoogleAuthManager.currentUser(appContext)?.uid ?: return null
        return firestore()
            .collection("users")
            .document(uid)
            .collection(FirebaseConfig.COLLECTION)
            .document(FirebaseConfig.DOCUMENT)
    }

    private fun deviceLabel(): String =
        listOf(Build.MANUFACTURER, Build.MODEL).filter { it.isNotBlank() }
            .joinToString(" ")
            .plus(" · Android")

    // ---------------------------------------------------------------------
    // Sync
    // ---------------------------------------------------------------------

    suspend fun performSync(repository: RoutineRepository): Result<String> = withContext(Dispatchers.IO) {
        if (!isConnected()) {
            _syncState.value = SyncState.IDLE
            return@withContext Result.failure(IllegalStateException("Sign in with Google to sync"))
        }

        _syncState.value = SyncState.SYNCING
        _syncMessage.value = "Syncing with Wave Focus cloud…"

        try {
            val ref = docRef() ?: return@withContext Result.failure(IllegalStateException("Not signed in"))
            val snapshot = ref.get().await()
            val cloudUpdatedAt = if (snapshot.exists()) (snapshot.get("updatedAt") as? Number)?.toLong() ?: 0L else 0L
            val localUpdatedAt = localUpdatedAt(repository)
            val canPull = snapshot.exists() &&
                !pendingChanges &&
                cloudUpdatedAt > localUpdatedAt &&
                cloudUpdatedAt > _lastSyncTimestamp.value

            if (canPull) {
                val state = readState(snapshot.get("state"))
                if (state == null) {
                    push(repository, ref)
                    return@withContext Result.success("Cloud copy was empty — uploaded local data")
                }
                applyState(repository, state)
                _lastSyncTimestamp.value = cloudUpdatedAt
                prefs.edit().putLong(KEY_LAST_SYNC, cloudUpdatedAt).putBoolean(KEY_PENDING, false).apply()
                pendingChanges = false
                _syncState.value = SyncState.SYNCED
                _syncMessage.value = "Restored the newest copy from the cloud"
                Result.success("Restored from cloud")
            } else if (!pendingChanges && snapshot.exists() && cloudUpdatedAt <= _lastSyncTimestamp.value) {
                _syncState.value = SyncState.SYNCED
                _syncMessage.value = "Everything is up to date"
                Result.success("Already up to date")
            } else {
                push(repository, ref)
                Result.success("Uploaded local data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            _syncState.value = SyncState.ERROR
            _syncMessage.value = "Sync failed: ${e.localizedMessage ?: "network or permission error"}"
            Result.failure(e)
        }
    }

    private suspend fun push(
        repository: RoutineRepository,
        ref: com.google.firebase.firestore.DocumentReference
    ) {
        val now = System.currentTimeMillis()
        val state = buildState(repository, now)
        ref.set(
            mapOf(
                "updatedAt" to now,
                "state" to jsonToPlainMap(state),
                "device" to deviceLabel()
            )
        ).await()

        _lastSyncTimestamp.value = now
        prefs.edit().putLong(KEY_LAST_SYNC, now).putBoolean(KEY_PENDING, false).apply()
        pendingChanges = false
        _syncState.value = SyncState.SYNCED
        _syncMessage.value = "Synced with Wave Focus cloud"
    }

    /**
     * Best-effort local change timestamp: newest edit across every synced table.
     * (Mirrors the PWA's single `state.updatedAt`.)
     */
    private suspend fun localUpdatedAt(repository: RoutineRepository): Long {
        var newest = 0L
        repository.getAllTasksSync().forEach { newest = maxOf(newest, it.updatedAt) }
        repository.getAllScriptsSync().forEach { newest = maxOf(newest, it.updatedAt) }
        repository.getAllLogsSync().forEach { newest = maxOf(newest, it.completedAt) }
        repository.getAllFocusSessionsSync().forEach { newest = maxOf(newest, it.completedAt) }
        repository.getAllNotesSync().forEach { newest = maxOf(newest, it.updatedAt) }
        repository.getAllBoardsSync().forEach { newest = maxOf(newest, it.updatedAt) }
        return newest
    }

    // ---------------------------------------------------------------------
    // Local → PWA state
    // ---------------------------------------------------------------------

    private suspend fun buildState(repository: RoutineRepository, now: Long): JSONObject {
        val tasks = repository.getAllTasksSync()
        val taskMap = readMap(KEY_MAP_TASKS)
        val usedTaskIds = mutableSetOf<String>()

        val taskArray = JSONArray()
        tasks.sortedBy { it.displayOrder }.forEach { task ->
            var cloudId = taskMap.optString(task.id.toString(), "")
            if (cloudId.isBlank() || usedTaskIds.contains(cloudId)) {
                cloudId = newCloudId("t")
            }
            usedTaskIds.add(cloudId)
            taskMap.put(task.id.toString(), cloudId)
            taskArray.put(taskToJson(task, cloudId))
        }
        writeMap(KEY_MAP_TASKS, taskMap)

        // Completions: { "YYYY-MM-DD": [cloudTaskId, ...] }
        val completions = JSONObject()
        repository.getAllLogsSync()
            .filter { it.isCompleted }
            .forEach { log ->
                val cloudId = taskMap.optString(log.taskId.toString(), "")
                if (cloudId.isNotBlank()) {
                    val date = LocalDate.ofEpochDay(log.dateEpochDay).toString()
                    val ids = completions.optJSONArray(date) ?: JSONArray()
                    if (!arrayContains(ids, cloudId)) ids.put(cloudId)
                    completions.put(date, ids)
                }
            }

        val sessions = JSONArray()
        repository.getAllFocusSessionsSync().forEach { session ->
            sessions.put(
                JSONObject()
                    .put("id", "s_${session.completedAt}_${session.durationSeconds}")
                    .put("date", LocalDate.ofEpochDay(session.dateEpochDay).toString())
                    .put("seconds", session.durationSeconds)
                    .put("mode", session.mode.lowercase())
                    .put("taskId", session.taskId?.let { taskMap.optString(it.toString(), "") } ?: "")
                    .put("taskTitle", session.taskTitle)
                    .put("endedAt", session.completedAt)
            )
        }

        val quests = JSONArray()
        val questMap = readMap(KEY_MAP_QUESTS)
        repository.getAllScriptsSync().forEach { script ->
            var cloudId = questMap.optString(script.id.toString(), "")
            if (cloudId.isBlank()) cloudId = newCloudId("q")
            questMap.put(script.id.toString(), cloudId)
            quests.put(
                JSONObject()
                    .put("id", cloudId)
                    .put("date", LocalDate.ofEpochDay(script.startDateEpochDay).toString())
                    .put("title", script.title)
                    .put("emoji", script.emoji)
                    .put("type", questTypeForPwa(script.questType))
                    .put("target", script.targetValue)
                    .put("xp", script.xpReward)
                    .put("done", script.isCompleted)
                    .put("createdAt", script.updatedAt)
            )
        }
        writeMap(KEY_MAP_QUESTS, questMap)

        // Standalone notes (boards carry their own cards in the PWA schema).
        val notes = JSONArray()
        val noteMap = readMap(KEY_MAP_NOTES)
        repository.getAllNotesSync().filter { it.boardId == null }.forEach { note ->
            var cloudId = noteMap.optString(note.id.toString(), "")
            if (cloudId.isBlank()) cloudId = newCloudId("n")
            noteMap.put(note.id.toString(), cloudId)
            notes.put(
                JSONObject()
                    .put("id", cloudId)
                    .put("title", note.title)
                    .put("content", note.content)
                    .put("color", note.colorHex)
                    .put("pinned", note.pinned)
                    .put("createdAt", note.createdAt)
                    .put("updatedAt", note.updatedAt)
            )
        }
        writeMap(KEY_MAP_NOTES, noteMap)

        // Boards: title + emoji + cards. Board elements become cards.
        val boards = JSONArray()
        val boardMap = readMap(KEY_MAP_BOARDS)
        val elements = repository.getAllBoardElementsSync()
        val boardNotes = repository.getAllNotesSync().filter { it.boardId != null }.groupBy { it.boardId!! }
        repository.getAllBoardsSync().forEach { board ->
            var cloudId = boardMap.optString(board.id.toString(), "")
            if (cloudId.isBlank()) cloudId = newCloudId("b")
            boardMap.put(board.id.toString(), cloudId)

            val cards = JSONArray()
            elements.filter { it.boardId == board.id }
                .sortedBy { it.createdAt }
                .forEach { element ->
                    cards.put(
                        JSONObject()
                            .put("id", "c_${element.id}")
                            .put("text", element.text)
                            .put("done", false)
                    )
                }
            boardNotes[board.id]?.sortedBy { it.createdAt }?.forEach { note ->
                cards.put(
                    JSONObject()
                        .put("id", "c_note_${note.id}")
                        .put("text", note.title.ifBlank { note.content.take(80) })
                        .put("done", false)
                )
            }

            boards.put(
                JSONObject()
                    .put("id", cloudId)
                    .put("title", board.title)
                    .put("emoji", "🗂️")
                    .put("color", board.backgroundColorHex)
                    .put("cards", cards)
                    .put("createdAt", board.createdAt)
            )
        }
        writeMap(KEY_MAP_BOARDS, boardMap)

        return JSONObject()
            .put("v", FirebaseConfig.SCHEMA_VERSION)
            .put("updatedAt", now)
            .put("settings", buildSettings())
            .put("categories", buildCategories())
            .put("tasks", taskArray)
            .put("completions", completions)
            .put("sessions", sessions)
            .put("quests", quests)
            .put("notes", notes)
            .put("boards", boards)
    }

    private fun buildSettings(): JSONObject {
        val routinePrefs = appContext.getSharedPreferences("daily_routine_prefs", Context.MODE_PRIVATE)
        return JSONObject()
            .put("theme", routinePrefs.getString("theme_mode", "system"))
            .put("lang", routinePrefs.getString("app_language", "en"))
            .put(
                "edition",
                if (routinePrefs.getString("active_app_edition", "standard") == "islamic") "islamic" else "standard"
            )
            .put("weekStart", 1)
            .put("notifications", true)
            .put("haptics", true)
            .put("onboarded", true)
    }

    private fun buildCategories(): JSONArray {
        val routinePrefs = appContext.getSharedPreferences("daily_routine_prefs", Context.MODE_PRIVATE)
        val defaults = listOf(
            "Health" to "#10B981",
            "Focus" to "#6366F1",
            "Mind" to "#14B8A6",
            "Productivity" to "#F97316",
            "Habit" to "#F43F5E"
        )
        val list = mutableListOf<Pair<String, String>>()
        val raw = routinePrefs.getString("custom_categories_json", null)
        if (raw != null) {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val name = obj.optString("name", "")
                    if (name.isNotBlank()) list.add(name to obj.optString("colorHex", "#6366F1"))
                }
            } catch (_: Exception) {
            }
        }
        if (list.isEmpty()) list.addAll(defaults)

        val result = JSONArray()
        list.forEach { (name, color) ->
            result.put(JSONObject().put("name", name).put("color", color))
        }
        return result
    }

    private fun taskToJson(task: RoutineTask, cloudId: String): JSONObject {
        val repeat = when {
            task.repeatType.equals("INTERVAL", true) -> "interval"
            task.repeatType.equals("SPECIFIC_DAY", true) || task.repeatType.equals("ONCE", true) -> "once"
            task.repeatType.equals("WEEKEND_ONCE", true) -> "weekend"
            else -> "weekly"
        }
        val days = task.daysOfWeek.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
        val anchor = if (task.startEpochDay > 0L) {
            LocalDate.ofEpochDay(task.startEpochDay).toString()
        } else {
            Instant.ofEpochMilli(task.createdAt).atZone(ZoneOffset.UTC).toLocalDate().toString()
        }

        return JSONObject()
            .put("id", cloudId)
            .put("title", task.title)
            .put("category", task.category)
            .put("timeOfDay", task.timeOfDay)
            .put("targetMinutes", task.targetMinutes)
            .put("repeatType", repeat)
            .put("days", JSONArray(days))
            .put("intervalDays", task.repeatIntervalDays.coerceAtLeast(1))
            .put("anchor", anchor)
            .put("note", "")
            .put("active", task.isActive)
            .put("createdAt", Instant.ofEpochMilli(task.createdAt).toString())
            .put("order", task.displayOrder)
    }

    private fun questTypeForPwa(questType: String): String = when {
        questType.contains("milestone", true) -> "milestone"
        questType.contains("epic", true) -> "epic"
        questType.contains("focus", true) -> "focus"
        else -> "daily"
    }

    // ---------------------------------------------------------------------
    // PWA state → local
    // ---------------------------------------------------------------------

    private suspend fun applyState(repository: RoutineRepository, state: JSONObject) {
        val taskArray = state.optJSONArray("tasks") ?: JSONArray()
        val newTasks = mutableListOf<Pair<String, RoutineTask>>()

        for (i in 0 until taskArray.length()) {
            val obj = taskArray.optJSONObject(i) ?: continue
            val cloudId = obj.optString("id", "")
            if (cloudId.isBlank()) continue
            newTasks.add(cloudId to jsonToTask(obj))
        }

        val scriptsArray = state.optJSONArray("quests") ?: JSONArray()
        val newScripts = mutableListOf<Pair<String, ScheduledScript>>()
        for (i in 0 until scriptsArray.length()) {
            val obj = scriptsArray.optJSONObject(i) ?: continue
            val cloudId = obj.optString("id", "")
            if (cloudId.isBlank()) continue
            newScripts.add(cloudId to jsonToScript(obj))
        }

        val notesArray = state.optJSONArray("notes") ?: JSONArray()
        val newNotes = mutableListOf<Pair<String, NoteItem>>()
        for (i in 0 until notesArray.length()) {
            val obj = notesArray.optJSONObject(i) ?: continue
            val cloudId = obj.optString("id", "")
            if (cloudId.isBlank()) continue
            newNotes.add(cloudId to jsonToNote(obj))
        }

        val boardsArray = state.optJSONArray("boards") ?: JSONArray()
        val newBoards = mutableListOf<Triple<String, BoardItem, List<Pair<String, Boolean>>>>()
        for (i in 0 until boardsArray.length()) {
            val obj = boardsArray.optJSONObject(i) ?: continue
            val cloudId = obj.optString("id", "")
            if (cloudId.isBlank()) continue
            val cards = mutableListOf<Pair<String, Boolean>>()
            val cardArray = obj.optJSONArray("cards") ?: JSONArray()
            for (c in 0 until cardArray.length()) {
                val card = cardArray.optJSONObject(c) ?: continue
                val text = card.optString("text", "")
                if (text.isNotBlank()) cards.add(text to card.optBoolean("done", false))
            }
            newBoards.add(
                Triple(
                    cloudId,
                    BoardItem(
                        title = obj.optString("title", "Board"),
                        backgroundColorHex = obj.optString("color", "#0F172A"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = System.currentTimeMillis()
                    ),
                    cards
                )
            )
        }

        // Clear everything that participates in sync.
        repository.dao.getAllBoardsSync().forEach { repository.deleteBoardById(it.id) }
        repository.dao.getAllBoardElementsSync().forEach { repository.dao.deleteBoardElementById(it.id) }
        repository.dao.getAllNotesSync().forEach { repository.deleteNoteById(it.id) }
        repository.dao.clearAllTasks()
        repository.dao.clearAllLogs()
        repository.dao.clearAllFocusSessions()
        repository.dao.clearAllScripts()

        // Insert tasks and rebuild the cloud ↔ local map.
        val taskMap = JSONObject()
        val taskIdMap = mutableMapOf<String, Long>()
        newTasks.forEach { (cloudId, task) ->
            val newId = repository.insertTask(task.copy(id = 0L))
            taskIdMap[cloudId] = newId
            taskMap.put(newId.toString(), cloudId)
        }
        writeMap(KEY_MAP_TASKS, taskMap)

        // Completions.
        val completions = state.optJSONObject("completions") ?: JSONObject()
        val logs = mutableListOf<TaskCompletionLog>()
        completions.keys().forEach { dateKey ->
            val epochDay = try {
                LocalDate.parse(dateKey).toEpochDay()
            } catch (_: Exception) {
                null
            } ?: return@forEach
            val ids = completions.optJSONArray(dateKey) ?: JSONArray()
            for (i in 0 until ids.length()) {
                val localId = taskIdMap[ids.optString(i)] ?: continue
                logs.add(
                    TaskCompletionLog(
                        taskId = localId,
                        dateEpochDay = epochDay,
                        isCompleted = true,
                        completedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        if (logs.isNotEmpty()) repository.dao.insertLogs(logs)

        // Sessions.
        val sessionsArray = state.optJSONArray("sessions") ?: JSONArray()
        val sessions = mutableListOf<FocusSession>()
        for (i in 0 until sessionsArray.length()) {
            val obj = sessionsArray.optJSONObject(i) ?: continue
            val endedAt = obj.optLong("endedAt", 0L)
            if (endedAt <= 0L) continue
            val date = obj.optString("date", "")
            val epochDay = try {
                LocalDate.parse(date).toEpochDay()
            } catch (_: Exception) {
                Instant.ofEpochMilli(endedAt).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
            }
            sessions.add(
                FocusSession(
                    taskId = taskIdMap[obj.optString("taskId", "")],
                    taskTitle = obj.optString("taskTitle", ""),
                    durationSeconds = obj.optInt("seconds", 0),
                    mode = obj.optString("mode", "focus").replaceFirstChar { it.uppercase() },
                    completedAt = endedAt,
                    dateEpochDay = epochDay
                )
            )
        }
        if (sessions.isNotEmpty()) repository.dao.insertFocusSessions(sessions)

        // Quests → ScheduledScript.
        val questMap = JSONObject()
        newScripts.forEach { (cloudId, script) ->
            val newId = repository.insertScript(script)
            questMap.put(newId.toString(), cloudId)
        }
        writeMap(KEY_MAP_QUESTS, questMap)

        // Notes.
        val noteMap = JSONObject()
        newNotes.forEach { (cloudId, note) ->
            val newId = repository.insertNote(note)
            noteMap.put(newId.toString(), cloudId)
        }
        writeMap(KEY_MAP_NOTES, noteMap)

        // Boards + cards as TEXT elements.
        val boardMap = JSONObject()
        newBoards.forEach { (cloudId, board, cards) ->
            val newId = repository.insertBoard(board)
            boardMap.put(newId.toString(), cloudId)
            cards.forEachIndexed { index, (text, _) ->
                repository.insertBoardElement(
                    BoardElement(
                        boardId = newId,
                        elementType = "TEXT",
                        text = text,
                        posX = 80f,
                        posY = 80f + index * 90f
                    )
                )
            }
        }
        writeMap(KEY_MAP_BOARDS, boardMap)
    }

    private fun jsonToTask(obj: JSONObject): RoutineTask {
        val repeat = obj.optString("repeatType", "weekly").lowercase()
        val daysArray = obj.optJSONArray("days")
        val days = if (daysArray != null) {
            (0 until daysArray.length()).mapNotNull { daysArray.optInt(it, 0).takeIf { d -> d in 1..7 } }
        } else {
            listOf(1, 2, 3, 4, 5, 6, 7)
        }
        val anchor = parseDay(obj.optString("anchor", ""))
        val repeatType = when (repeat) {
            "interval" -> "INTERVAL"
            "once" -> "SPECIFIC_DAY"
            "weekend" -> "WEEKEND_ONCE"
            else -> "WEEKLY"
        }
        val interval = obj.optInt("intervalDays", 1).coerceAtLeast(1)

        return RoutineTask(
            title = obj.optString("title", "Untitled"),
            category = obj.optString("category", "Health"),
            timeOfDay = obj.optString("timeOfDay", "Anytime"),
            targetMinutes = obj.optInt("targetMinutes", 0),
            daysOfWeek = (if (days.isEmpty()) listOf(1, 2, 3, 4, 5, 6, 7) else days).joinToString(","),
            repeatType = repeatType,
            repeatIntervalDays = interval,
            startEpochDay = if (repeatType == "WEEKLY") 0L else (anchor ?: LocalDate.now().toEpochDay()),
            isActive = obj.optBoolean("active", true),
            createdAt = parseInstant(obj.optString("createdAt", "")) ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            displayOrder = obj.optInt("order", 0)
        )
    }

    private fun jsonToScript(obj: JSONObject): ScheduledScript {
        val date = parseDay(obj.optString("date", "")) ?: LocalDate.now().toEpochDay()
        val type = obj.optString("type", "daily")
        return ScheduledScript(
            title = obj.optString("title", "Quest"),
            startDateEpochDay = date,
            endDateEpochDay = date,
            category = "Quest",
            updatedAt = obj.optLong("createdAt", System.currentTimeMillis()),
            isCompleted = obj.optBoolean("done", false),
            targetValue = obj.optString("target", ""),
            xpReward = obj.optInt("xp", 50),
            questType = when (type) {
                "milestone" -> "Milestone Goal"
                "epic" -> "Epic Challenge"
                "focus" -> "Focus Target"
                else -> "Daily Quest"
            },
            emoji = obj.optString("emoji", "🎯")
        )
    }

    private fun jsonToNote(obj: JSONObject): NoteItem = NoteItem(
        boardId = null,
        title = obj.optString("title", ""),
        content = obj.optString("content", ""),
        colorHex = obj.optString("color", "#1E293B"),
        pinned = obj.optBoolean("pinned", false),
        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
    )

    private fun parseDay(value: String): Long? = try {
        if (value.isBlank()) null else LocalDate.parse(value.take(10)).toEpochDay()
    } catch (_: DateTimeParseException) {
        null
    }

    private fun parseInstant(value: String): Long? = try {
        if (value.isBlank()) null else Instant.parse(value).toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }

    // ---------------------------------------------------------------------
    // Backups (PWA-compatible JSON)
    // ---------------------------------------------------------------------

    suspend fun exportBackupJson(repository: RoutineRepository): String = withContext(Dispatchers.IO) {
        val state = buildState(repository, System.currentTimeMillis())
        JSONObject()
            .put("app", "wavefocus")
            .put("schema", FirebaseConfig.SCHEMA_VERSION)
            .put("data", state)
            .toString(2)
    }

    suspend fun importBackupJson(jsonString: String, repository: RoutineRepository): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val root = JSONObject(jsonString)
                val state = root.optJSONObject("data") ?: if (root.has("tasks")) root else null
                if (state == null) {
                    return@withContext Result.failure(IllegalArgumentException("Not a Wave Focus backup"))
                }
                applyState(repository, state)
                markLocalChanged()
                Result.success(state.optJSONArray("tasks")?.length() ?: 0)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ---------------------------------------------------------------------
    // JSON helpers
    // ---------------------------------------------------------------------

    private fun readState(value: Any?): JSONObject? = when (value) {
        is Map<*, *> -> mapToJson(value)
        is String -> try {
            JSONObject(value)
        } catch (_: Exception) {
            null
        }
        else -> null
    }

    private fun jsonToPlainMap(json: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        json.keys().forEach { key ->
            map[key] = plainValue(json.opt(key))
        }
        return map
    }

    private fun plainValue(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> jsonToPlainMap(value)
        is JSONArray -> (0 until value.length()).map { plainValue(value.opt(it)) }
        else -> value
    }

    private fun mapToJson(map: Map<*, *>): JSONObject {
        val json = JSONObject()
        map.forEach { (key, value) ->
            json.put(key?.toString() ?: "", jsonValue(value))
        }
        return json
    }

    private fun jsonValue(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is Map<*, *> -> mapToJson(value)
        is List<*> -> JSONArray().also { array -> value.forEach { array.put(jsonValue(it)) } }
        is Array<*> -> JSONArray().also { array -> value.forEach { array.put(jsonValue(it)) } }
        else -> value
    }

    private fun readMap(key: String): JSONObject = try {
        JSONObject(prefs.getString(key, "{}") ?: "{}")
    } catch (_: Exception) {
        JSONObject()
    }

    private fun writeMap(key: String, value: JSONObject) {
        prefs.edit().putString(key, value.toString()).apply()
    }

    private fun newCloudId(prefix: String): String {
        val raw = abs(UUID.randomUUID().mostSignificantBits).toString(36)
        return "${prefix}_$raw"
    }

    private fun arrayContains(array: JSONArray, value: String): Boolean {
        for (i in 0 until array.length()) {
            if (array.optString(i) == value) return true
        }
        return false
    }
}
