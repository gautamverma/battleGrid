package com.antigravity.battleship.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class SoundPack { CLASSIC, SCI_FI }

class SoundManager {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    var currentPack by mutableStateOf(SoundPack.SCI_FI)
    var isMuted by mutableStateOf(false)

    fun playLaunch() {
        if (isMuted) return
        when (currentPack) {
            SoundPack.CLASSIC -> toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            SoundPack.SCI_FI -> toneGenerator.startTone(ToneGenerator.TONE_SUP_DIAL, 150)
        }
    }

    fun playHit() {
        if (isMuted) return
        when (currentPack) {
            SoundPack.CLASSIC -> toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 200)
            SoundPack.SCI_FI -> toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 300)
        }
    }

    fun playMiss() {
        if (isMuted) return
        when (currentPack) {
            SoundPack.CLASSIC -> toneGenerator.startTone(ToneGenerator.TONE_PROP_PROMPT, 100)
            SoundPack.SCI_FI -> toneGenerator.startTone(ToneGenerator.TONE_CDMA_LOW_L, 150)
        }
    }
    
    fun playDestroyed() {
        if (isMuted) return
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 500)
    }
}
