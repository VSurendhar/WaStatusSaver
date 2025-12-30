package com.voidDeveloper.wastatussaver.presentation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

class NoisyAudioReceiver(
    private val onNoisy: () -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
            onNoisy()
        }
    }
}
