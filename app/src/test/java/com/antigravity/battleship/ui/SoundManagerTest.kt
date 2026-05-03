package com.antigravity.battleship.ui

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SoundManagerTest {

    private lateinit var soundManager: SoundManager

    @Before
    fun setup() {
        soundManager = SoundManager()
    }

    @Test
    fun `default pack is SCI_FI`() {
        assertEquals(SoundPack.SCI_FI, soundManager.currentPack)
    }

    @Test
    fun `default is not muted`() {
        assertFalse(soundManager.isMuted)
    }

    @Test
    fun `can change sound pack`() {
        soundManager.currentPack = SoundPack.CLASSIC
        assertEquals(SoundPack.CLASSIC, soundManager.currentPack)
    }

    @Test
    fun `can mute and unmute`() {
        soundManager.isMuted = true
        assertTrue(soundManager.isMuted)
        soundManager.isMuted = false
        assertFalse(soundManager.isMuted)
    }

    @Test
    fun `playLaunch does not crash when muted`() {
        soundManager.isMuted = true
        soundManager.playLaunch() // Should return early without exception
    }

    @Test
    fun `playHit does not crash when muted`() {
        soundManager.isMuted = true
        soundManager.playHit()
    }

    @Test
    fun `playMiss does not crash when muted`() {
        soundManager.isMuted = true
        soundManager.playMiss()
    }

    @Test
    fun `playDestroyed does not crash when muted`() {
        soundManager.isMuted = true
        soundManager.playDestroyed()
    }
}
