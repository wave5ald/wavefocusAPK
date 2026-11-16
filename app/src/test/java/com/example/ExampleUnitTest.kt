package com.example

import com.example.data.model.DaySummary
import com.example.data.model.FocusSession
import com.example.data.model.NoteItem
import com.example.data.model.PresetTaskItem
import com.example.data.model.RoutinePreset
import com.example.data.model.RoutineTask
import com.example.data.model.ScheduledPresetPlan
import com.example.util.CategoryLocalization
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun categoryLocalization_translatesOnlyMainCategories() {
    // Built-in categories identify as main
    assertTrue(CategoryLocalization.isMainCategory("Health"))
    assertTrue(CategoryLocalization.isMainCategory("Focus"))
    assertTrue(CategoryLocalization.isMainCategory("Mind"))
    assertTrue(CategoryLocalization.isMainCategory("Habit"))
    assertTrue(CategoryLocalization.isMainCategory("Other"))
    assertTrue(CategoryLocalization.isMainCategory("الصحة"))

    // Custom user categories must NOT identify as main
    assertFalse(CategoryLocalization.isMainCategory("Gym Workouts"))
    assertFalse(CategoryLocalization.isMainCategory("Spanish Study"))

    // Main categories translate to Arabic
    assertEquals("الصحة", CategoryLocalization.getLocalizedCategoryName("Health", isArabic = true))
    assertEquals("التركيز", CategoryLocalization.getLocalizedCategoryName("Focus", isArabic = true))
    assertEquals("الذهن", CategoryLocalization.getLocalizedCategoryName("Mind", isArabic = true))
    assertEquals("العادة", CategoryLocalization.getLocalizedCategoryName("Habit", isArabic = true))
    assertEquals("أخرى", CategoryLocalization.getLocalizedCategoryName("Other", isArabic = true))

    // Main categories translate to English
    assertEquals("Health", CategoryLocalization.getLocalizedCategoryName("الصحة", isArabic = false))
    assertEquals("Focus", CategoryLocalization.getLocalizedCategoryName("التركيز", isArabic = false))

    // Custom categories MUST NOT be translated (user-defined names remain intact)
    assertEquals("Gym Workouts", CategoryLocalization.getLocalizedCategoryName("Gym Workouts", isArabic = true))
    assertEquals("Gym Workouts", CategoryLocalization.getLocalizedCategoryName("Gym Workouts", isArabic = false))
    assertEquals("قراءة متقدمة", CategoryLocalization.getLocalizedCategoryName("قراءة متقدمة", isArabic = false))
  }

  @Test
  fun routinePreset_creationAndScheduling() {
    val preset = RoutinePreset(
      name = "Weekend Sprint",
      description = "Dedicated weekend focus",
      iconEmoji = "🏃",
      colorHex = "#10B981",
      tasks = listOf(
        PresetTaskItem("Morning 5k Run", "Health", "Morning", 30),
        PresetTaskItem("Reading Session", "Mind", "Afternoon", 45)
      )
    )
    assertEquals("Weekend Sprint", preset.name)
    assertEquals(2, preset.tasks.size)

    val plan = ScheduledPresetPlan(
      presetId = preset.id,
      presetName = preset.name,
      startDateEpochDay = LocalDate.now().plusDays(3).toEpochDay(),
      replaceExisting = true
    )
    assertEquals(preset.id, plan.presetId)
    assertTrue(plan.replaceExisting)
    assertFalse(plan.isApplied)
  }

  @Test
  fun routineTask_defaultValues() {
    val task = RoutineTask(
      id = 1L,
      title = "Morning Meditation",
      category = "Mind",
      timeOfDay = "Morning",
      targetMinutes = 15,
      displayOrder = 0
    )
    assertEquals("Morning Meditation", task.title)
    assertEquals("Mind", task.category)
    assertEquals("Morning", task.timeOfDay)
    assertEquals(15, task.targetMinutes)
    assertEquals(0, task.displayOrder)
    assertTrue(task.isActive)
  }

  @Test
  fun focusSession_durationCalculation() {
    val session = FocusSession(
      id = 10L,
      taskTitle = "Deep Work",
      durationSeconds = 1500,
      mode = "Pomodoro",
      dateEpochDay = LocalDate.now().toEpochDay()
    )
    assertEquals(1500, session.durationSeconds)
    assertEquals("Deep Work", session.taskTitle)
    assertEquals("Pomodoro", session.mode)
  }

  @Test
  fun noteItem_creation() {
    val note = NoteItem(
      id = 1L,
      title = "Idea Scratchpad",
      content = "Ship local-first Android app",
      colorHex = "#2196F3"
    )
    assertEquals("Idea Scratchpad", note.title)
    assertEquals("Ship local-first Android app", note.content)
    assertEquals("#2196F3", note.colorHex)
  }

  @Test
  fun daySummary_completionRate() {
    val summary = DaySummary(
      epochDay = 20000L,
      totalTasks = 5,
      completedTasks = 4,
      completionPercentage = 80.0f,
      focusMinutes = 50
    )
    assertEquals(4, summary.completedTasks)
    assertEquals(5, summary.totalTasks)
    assertEquals(80.0f, summary.completionPercentage, 0.01f)
    assertEquals(50, summary.focusMinutes)
  }
}
