package com.ssafy.s15p21a206.tiger.core.session

import com.ssafy.s15p21a206.tiger.core.model.capture.RecordingResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionVideoTest {
    @Test
    fun rotationFreeCaptureKeepsStoredSize() {
        assertEquals(
            RecordingResolution(1920, 1080),
            displayResolution(width = 1920, height = 1080, rotationDegrees = 0),
        )
    }

    @Test
    fun quarterTurnCaptureSwapsSizeToWhatThePlayerShows() {
        assertEquals(
            RecordingResolution(1080, 1920),
            displayResolution(width = 1920, height = 1080, rotationDegrees = 90),
        )
        assertEquals(
            RecordingResolution(1080, 1920),
            displayResolution(width = 1920, height = 1080, rotationDegrees = 270),
        )
    }

    @Test
    fun halfTurnCaptureKeepsStoredSize() {
        assertEquals(
            RecordingResolution(1280, 720),
            displayResolution(width = 1280, height = 720, rotationDegrees = 180),
        )
    }

    @Test
    fun missingRotationIsTreatedAsNone() {
        assertEquals(
            RecordingResolution(1280, 720),
            displayResolution(width = 1280, height = 720, rotationDegrees = null),
        )
    }

    @Test
    fun missingOrNonPositiveSizeIsUnavailable() {
        assertNull(displayResolution(null, 1080, 0))
        assertNull(displayResolution(1920, null, 0))
        assertNull(displayResolution(0, 1080, 0))
        assertNull(displayResolution(1920, -1, 0))
    }
}
