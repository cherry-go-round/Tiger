package com.ssafy.s15p21a206.tiger.capture.arcore

import org.junit.Assert.assertEquals
import org.junit.Test

class PoseRowTest {
    @Test
    fun `pose row follows the header column order`() {
        assertEquals(
            "123,0.1,-0.2,0.3,0.0,0.0,0.7071,0.7071,TRACKING,NONE",
            poseRow(
                123L,
                translation = floatArrayOf(0.1f, -0.2f, 0.3f),
                rotation = floatArrayOf(0f, 0f, 0.7071f, 0.7071f),
                trackingState = "TRACKING",
                failureReason = "NONE",
            ),
        )
    }

    @Test
    fun `a lost frame carries its state and reason`() {
        assertEquals(
            "456,1.0,2.0,3.0,0.0,0.0,0.0,1.0,PAUSED,INSUFFICIENT_LIGHT",
            poseRow(
                456L,
                translation = floatArrayOf(1f, 2f, 3f),
                rotation = floatArrayOf(0f, 0f, 0f, 1f),
                trackingState = "PAUSED",
                failureReason = "INSUFFICIENT_LIGHT",
            ),
        )
    }
}
