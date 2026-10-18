package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.WallpaperViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdminFeaturesTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var application: Application
    private lateinit var viewModel: WallpaperViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        // Clear prefs before test
        application.getSharedPreferences("orbitwall_prefs", Application.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        viewModel = WallpaperViewModel(application)
        ShadowLooper.idleMainLooper()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val TEST_SECRET_PASS = "17" + "288"

    @Test
    fun `admin login with valid credentials succeeds`() {
        val loginSuccess = viewModel.loginAdmin("Fahit", TEST_SECRET_PASS)
        assertTrue("Admin login should succeed with valid credentials", loginSuccess)

        val uiState = viewModel.uiState.value
        assertTrue("isAdmin should be true", uiState.isAdmin)
        assertTrue("isAdminConsoleOpen should be true", uiState.isAdminConsoleOpen)
    }

    @Test
    fun `admin login with invalid credentials fails`() {
        val loginFail = viewModel.loginAdmin("Hacker", "99999")
        assertFalse("Admin login should fail with wrong code", loginFail)

        val uiState = viewModel.uiState.value
        assertFalse("isAdmin should be false", uiState.isAdmin)
        assertFalse("isAdminConsoleOpen should be false", uiState.isAdminConsoleOpen)
    }

    @Test
    fun `admin logout resets admin state`() {
        viewModel.loginAdmin("Admin", TEST_SECRET_PASS)
        assertTrue(viewModel.uiState.value.isAdmin)

        viewModel.logoutAdmin()
        assertFalse(viewModel.uiState.value.isAdmin)
        assertFalse(viewModel.uiState.value.isAdminConsoleOpen)
    }

    @Test
    fun `toggle featured wallpaper updates featured list and home section`() {
        val testWallpaperId = "paper-2k-crimson-eclipse-orbit"
        val initialFeatured = viewModel.uiState.value.featuredWallpaperIds.contains(testWallpaperId)

        viewModel.toggleFeaturedWallpaper(testWallpaperId)
        val afterFirstToggle = viewModel.uiState.value.featuredWallpaperIds.contains(testWallpaperId)
        assertEquals(!initialFeatured, afterFirstToggle)

        // Toggle back
        viewModel.toggleFeaturedWallpaper(testWallpaperId)
        val afterSecondToggle = viewModel.uiState.value.featuredWallpaperIds.contains(testWallpaperId)
        assertEquals(initialFeatured, afterSecondToggle)
    }

    @Test
    fun `delete wallpaper removes it from catalog and featured`() {
        val targetId = "paper-2k-crimson-eclipse-orbit"

        // Make sure it is featured first
        if (!viewModel.uiState.value.featuredWallpaperIds.contains(targetId)) {
            viewModel.toggleFeaturedWallpaper(targetId)
        }
        assertTrue(viewModel.uiState.value.featuredWallpaperIds.contains(targetId))

        // Delete the wallpaper
        viewModel.deleteWallpaper(targetId)

        val updatedState = viewModel.uiState.value
        assertTrue("Deleted ID should be in deletedWallpaperIds", updatedState.deletedWallpaperIds.contains(targetId))
        assertFalse("Wallpaper should be removed from wallpapers list", updatedState.wallpapers.any { it.id == targetId })
        assertFalse("Deleted wallpaper should not be in featured list", updatedState.featuredWallpaperIds.contains(targetId))
        assertFalse("Deleted wallpaper should not be in computed featuredWallpapers", updatedState.featuredWallpapers.any { it.id == targetId })
    }

    @Test
    fun `toggle onboarding wallpaper updates onboarding list and onboardingWallpapers`() {
        val testWallpaperId = "paper-minimal-capsule-stack"
        val initialOnboarding = viewModel.uiState.value.onboardingWallpaperIds.contains(testWallpaperId)

        viewModel.toggleOnboardingWallpaper(testWallpaperId)
        val afterFirstToggle = viewModel.uiState.value.onboardingWallpaperIds.contains(testWallpaperId)
        assertEquals(!initialOnboarding, afterFirstToggle)

        // Toggle back
        viewModel.toggleOnboardingWallpaper(testWallpaperId)
        val afterSecondToggle = viewModel.uiState.value.onboardingWallpaperIds.contains(testWallpaperId)
        assertEquals(initialOnboarding, afterSecondToggle)
    }

    @Test
    fun `delete wallpaper removes it from onboarding list as well`() {
        val targetId = "paper-itachi-red-moon"

        // Make sure it is in onboarding set first
        if (!viewModel.uiState.value.onboardingWallpaperIds.contains(targetId)) {
            viewModel.toggleOnboardingWallpaper(targetId)
        }
        assertTrue(viewModel.uiState.value.onboardingWallpaperIds.contains(targetId))

        // Delete the wallpaper
        viewModel.deleteWallpaper(targetId)

        val updatedState = viewModel.uiState.value
        assertTrue("Deleted ID should be in deletedWallpaperIds", updatedState.deletedWallpaperIds.contains(targetId))
        assertFalse("Wallpaper should be removed from wallpapers list", updatedState.wallpapers.any { it.id == targetId })
        assertFalse("Deleted wallpaper should not be in onboarding list", updatedState.onboardingWallpaperIds.contains(targetId))
        assertFalse("Deleted wallpaper should not be in computed onboardingWallpapers", updatedState.onboardingWallpapers.any { it.id == targetId })
    }

    @Test
    fun `previewing onboarding flag toggles correctly`() {
        assertFalse(viewModel.uiState.value.isPreviewingOnboarding)
        viewModel.setPreviewingOnboarding(true)
        assertTrue(viewModel.uiState.value.isPreviewingOnboarding)
        viewModel.setPreviewingOnboarding(false)
        assertFalse(viewModel.uiState.value.isPreviewingOnboarding)
    }

    @Test
    fun `promote wallpaper updates promoted list and removes from demoted`() {
        val targetWallpaperId = "paper-custom-target-1"
        viewModel.demoteWallpaper(targetWallpaperId)
        assertTrue(viewModel.uiState.value.demotedWallpaperIds.contains(targetWallpaperId))

        viewModel.promoteWallpaper(targetWallpaperId)
        val updatedState = viewModel.uiState.value
        assertTrue("Promoted IDs should contain target", updatedState.promotedWallpaperIds.contains(targetWallpaperId))
        assertFalse("Promoted wallpaper must not be in demoted list", updatedState.demotedWallpaperIds.contains(targetWallpaperId))

        // Toggle promote off
        viewModel.promoteWallpaper(targetWallpaperId)
        assertFalse("Toggling promote again should remove it from promoted IDs", viewModel.uiState.value.promotedWallpaperIds.contains(targetWallpaperId))
    }

    @Test
    fun `demote wallpaper updates demoted list and removes from promoted`() {
        val targetWallpaperId = "paper-custom-target-2"
        viewModel.promoteWallpaper(targetWallpaperId)
        assertTrue(viewModel.uiState.value.promotedWallpaperIds.contains(targetWallpaperId))

        viewModel.demoteWallpaper(targetWallpaperId)
        val updatedState = viewModel.uiState.value
        assertTrue("Demoted IDs should contain target", updatedState.demotedWallpaperIds.contains(targetWallpaperId))
        assertFalse("Demoted wallpaper must not be in promoted list", updatedState.promotedWallpaperIds.contains(targetWallpaperId))

        // Toggle demote off
        viewModel.demoteWallpaper(targetWallpaperId)
        assertFalse("Toggling demote again should remove it from demoted IDs", viewModel.uiState.value.demotedWallpaperIds.contains(targetWallpaperId))
    }

    @Test
    fun `totalAppDownloads computes valid number`() {
        val total = viewModel.uiState.value.totalAppDownloads
        assertTrue("Total app downloads should be non-negative", total >= 0L)
    }

    @Test
    fun `updateWallpaperDownloadCount updates downloads for a specific wallpaper`() {
        val wallpapers = viewModel.uiState.value.wallpapers
        if (wallpapers.isNotEmpty()) {
            val target = wallpapers.first()
            val newCount = 7777L
            viewModel.updateWallpaperDownloadCount(target.id, newCount)

            val updatedTarget = viewModel.uiState.value.wallpapers.find { it.id == target.id }
            assertNotNull(updatedTarget)
            assertEquals(newCount, updatedTarget!!.downloads)
        }
    }

    @Test
    fun `updateGlobalTotalDownloads sets and resets total downloads override`() {
        val testOverride = 999999L
        viewModel.updateGlobalTotalDownloads(testOverride)
        assertEquals(testOverride, viewModel.uiState.value.totalAppDownloads)

        // Reset to catalog sum
        viewModel.updateGlobalTotalDownloads(null)
        val catalogSum = viewModel.uiState.value.wallpapers.sumOf { it.downloads }
        assertEquals(catalogSum, viewModel.uiState.value.totalAppDownloads)
    }

    @Test
    fun `resetInflatedDownloadStats clears overrides and resets state`() {
        viewModel.updateGlobalTotalDownloads(123456L)
        assertEquals(123456L, viewModel.uiState.value.totalAppDownloads)

        viewModel.resetInflatedDownloadStats()
        ShadowLooper.idleMainLooper()

        val updatedOverride = viewModel.uiState.value.globalTotalDownloadsOverride
        assertEquals(null, updatedOverride)
    }
}
