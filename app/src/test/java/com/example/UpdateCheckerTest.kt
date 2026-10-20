package com.example

import com.example.util.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun testExtractVersion() {
        assertEquals("1.1.5", UpdateChecker.extractVersion("1.1.5"))
        assertEquals("1.1.5", UpdateChecker.extractVersion("v1.1.5"))
        assertEquals("1.1.5", UpdateChecker.extractVersion("orbitwall-v1.1.5"))
        assertEquals("1.1.3", UpdateChecker.extractVersion("orbitwall-v1.1.3"))
        assertEquals("1.1.5", UpdateChecker.extractVersion("Orbitwall v1.1.5 Release"))
        assertEquals("1.1.3", UpdateChecker.extractVersion("Orbitwall-v1.1.3.apk"))
    }

    @Test
    fun testIsNewerVersion() {
        // Newer versions
        assertTrue(UpdateChecker.isNewerVersion(remote = "1.1.6", current = "1.1.5"))
        assertTrue(UpdateChecker.isNewerVersion(remote = "v1.2.0", current = "1.1.5"))
        assertTrue(UpdateChecker.isNewerVersion(remote = "orbitwall-v2.0.0", current = "1.1.5"))
        assertTrue(UpdateChecker.isNewerVersion(remote = "orbitwall-v1.1.6", current = "1.1.5"))
        assertTrue(UpdateChecker.isNewerVersion(remote = "1.1.5.1", current = "1.1.5"))

        // Equal versions
        assertFalse(UpdateChecker.isNewerVersion(remote = "1.1.5", current = "1.1.5"))
        assertFalse(UpdateChecker.isNewerVersion(remote = "v1.1.5", current = "1.1.5"))
        assertFalse(UpdateChecker.isNewerVersion(remote = "orbitwall-v1.1.5", current = "1.1.5"))

        // Older versions
        assertFalse(UpdateChecker.isNewerVersion(remote = "1.1.4", current = "1.1.5"))
        assertFalse(UpdateChecker.isNewerVersion(remote = "v1.1.3", current = "1.1.5"))
        assertFalse(UpdateChecker.isNewerVersion(remote = "orbitwall-v1.1.3", current = "1.1.5"))
        assertFalse(UpdateChecker.isNewerVersion(remote = "1.0.0", current = "1.1.5"))
    }

    @Test
    fun testCleanReleaseNotes() {
        val raw = "### Features\n• **Cloud Admin Sync**\n• `Google Sheets` telemetry\n__Optimized__"
        val cleaned = UpdateChecker.cleanReleaseNotes(raw)
        assertFalse(cleaned.contains("###"))
        assertFalse(cleaned.contains("**"))
        assertFalse(cleaned.contains("`"))
        assertFalse(cleaned.contains("__"))
        assertTrue(cleaned.contains("Features"))
        assertTrue(cleaned.contains("Cloud Admin Sync"))
    }
}
