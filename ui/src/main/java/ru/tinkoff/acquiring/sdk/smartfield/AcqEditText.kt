package ru.tinkoff.acquiring.sdk.smartfield

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.text.InputFilter
import android.text.Spanned
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updatePadding
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.utils.HapticUtil
import kotlin.math.roundToInt

/**
 * @author Ilnar Khafizov
 */
internal class AcqEditText
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    var errorHighlighted = false
        set(value) {
            field = value
            refreshDrawableState()
        }
    var pseudoFocused = false
        set(value) {
            field = value
            refreshDrawableState()
        }

    var keyboardBackPressedListener: (() -> Unit)? = null

    var appendix: String? = null
        set(value) {
            if (field == value) return
            field = value
            invalidateAppendix()
        }

    var appendixSpace: Float = 0f
        set(value) {
            field = value
            invalidateAppendix()
        }

    var appendixColorRes: Int = -1
        set(value) {
            field = value
            appendixColor = value.takeIf { it != -1 }?.let {
                ResourcesCompat.getColorStateList(context.resources, it, context.theme)
            }
        }
    private var appendixColor: ColorStateList? = null
        set(value) {
            field = value
            invalidateAppendix()
        }

    var appendixSide: Int = ZAppendixSide.RIGHT
        set(value) {
            field = value
            invalidateAppendix()
        }

    var maxSymbols = -1
        set(value) {
            field = value
            if (field > 0 && text?.length ?: 0 > field) {
                text = text
            }
        }

    private val fontMetrics = Paint.FontMetrics()

    private val lengthFilter = object : InputFilter {
        override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
            if (maxSymbols <= 0) return null
            var keep = maxSymbols - (dest.length - (dend - dstart))
            return when {
                keep <= 0 -> ""
                keep >= end - start -> null
                else -> {
                    keep += start
                    if (Character.isHighSurrogate(source[keep - 1])) {
                        --keep
                        if (keep == start) return ""
                    }
                    HapticUtil.performWarningHaptic(context)
                    source.subSequence(start, keep)
                }
            }
        }
    }

    var focusAllower: FocusAllower? = null

    init {
        appendixSpace = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            APPENDIX_SPACE_DEFAULT, context.resources.displayMetrics)
        appendixColorRes = appendixColorRes

        filters += lengthFilter
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val states = arrayListOf<Int>()
        if (errorHighlighted) states.add(ERROR_STATE)
        if (pseudoFocused) states.add(PSEUDO_FOCUS_STATE)
        val state = super.onCreateDrawableState(extraSpace + states.size)
        mergeDrawableStates(state, states.toIntArray())
        return state
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawAppendix(canvas)
    }

    private fun invalidateAppendix() {
        if (appendix.isNullOrEmpty()) {
            updatePadding(left = 0, right = 0)
        } else {
            paint.getFontMetrics(fontMetrics)
            val offset = (paint.measureText(appendix) + appendixSpace).roundToInt()
            when (appendixSide) {
                ZAppendixSide.LEFT -> updatePadding(left = offset, right = 0)
                ZAppendixSide.RIGHT -> updatePadding(left = 0, right = offset)
            }
        }
        invalidate()
    }

    private fun drawAppendix(canvas: Canvas) {
        val appendix = appendix
        // nothing to draw
        if (appendix.isNullOrEmpty()) return
        // self hint is drawn
        if (text.isNullOrEmpty() && !hint.isNullOrEmpty()) return

        paint.getFontMetrics(fontMetrics)

        val prevColor = paint.color
        paint.color = appendixColor?.getColorForState(drawableState, prevColor) ?: prevColor

        val appendixOffset = when (appendixSide) {
            ZAppendixSide.RIGHT -> paddingLeft + paint.measureText(text.toString()) + appendixSpace
            else -> 0f
        }
        canvas.drawText(appendix, appendixOffset, paddingTop - fontMetrics.top, paint)

        paint.color = prevColor
    }

    fun getCursorTop(): Int? {
        val layout = layout ?: return null
        val line = layout.getLineForOffset(selectionEnd)
        var lineTop = layout.getLineTop(line)

        lineTop += extendedPaddingTop - scrollY

        return lineTop
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP) {
            keyboardBackPressedListener?.invoke()
        }
        return super.onKeyPreIme(keyCode, event)
    }

    private val showKeyboardRunnable = Runnable { if (showSoftInputOnFocus && isFocused) showKeyboard() }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean = when {
        textInputLayout()?.textEditable == false -> isFocused
        focusAllower?.allowsViewTakeFocus(this) == false -> isFocused
        else -> super.requestFocus(direction, previouslyFocusedRect)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)

        removeCallbacks(showKeyboardRunnable)
        if (isFocused) {
            showKeyboardRunnable.run()
            postDelayed(showKeyboardRunnable, SHOW_KEYBOARD_DELAY)
        }
    }

    fun showKeyboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.windowInsetsController?.show(WindowInsets.Type.ime())
        } else {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(this, 0)
        }
    }

    fun hideKeyboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.windowInsetsController?.hide(WindowInsets.Type.ime())
        } else {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    fun interface FocusAllower {
        fun allowsViewTakeFocus(view: AcqEditText): Boolean
    }

    companion object {

        private const val SHOW_KEYBOARD_DELAY = 200L

        private const val APPENDIX_SPACE_DEFAULT = 6f //dp

        private val ERROR_STATE = R.attr.acq_sf_state_error
        private val PSEUDO_FOCUS_STATE = R.attr.acq_sf_state_pseudo_focus

        fun AcqEditText.textInputLayout(): AcqTextInputLayout? = parent as? AcqTextInputLayout
    }
}

object ZAppendixSide {
    const val LEFT = 0
    const val RIGHT = 1
}
