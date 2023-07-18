package ru.tinkoff.acquiring.sdk.utils

import android.app.Activity
import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.annotation.IdRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal object ViewUtil {

    private const val ERROR_SHAKE_AMPLITUDE = 3f // dp
    private const val ERROR_SHAKE_DURATION = 500L
    private const val ERROR_SHAKE_CYCLES = 3

    // Methods using these buffer objects are meant to be used in single (UI) thread
    private val tMatrix = Matrix()
    private val tRectF = RectF()

    fun getDescendantRect(parent: ViewGroup, descendant: View, rect: Rect) {
        rect.set(0, 0, descendant.width, descendant.height)
        offsetDescendantRect(parent, descendant, rect)
    }

    fun offsetDescendantRect(parent: ViewGroup, descendant: View, rect: Rect) {
        tMatrix.reset()
        offsetDescendantMatrix(parent, descendant, tMatrix)
        tRectF.set(rect)
        tMatrix.mapRect(tRectF)
        rect.set((tRectF.left + 0.5f).toInt(), (tRectF.top + 0.5f).toInt(), (tRectF.right + 0.5f).toInt(), (tRectF.bottom + 0.5f).toInt())
    }

    private fun offsetDescendantMatrix(target: ViewParent, view: View, matrix: Matrix) {
        val parent = view.parent
        if (parent is View && parent !== target) {
            offsetDescendantMatrix(target, parent, matrix)
            matrix.preTranslate((-parent.scrollX).toFloat(), (-parent.scrollY).toFloat())
        }

        matrix.preTranslate(view.left.toFloat(), view.top.toFloat())
        if (!view.matrix.isIdentity) {
            matrix.preConcat(view.matrix)
        }
    }
}

internal fun Context.dpToPx(dp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

internal fun Context.dpToPx(dp: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()

internal fun Context.spToPx(sp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)

internal fun Context.spToPx(sp: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), resources.displayMetrics).toInt()

internal fun View.horizontalPadding(): Int {
    return paddingLeft + paddingRight
}

internal fun View.setHorizontalPadding(padding: Int) {
    setPadding(padding, paddingTop, padding, paddingBottom)
}

internal fun View.verticalPadding(): Int {
    return paddingTop + paddingBottom
}

internal fun View.setVerticalPadding(padding: Int) {
    setPadding(paddingLeft, padding, paddingRight, padding)
}

internal fun ViewGroup.LayoutParams.horizontalMargin(): Int {
    return (this as ViewGroup.MarginLayoutParams).leftMargin + rightMargin
}

internal fun ViewGroup.LayoutParams.verticalMargin(): Int {
    return (this as ViewGroup.MarginLayoutParams).topMargin + bottomMargin
}

internal fun ViewGroup.LayoutParams.setHorizontalMargin(margin: Int) =
    with(this as ViewGroup.MarginLayoutParams) {
        leftMargin = margin
        rightMargin = margin
    }

internal fun ViewGroup.LayoutParams.setVerticalMargin(margin: Int) =
    with(this as ViewGroup.MarginLayoutParams) {
        topMargin = margin
        bottomMargin = margin
    }

internal fun View.measuredFullWidth(): Int {
    return measuredWidth + layoutParams.horizontalMargin()
}

internal fun View.measuredFullHeight(): Int {
    return measuredHeight + layoutParams.verticalMargin()
}

internal fun ViewGroup.forEachChild(action: (child: View) -> Unit) {
    for (i in 0 until childCount) {
        action(getChildAt(i))
    }
}

internal fun lerp(start: Int, end: Int, fraction: Float): Int {
    return (start + (end - start) * fraction).roundToInt()
}

internal fun lerp(start: Long, end: Long, fraction: Float): Long {
    return (start + (end - start) * fraction).roundToLong()
}

internal fun lerp(start: Float, end: Float, fraction: Float): Float {
    return (start + (end - start) * fraction)
}

internal fun <T> lazyUnsafe(initializer: () -> T): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE, initializer)

internal fun <T> Fragment.lazyView(@IdRes id: Int): Lazy<T> =
    lazyUnsafe { requireView().findViewById(id) }

internal fun <T> Activity.lazyView(@IdRes id: Int): Lazy<T> =
    lazyUnsafe { findViewById(id) }