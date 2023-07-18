package ru.tinkoff.acquiring.sdk.smartfield

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.method.TransformationMethod
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.utils.SimpleTextWatcher.Companion.afterTextChanged
import ru.tinkoff.acquiring.sdk.utils.forEachChild
import ru.tinkoff.acquiring.sdk.utils.lerp
import ru.tinkoff.acquiring.sdk.utils.setHorizontalPadding
import ru.tinkoff.acquiring.sdk.utils.setVerticalPadding

/**
 * @author Ilnar Khafizov
 */
internal open class AcqTextFieldView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(getContext()).inflate(R.layout.acq_layout_text_field, this)
    }

    val textInputLayout: AcqTextInputLayout = findViewById(R.id.text_input_layout)
    val editText: AcqEditText = findViewById(R.id.edit_text)
    private val symbolCounter = SymbolCounter(findViewById(R.id.symbol_counter))

    var editable = true
        set(value) {
            field = value
            isEnabled = field
            recursiveSetEnabled(field)
        }

    var textEditable by textInputLayout::textEditable
    var title: CharSequence? by textInputLayout::title
    var floatingTitle: Boolean by textInputLayout::floatingTitle
    var placeholder: CharSequence? by textInputLayout::placeholder
    var appendix: String? by textInputLayout::appendix
    var appendixColorRes: Int by textInputLayout::appendixColorRes
    var appendixSide: Int by textInputLayout::appendixSide
    var textSize: Float by textInputLayout::textSize
    var fontFamily: Int by textInputLayout::fontFamily
    var textStyle: Int by textInputLayout::textStyle
    var inputType: Int by textInputLayout::inputType
    var transformationMethod: TransformationMethod? by textInputLayout::transformationMethod
    var maxLines: Int by textInputLayout::maxLines
    var maxSymbols: Int
        get() = textInputLayout.maxSymbols
        set(value) {
            textInputLayout.maxSymbols = value
            symbolCounter.update()
        }

    var maxSymbolsCounterVisible = true
        set(value) {
            if (field == value) return
            field = value
            symbolCounter.update()
        }

    var text: String? by textInputLayout::text

    var errorHighlighted: Boolean by textInputLayout::errorHighlighted
    var pseudoFocused: Boolean by textInputLayout::pseudoFocused
    var focusAllower: AcqEditText.FocusAllower? by editText::focusAllower
    var nextPressedListener: (() -> Unit)? by textInputLayout::nextPressedListener
    var textChangedCallback: ((Editable?) -> Unit)? by textInputLayout::textChangedCallback
    var focusChangeCallback: ((AcqEditText) -> Unit)? by textInputLayout::focusChangeCallback
    var keyboardBackPressedListener: (() -> Unit)? by textInputLayout::keyboardBackPressedListener

    init {
        orientation = VERTICAL
        setVerticalPadding(context.resources.getDimensionPixelSize(R.dimen.acq_sf_vertical_padding))

        editText.isSaveFromParentEnabled = false

        textInputLayout.addFocusChangeListener { symbolCounter.update() }
        editText.afterTextChanged { symbolCounter.update() }
        setViewClickListener { editText.requestFocus() }

        initAttrs(attrs)
    }

    protected open fun initAttrs(attrs: AttributeSet?) {
        if (attrs == null) return

        val a = context.obtainStyledAttributes(attrs, R.styleable.AcqTextFieldView)
        editable = a.getBoolean(R.styleable.AcqTextFieldView_acq_editable, editable)
        textEditable = a.getBoolean(R.styleable.AcqTextFieldView_acq_textEditable, textEditable)
        title = a.getString(R.styleable.AcqTextFieldView_acq_title) ?: title
        floatingTitle = a.getBoolean(R.styleable.AcqTextFieldView_acq_floatingTitle, floatingTitle)
        placeholder = a.getString(R.styleable.AcqTextFieldView_acq_placeholder) ?: placeholder
        appendix = a.getString(R.styleable.AcqTextFieldView_acq_appendix) ?: appendix
        appendixColorRes = a.getResourceId(R.styleable.AcqTextFieldView_acq_appendixColorRes, appendixColorRes)
        appendixSide = a.getInt(R.styleable.AcqTextFieldView_acq_appendixSide, appendixSide)
        textSize = a.getDimension(R.styleable.AcqTextFieldView_acq_textSize, -1f)
            .takeIf { it != -1f }?.let { it / context.resources.displayMetrics.scaledDensity } ?: textSize
        fontFamily = a.getResourceId(R.styleable.AcqTextFieldView_acq_fontFamily, fontFamily)
        textStyle = a.getInt(R.styleable.AcqTextFieldView_acq_textStyle, textStyle)
        // android:inputType is superseded by acq_inputType if it's set
        inputType = a.getInt(R.styleable.AcqTextFieldView_android_inputType, inputType)
        inputType = a.getInt(R.styleable.AcqTextFieldView_acq_inputType, inputType)
        maxLines = a.getInt(R.styleable.AcqTextFieldView_acq_maxLines, maxLines)
        transformationMethod = editText.transformationMethod
        maxSymbols = a.getInt(R.styleable.AcqTextFieldView_acq_maxSymbols, maxSymbols)
        maxSymbolsCounterVisible = a.getBoolean(R.styleable.AcqTextFieldView_acq_maxSymbolsCounterVisible, maxSymbolsCounterVisible)
        text = a.getString(R.styleable.AcqTextFieldView_acq_text) ?: text
        a.recycle()
    }

    fun setText(text: CharSequence?, resetCursor: Boolean = false) = textInputLayout.setText(text, resetCursor)

    fun requestViewFocus(): Boolean = textInputLayout.requestViewFocus()

    fun clearViewFocus() = textInputLayout.clearViewFocus()

    fun isViewFocused(): Boolean = textInputLayout.isViewFocused()

    fun setViewClickListener(listener: ((View) -> Unit)?) =
        when (listener) {
            null -> {
                setOnClickListener(null)
                isClickable = false
                textInputLayout.setViewClickListener(listener)
            }
            else -> {
                setOnClickListener(listener)
                textInputLayout.setViewClickListener(listener)
            }
        }

    fun addViewFocusChangeListener(listener: ((AcqEditText) -> Unit)) =
        textInputLayout.addFocusChangeListener(listener)

    fun removeViewFocusChangeListener(listener: ((AcqEditText) -> Unit)) =
        textInputLayout.removeFocusChangeListener(listener)

    fun addLeftBauble(bauble: View, index: Int = 0) = textInputLayout.addLeftBauble(bauble, index)

    fun addRightBauble(bauble: View, index: Int = 0) = textInputLayout.addRightBauble(bauble, index)

    fun setLines(lines: Int) = editText.setLines(lines)

    fun setSelection(start: Int, end: Int = start) = textInputLayout.setSelection(start, end)

    fun showKeyboard() = editText.showKeyboard()

    fun hideKeyboard() = editText.hideKeyboard()

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()!!
        val ss = SavedState(superState)
        ss.childrenStates = SparseArray()
        for (i in 0 until childCount) {
            getChildAt(i).saveHierarchyState(ss.childrenStates)
        }
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        for (i in 0 until childCount) {
            getChildAt(i).restoreHierarchyState(savedState.childrenStates)
        }
    }

    private inner class SymbolCounter(val view: TextView) {

        init {
            view.tag = (tag as? String)?.plus(SYMBOL_COUNTER_TAG_POSTFIX)
        }

        private var visible = false
            set(value) {
                if (field == value) return
                field = value
                anim?.cancel()
                when {
                    this@AcqTextFieldView.isLaidOut -> animateVisible(field)
                    else -> with(view) {
                        layoutParams.height = if (visible) ViewGroup.LayoutParams.WRAP_CONTENT else 0
                        requestLayout()
                    }
                }
            }

        private var anim: Animator? = null

        fun update() {
            if (maxSymbols <= 0 || !maxSymbolsCounterVisible || !editText.isFocused) {
                visible = false
            } else {
                view.text = when (val length = editText.length()) {
                    0 -> context.resources.getQuantityString(
                        R.plurals.acq_sf_max_symbol_counter_empty,
                        maxSymbols, maxSymbols)
                    else -> context.resources.getQuantityString(
                        R.plurals.acq_sf_max_symbol_counter_remaining,
                        maxSymbols - length, maxSymbols - length)
                }
                visible = true
            }
        }

        private fun animateVisible(visible: Boolean) {
            val params = view.layoutParams
            val startHeight = view.height
            val targetHeight = if (visible) {
                view.measure(makeMeasureSpec(width - paddingLeft - paddingRight, EXACTLY), UNSPECIFIED)
                view.measuredHeight
            } else 0
            val startAlpha = view.alpha
            val targetAlpha = if (visible) 1f else 0f
            anim = ValueAnimator.ofFloat(0f, 1f).apply {
                addUpdateListener {
                    val fraction = it.animatedValue as Float
                    params.height = lerp(startHeight, targetHeight, fraction)
                    view.alpha = lerp(startAlpha, targetAlpha, fraction)
                    view.postOnAnimation { view.requestLayout() }
                }
                duration = SYMBOL_COUNTER_ANIM_DURATION
                start()
            }
        }
    }

    class SavedState : BaseSavedState {

        var childrenStates: SparseArray<Parcelable>? = null

        constructor(superState: Parcelable) : super(superState)

        @Suppress("UNCHECKED_CAST")
        private constructor(source: Parcel, classLoader: ClassLoader?) : super(source) {
            childrenStates = source.readSparseArray<Parcelable>(classLoader)
        }

        @Suppress("UNCHECKED_CAST")
        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeSparseArray(childrenStates as SparseArray<Any>)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.ClassLoaderCreator<SavedState> =
                object : Parcelable.ClassLoaderCreator<SavedState> {
                    override fun createFromParcel(
                        source: Parcel,
                        loader: ClassLoader?
                    ): SavedState {
                        return SavedState(source, loader)
                    }

                    override fun createFromParcel(source: Parcel): SavedState {
                        return createFromParcel(source, null)
                    }

                    override fun newArray(size: Int): Array<SavedState?> {
                        return arrayOfNulls(size)
                    }
                }
        }
    }

    protected companion object {

        const val SYMBOL_COUNTER_TAG_POSTFIX = "_symbol_counter"
        private const val SYMBOL_COUNTER_ANIM_DURATION = 200L

        fun ViewGroup.recursiveSetEnabled(enabled: Boolean): Unit = forEachChild {
            it.isEnabled = enabled
            (it as? ViewGroup)?.recursiveSetEnabled(enabled)
        }
    }
}