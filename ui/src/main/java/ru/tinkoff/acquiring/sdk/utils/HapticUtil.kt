package ru.tinkoff.acquiring.sdk.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings

@Suppress("MagicNumber")
internal object HapticUtil {

    fun performErrorHaptic(context: Context) {
        if (!isSystemHapticEnabled(context)) return

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 7, 120, 4), -1))
        } else {
            vibrator.vibrate(longArrayOf(0, 7, 120, 4), -1)
        }
    }

    fun performWarningHaptic(context: Context) {
        if (!isSystemHapticEnabled(context)) return

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(7, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(7)
        }
    }

    fun isSystemHapticEnabled(context: Context): Boolean {
        return Settings.System.getInt(context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0
    }
}