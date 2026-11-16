package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.CryptoManager
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.RoutineViewModel
import com.example.ui.viewmodel.TimerMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Wave Focus", appName)
  }

  @Test
  fun `verify offline crypto encryption and decryption`() {
    val plainText = "My secret scheduled script for reflection"
    val (cipher, iv) = CryptoManager.encrypt(plainText)
    val decrypted = CryptoManager.decrypt(cipher, iv)
    assertEquals(plainText, decrypted)
  }

  @Test
  fun `verify ViewModel initialization and database startup`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = RoutineViewModel(app)
    assertNotNull(vm)
    assertTrue("Local user should be signed in by default in offline mode", vm.isUserSignedIn.value)
    assertEquals(NavTab.ROUTINE, vm.currentTab.value)
  }

  @Test
  fun `verify tab navigation in ViewModel`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = RoutineViewModel(app)

    vm.setTab(NavTab.FOCUS)
    assertEquals(NavTab.FOCUS, vm.currentTab.value)

    vm.setTab(NavTab.SCRIPTS)
    assertEquals(NavTab.SCRIPTS, vm.currentTab.value)

    vm.setTab(NavTab.NOTES)
    assertEquals(NavTab.NOTES, vm.currentTab.value)

    vm.setTab(NavTab.ANALYTICS)
    assertEquals(NavTab.ANALYTICS, vm.currentTab.value)
  }

  @Test
  fun `verify timer modes and duration`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = RoutineViewModel(app)

    vm.setTimerMode(TimerMode.POMODORO)
    assertEquals(TimerMode.POMODORO, vm.timerMode.value)
    assertEquals(25 * 60, vm.timerTotalSeconds.value)

    vm.setTimerMode(TimerMode.SHORT_BREAK)
    assertEquals(TimerMode.SHORT_BREAK, vm.timerMode.value)
    assertEquals(5 * 60, vm.timerTotalSeconds.value)

    vm.setTimerMode(TimerMode.DEEP_WORK)
    assertEquals(TimerMode.DEEP_WORK, vm.timerMode.value)
    assertEquals(50 * 60, vm.timerTotalSeconds.value)
  }

  @Test
  fun `verify language toggle and theme modes`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = RoutineViewModel(app)

    val initialLang = vm.appLanguage.value
    vm.toggleLanguage()
    val toggledLang = vm.appLanguage.value
    assertNotEquals(initialLang, toggledLang)

    vm.setThemeMode("dark")
    assertEquals("dark", vm.themeMode.value)

    vm.setThemeMode("light")
    assertEquals("light", vm.themeMode.value)
  }

  @Test
  fun `verify built-in routine presets and date scheduling`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = RoutineViewModel(app)

    // Curated presets are loaded out-of-the-box
    val presets = vm.allPresets.value
    assertTrue("Curated presets should exist", presets.size >= 4)
    val workdayPreset = presets.find { it.id == "preset_workday" }
    assertNotNull(workdayPreset)
    assertTrue("Workday preset must have tasks", workdayPreset!!.tasks.isNotEmpty())

    // Schedule a preset for 2 days from now
    val scheduleDate = LocalDate.now().plusDays(2)
    vm.schedulePresetForDate(workdayPreset, scheduleDate, replaceExisting = true, note = "Team sprint")
    
    val scheduledList = vm.scheduledPresets.value
    assertTrue("Scheduled list must contain new plan", scheduledList.any { it.presetId == workdayPreset.id })
    val plan = scheduledList.find { it.presetId == workdayPreset.id }!!
    assertEquals(scheduleDate.toEpochDay(), plan.startDateEpochDay)

    // Cancel scheduled preset
    vm.cancelScheduledPreset(plan.id)
    assertFalse("Cancelled plan must not be present", vm.scheduledPresets.value.any { it.id == plan.id })
  }

  @Test
  fun `verify main categories are preserved and cannot be removed`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = RoutineViewModel(app)

    val categories = vm.allCategories.value
    val categoryNames = categories.map { it.name }
    assertTrue(categoryNames.contains("Health"))
    assertTrue(categoryNames.contains("Focus"))
    assertTrue(categoryNames.contains("Mind"))
    assertTrue(categoryNames.contains("Habit"))
    assertTrue(categoryNames.contains("Other"))

    // Attempting to delete a default built-in category should be a no-op
    vm.deleteCategory("Health")
    val afterDelete = vm.allCategories.value.map { it.name }
    assertTrue("Default main category 'Health' must remain intact", afterDelete.contains("Health"))
  }
}
