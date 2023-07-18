package ru.tinkoff.acquiring.sdk.redesign.common.util

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.CycleInterpolator

object AcqShimmerAnimator {

    const val FADE_MIN = 0.4f
    const val FADE_MAX = 1f
    const val FADE_TOTAL_DURATION = 1000L
    const val POSITIONED_DELAY = 80L

    fun animateSequentially(views: Iterable<View>, positionedDelay: Long = POSITIONED_DELAY) {
        views.forEachIndexed { i, view ->
            animatePositioned(
                view = view,
                position = i,
                positionedDelay = positionedDelay
            )
        }
    }

    fun animateSequentially(vararg views: View) {
        views.forEachIndexed { i, view -> animatePositioned(view, i) }
    }

    fun animate(
        view: View,
        delay: Long = 0L,
        duration: Long = FADE_TOTAL_DURATION,
        fadeMin: Float = FADE_MIN,
        fadeMax: Float = FADE_MAX
    ) {
        AlphaAnimation((fadeMin + fadeMax) / 2, fadeMax).also {
            it.duration = duration
            it.interpolator = CycleInterpolator(1f)
            it.repeatMode = Animation.RESTART
            it.repeatCount = Animation.INFINITE
            if (delay == 0L) {
                view.startAnimation(it)
            } else {
                it.startTime = AnimationUtils.currentAnimationTimeMillis() + delay
                view.animation = it
            }
        }
    }

    fun animatePositioned(
        view: View,
        position: Int,
        duration: Long = FADE_TOTAL_DURATION,
        positionedDelay: Long = POSITIONED_DELAY,
        fadeMin: Float = FADE_MIN,
        fadeMax: Float = FADE_MAX
    ) {
        animate(view, position * positionedDelay, duration, fadeMin, fadeMax)
    }
}
