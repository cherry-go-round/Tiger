package com.ssafy.s15p21a206.tiger.core.capture.writer

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorRowTest {
    @Test
    fun `rotation vector row carries five values between timestamp and accuracy`() {
        assertEquals(
            "123,0.1,0.2,0.3,0.4,0.5,3",
            sensorRow(123L, floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f), valueCount = 5, accuracy = 3),
        )
    }

    @Test
    fun `motion row carries three values and drops the rest`() {
        assertEquals(
            "456,-9.81,0.0,1.5,2",
            sensorRow(456L, floatArrayOf(-9.81f, 0f, 1.5f, 7f), valueCount = 3, accuracy = 2),
        )
    }

    @Test
    fun `missing values are filled with zero so the columns match the header`() {
        assertEquals(
            "789,0.1,0.2,0.3,0.4,0.0,1",
            sensorRow(789L, floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f), valueCount = 5, accuracy = 1),
        )
    }
}
