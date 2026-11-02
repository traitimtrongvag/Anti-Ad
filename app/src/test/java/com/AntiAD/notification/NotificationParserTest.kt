package com.AntiAD.notification

import com.AntiAD.findAdMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationParserTest {
    @Test
    fun matchesLocalizedKeywordInAnyNotificationField() {
        assertEquals("Werbung", findAdMatch("Werbung", "", ""))
        assertEquals("Sponsored: إعلان", findAdMatch("", "", "Sponsored: إعلان"))
        assertEquals("Audio ad: Quảng cáo", findAdMatch("", "Audio ad: Quảng cáo", ""))
    }

    @Test
    fun prioritizesExactTitleAndThenAdvertisementSubtext() {
        assertEquals(
            "Advertisement",
            findAdMatch("Advertisement", "Sponsored content", "Advertisement")
        )
        assertEquals(
            "Advertisement • 30 sec",
            findAdMatch("Track title", "Track details", "Advertisement • 30 sec")
        )
    }

    @Test
    fun doesNotMatchRegularMusicOrLiteralNull() {
        assertNull(findAdMatch("Song title", "Artist name", "null"))
        assertNull(findAdMatch("null", "null", "null"))
    }
}
