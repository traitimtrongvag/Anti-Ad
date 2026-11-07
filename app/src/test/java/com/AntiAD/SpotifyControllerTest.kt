package com.AntiAD

import android.media.session.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Test

class SpotifyControllerTest {
    @Test
    fun bufferingIsNotReportedAsConfirmedPlayback() {
        assertEquals(
            SpotifyController.State.UNKNOWN,
            SpotifyController.classifyPlaybackState(PlaybackState.STATE_BUFFERING, "Track title")
        )
    }

    @Test
    fun playingStateUsesTitleToDistinguishAds() {
        assertEquals(
            SpotifyController.State.PLAYING_MUSIC,
            SpotifyController.classifyPlaybackState(PlaybackState.STATE_PLAYING, "Track title")
        )
        assertEquals(
            SpotifyController.State.PLAYING_AD,
            SpotifyController.classifyPlaybackState(PlaybackState.STATE_PLAYING, "Advertisement")
        )
    }

    @Test
    fun playbackOnlyKeywordDoesNotClassifyTrackAsAd() {
        assertEquals(
            SpotifyController.State.PLAYING_MUSIC,
            SpotifyController.classifyPlaybackState(PlaybackState.STATE_PLAYING, "باز یابی")
        )
    }
}
