package ru.tinkoff.acquiring.sdk.smartfield

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.InsetDrawable
import android.text.Editable
import android.text.TextPaint
import android.text.method.TransformationMethod
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.updatePadding
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.utils.SimpleTextWatcher.Companion.afterTextChanged
import ru.tinkoff.acquiring.sdk.utils.ViewUtil
import ru.tinkoff.acquiring.sdk.utils.dpToPx
import ru.tinkoff.acquiring.sdk.utils.forEachChild
import ru.tinkoff.acquiring.sdk.utils.horizontalMargin
import ru.tinkoff.acquiring.sdk.utils.horizontalPadding
import ru.tinkoff.acquiring.sdk.utils.lerp
import ru.tinkoff.acquiring.sdk.utils.measuredFullHeight
import ru.tinkoff.acquiring.sdk.utils.measuredFullWidth
import ru.tinkoff.acquiring.sdk.utils.spToPx
import ru.tinkoff.acquiring.sdk.utils.verticalMargin
import ru.tinkoff.acquiring.sdk.utils.verticalPadding
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong

internal open class AcqTextInputLayout
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    lateinit var editText: AcqEditText
        private set

    // state
    private var expandedTitleTextSize: Float = context.spToPx(16f)
        set(value) {
            if (field == value) return
            field = value
            clearTitlePaintShaders()
            invalidate()
        }
    private var collapsedTitleTextSize: Int = context.spToPx(DEFAULT_COLLAPSED_TITLE_TEXT_SIZE)
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
        }
    private var collapsedTitleBottomMargin: Int = context.dpToPx(DEFAULT_TITLE_BOTTOM_MARGIN)
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
        }
    private var defaultTitleTextColor: ColorStateList? = null

    var textEditable = true
        set(value) {
            field = value
            editText.isFocusableInTouchMode = field
        }

    var title: CharSequence? = null
        set(value) {
            if (field == value) return
            field = value
            clearTitlePaintShaders()
            invalidate()
        }

    var floatingTitle = true
        set(value) {
            if (field == value) return
            field = value
            updateTitleFraction(false)
            requestLayout()
        }

    var placeholder: CharSequence?
        get() = editText.hint
        set(value) {
            editText.hint = value
        }

    var appendix: String?
        get() = editText.appendix
        set(value) {
            editText.appendix = value
        }

    var appendixColorRes: Int
        get() = editText.appendixColorRes
        set(value) {
            editText.appendixColorRes = value
        }

    var appendixSide: Int
        get() = editText.appendixSide
        set(value) {
            editText.appendixSide = value
        }

    var textSize: Float
        get() = editText.textSize / editText.paint.density
        set(value) {
            editText.textSize = value
        }

    var fontFamily: Int = -1
        set(value) {
            field = value
            if (value == -1) return
            editText.setTypeface(ResourcesCompat.getFont(context, value), textStyle)
        }

    var textStyle: Int = Typeface.NORMAL
        set(value) {
            field = value
            if (fontFamily == -1) {
                editText.setTypeface(editText.typeface, value)
            } else {
                editText.setTypeface(ResourcesCompat.getFont(context, fontFamily), value)
            }
        }

    var inputType: Int
        get() = editText.inputType
        set(value) {
            editText.inputType = value
        }

    var transformationMethod: TransformationMethod?
        get() = editText.transformationMethod
        set(value) {
            editText.transformationMethod = value
        }

    var maxLines: Int
        get() = editText.maxLines
        set(value) {
            if (value < 0) {
                if (editText.maxLines >= 0) {
                    editText.maxHeight = Int.MAX_VALUE
                }
            } else {
                editText.maxLines = value
            }
        }

    var maxSymbols: Int
        get() = editText.maxSymbols
        set(value) {
            if (editText.maxSymbols <= 0 && value <= 0) return
            editText.maxSymbols = value
        }

    var text: String?
        get() = editText.text?.toString()
        set(value) {
            setText(value)
        }

    private var _errorHighlighted = false
        set(value) {
            field = value
            refreshDrawableState()
        }
    var errorHighlighted: Boolean
        get() = _errorHighlighted
        set(value) {
            if (floatingTitle) {
                _errorHighlighted = value
                editText.errorHighlighted = false
            } else {
                // ниже не совсем корректно, потому что
                // 1) при наличии фокуса у поля поле _errorHighlighted должно быть false
                // 2) при отсутствии фокуса и текста у поля поле editText.errorHighlighted должно быть false
                // но, в момент изменении поля errorHighlighted мы не можем точно узнать наличие фокуса,
                // именно поэтому оставляем такой код, который дает нам точную информацию, что ошибка отображена,
                // но не дает нам точной информации кто именно ее отображает
                _errorHighlighted = value
                editText.errorHighlighted = value
            }
        }

    var pseudoFocused: Boolean
        get() = editText.pseudoFocused
        set(value) {
            editText.pseudoFocused = value
        }

    var titleTextColor: ColorStateList? = null
        set(value) {
            field = value
            defaultTitleTextColor = value
            refreshDrawableState()
        }

    @VisibleForTesting
    var currentTitleTextColor: Int = DEFAULT_TITLE_TEXT_COLOR
        private set

    /**
     * Current animation stage of title.
     *
     * If [floatingTitle] is enabled this determines scale and position of title, if
     * [floatingTitle] is disabled - it's alpha value.
     *
     * - 1 - title fully expanded (drawn in place of editText's text) or fully opaque
     * - 0 - title fully collapsed (drawn above editText's text) or fully transparent
     *
     * It also affects alpha of editText both when [floatingTitle] is enabled or disabled.
     */
    private var titleFraction: Float = 1f
    private val titlePos = PointF()
    private var titleScale = 1f
    private var titleAnim: Animator? = null

    private val collapsedBounds = Rect()
    private val expandedBounds = Rect()

    private var titlePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var titleTexture: Bitmap? = null
    private var titleShader: Shader? = null
    private val tmpRect = Rect()
    private val tmpMatrix = Matrix()
    private var currentTitleTextColorShaderCache: Int? = null
    private var expandedBoundsWidthShaderCache: Int? = null

    var nextPressedListener: (() -> Unit)? = null
    var textChangedCallback: ((Editable?) -> Unit)? = null
    var focusChangeCallback: ((AcqEditText) -> Unit)? = null
    var keyboardBackPressedListener: (() -> Unit)?
        get() = editText.keyboardBackPressedListener
        set(value) {
            editText.keyboardBackPressedListener = value
        }

    private val focusChangeListeners = arrayListOf<((AcqEditText) -> Unit)>()

    init {
        @Suppress("LeakingThis")
        setWillNotDraw(false)

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AcqTextInputLayout, defStyleAttr, 0)

            title = typedArray.getString(R.styleable.AcqTextInputLayout_acq_til_title)
            floatingTitle = typedArray.getBoolean(R.styleable.AcqTextInputLayout_acq_til_title_enabled, true)
            collapsedTitleTextSize = typedArray.getDimensionPixelSize(
                R.styleable.AcqTextInputLayout_acq_til_title_text_size, collapsedTitleTextSize
            )
            defaultTitleTextColor = typedArray.getColorStateList(R.styleable.AcqTextInputLayout_acq_til_title_text_color)
                ?: ColorStateList.valueOf(DEFAULT_TITLE_TEXT_COLOR)
            titleTextColor = defaultTitleTextColor
            collapsedTitleBottomMargin = typedArray.getDimensionPixelSize(
                R.styleable.AcqTextInputLayout_acq_til_title_bottom_margin, collapsedTitleBottomMargin
            )

            typedArray.recycle()
        }

        setInsets(0, 0)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val states = arrayListOf<Int>()
        if (_errorHighlighted) {
            states.add(R.attr.acq_sf_state_error)
        }
        val state = super.onCreateDrawableState(extraSpace + states.size)
        mergeDrawableStates(state, states.toIntArray())
        val focusedIndex = state.indexOf(android.R.attr.state_focused)
        if (focusedIndex != -1) {
            state[focusedIndex] = 0
        }
        return state
    }

//    override fun childDrawableStateChanged(child: View) {
//        if (child == editText) {
//            refreshDrawableState()
//        } else {
//            super.childDrawableStateChanged(child)
//        }
//    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        currentTitleTextColor = titleTextColor?.getColorForState(
            drawableState, DEFAULT_TITLE_TEXT_COLOR
        ) ?: DEFAULT_TITLE_TEXT_COLOR
        invalidate()
    }

    fun setInsets(left: Int = 0, right: Int = 0) {
        val padding = context.dpToPx(HORIZONTAL_PADDING)
        val drawable = (background as? InsetDrawable)?.drawable ?: background
        background = InsetDrawable(drawable, left, right, 0, 0)
        updatePadding(left = left + padding, right = right + padding)
    }

    @Suppress("ComplexCondition")
    private fun updateTitleFraction(animate: Boolean = true) {
        titleAnim?.cancel()

        val targetFraction = if (editText.text.isNullOrEmpty() && !editText.hasFocus()) 1f else 0f

        // animating if necessary
        if (animate && ViewCompat.isLaidOut(this) && titleFraction != targetFraction) {
            animateTitleFraction(targetFraction)
        } else {
            setTitleFraction(targetFraction)
            editText.invalidate()
            invalidate()
        }
    }

    private fun setTitleFraction(fraction: Float) {
        titleFraction = fraction
        if (floatingTitle) {
            titlePaint.alpha = ALPHA_MAX
            editText.alpha = 1 - fraction
            titleScale = (titleFraction * expandedTitleTextSize +
                    (1 - titleFraction) * collapsedTitleTextSize) / expandedTitleTextSize
            titlePos.set(
                lerp(collapsedBounds.left.toFloat(), expandedBounds.left.toFloat(), titleFraction),
                lerp(collapsedBounds.top.toFloat(), expandedBounds.top.toFloat(), titleFraction)
            )
        } else {
            titlePaint.alpha = (fraction * ALPHA_MAX).toInt()
            editText.alpha = if (title.isNullOrEmpty()) 1f else 1 - fraction
            titleScale = 1f
            titlePos.set(expandedBounds.left.toFloat(), expandedBounds.top.toFloat())
        }
    }

    private fun animateTitleFraction(fraction: Float) {
        titleAnim = ValueAnimator.ofFloat(titleFraction, fraction).apply {
            addUpdateListener {
                setTitleFraction(it.animatedValue as Float)
                ViewCompat.postInvalidateOnAnimation(editText)
                ViewCompat.postInvalidateOnAnimation(this@AcqTextInputLayout)
            }
            interpolator = TITLE_INTERPOLATOR
            duration = (abs(titleFraction - fraction) * ANIMATION_DURATION).roundToLong()
            titleAnim?.cancel()
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawTitle(canvas)
    }

    @VisibleForTesting
    fun drawTitle(canvas: Canvas) {
        if (!isDrawingTitle()) return
        ensureTitlePaintShader() ?: return

        val canvasSave = canvas.save()
        canvas.translate(titlePos.x, titlePos.y)
        titleShader!!.setLocalMatrix(tmpMatrix.apply {
            reset()
            preScale(titleScale, titleScale)
        })
        canvas.drawPaint(titlePaint)
        canvas.restoreToCount(canvasSave)
    }

    @VisibleForTesting
    fun isDrawingTitle(): Boolean = (floatingTitle || titleFraction != 0f) && !title.isNullOrBlank()

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams) {
        if (child is AcqEditText) {
            super.addView(child, index, params)
            editText = child
            onEditTextSet()
        } else {
            super.addView(child, index, params)
        }
    }

    private fun onEditTextSet() {
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                nextPressedListener?.invoke()
                return@setOnEditorActionListener true
            }
            false
        }

        editText.afterTextChanged {
            updateTitleFraction()
            textChangedCallback?.invoke(it)
        }

        editText.setOnFocusChangeListener { editText, _ ->
            editText as AcqEditText
            updateTitleFraction()
            if (!editText.isFocused && !textEditable) {
                // probably textEditable just changed, we don't want to trigger focus change
                // event for consumer but rather change focus variant to pseudoFocus
                pseudoFocused = true
            } else {
                focusChangeCallback?.invoke(this.editText)
            }
            if (editText.isFocused) {
                editText.setSelection(editText.text?.length ?: 0)
            }
            focusChangeListeners.forEach { it.invoke(editText) }
        }
        updateTitleFraction(false)
    }

    @Suppress("ComplexMethod")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val availableHeight = heightSize - verticalPadding()
        var occupiedHeight = verticalPadding()
        var occupiedWidth = horizontalPadding()

        fun measureChild(child: View) {
            if (child.isGone) return
            val params = child.params()

            check(child == editText || params.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                "Bauble views can't have MATCH_PARENT width"
            }

            child.measure(
                when (child) {
                    editText -> when (widthMode) {
                        MeasureSpec.UNSPECIFIED -> MeasureSpec.UNSPECIFIED
                        MeasureSpec.EXACTLY -> MeasureSpec.makeMeasureSpec(
                            widthSize - occupiedWidth - params.horizontalMargin(), MeasureSpec.EXACTLY
                        )
                        else -> MeasureSpec.makeMeasureSpec(
                            widthSize - occupiedWidth - params.horizontalMargin(), MeasureSpec.AT_MOST
                        )
                    }
                    else -> MeasureSpec.UNSPECIFIED
                },
                when (heightMode) {
                    MeasureSpec.UNSPECIFIED -> MeasureSpec.UNSPECIFIED
                    else -> MeasureSpec.makeMeasureSpec(
                        availableHeight -
                                params.verticalMargin(), MeasureSpec.AT_MOST
                    )
                }
            )

            occupiedWidth += child.measuredFullWidth()
            occupiedHeight = maxOf(occupiedHeight, child.measuredFullHeight() + verticalPadding())
        }

        forEachChild { child -> if (child !== editText) measureChild(child) }
        measureChild(editText)

        occupiedHeight = occupiedHeight.coerceAtLeast(minimumHeight)

        val measuredWidth = when (widthMode) {
            MeasureSpec.UNSPECIFIED -> occupiedWidth
            MeasureSpec.AT_MOST -> minOf(occupiedWidth, widthSize)
            else -> widthSize
        }
        val measuredHeight = when (heightMode) {
            MeasureSpec.UNSPECIFIED -> occupiedHeight
            MeasureSpec.AT_MOST -> minOf(occupiedHeight, heightSize)
            else -> heightSize
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val availableHeight = b - t
        val childBottom = availableHeight - paddingBottom
        var childLeft = paddingLeft

        forEachChild { child ->
            if (child.isGone) return@forEachChild
            val params = child.params()
            val childFullWidth = child.measuredFullWidth()
            val childFullHeight = child.measuredFullHeight()

            val verticalGravityOffset = if (childFullHeight < availableHeight)
                (availableHeight - childFullHeight) / 2 else 0

            child.layout(
                childLeft + params.leftMargin, paddingTop + verticalGravityOffset + params.topMargin,
                childLeft + childFullWidth, childBottom - verticalGravityOffset - params.bottomMargin
            )

            childLeft += childFullWidth
        }

        recalculateTitleBounds()
        setTitleFraction(titleFraction)
    }

    private fun recalculateTitleBounds() {
        val rect = tmpRect.also { ViewUtil.getDescendantRect(this, editText, it) }

        val left = rect.left
        val right = rect.right
        val top = rect.top + editText.compoundPaddingTop
        val bottom = rect.bottom - editText.compoundPaddingBottom
        val expandedTitleHeight = expandedTitleTextSize
        val offset = ((bottom - top - expandedTitleHeight) / 2).toInt()
        expandedBounds.set(left, top + offset, right, bottom - offset)

        if (floatingTitle) {
            val emptySpace = measuredHeight - editText.measuredHeight
            val collapsedTitleHeight = collapsedTitleTextSize + collapsedTitleBottomMargin
            val remainingSpace = emptySpace - collapsedTitleHeight
            val titleTopOffset = context.dpToPx(TITLE_TOP_OFFSET)
            val collapsedTitleTopMargin = (remainingSpace / 2 + titleTopOffset).coerceAtLeast(0)
            collapsedBounds.set(
                left, collapsedTitleTopMargin,
                right, collapsedTitleTopMargin + collapsedTitleTextSize
            )
            val textOffset = (collapsedTitleHeight + collapsedTitleTopMargin - emptySpace / 2)
                .coerceAtLeast(0)
            editText.layout(rect.left, rect.top + textOffset, rect.right, rect.bottom + textOffset)
        }
    }

    private fun ensureTitlePaintShader(): Shader? {
        val currentTitleTextColor = currentTitleTextColor
        val expandedBoundsWidth = expandedBounds.width().takeIf { it != 0 } ?: return null
        titlePaint.shader?.let {
            if (currentTitleTextColorShaderCache != currentTitleTextColor) return@let
            if (expandedBoundsWidthShaderCache != expandedBoundsWidth) return@let
            return it
        }
        currentTitleTextColorShaderCache = currentTitleTextColor
        expandedBoundsWidthShaderCache = expandedBoundsWidth

        clearTitlePaintShaders()

        titleShader = createTitleShader() ?: return null
        val fadeShader = createFadedEllipsizeShader(expandedBoundsWidth)
        return ComposeShader(titleShader!!, fadeShader, PorterDuff.Mode.DST_IN).also {
            titlePaint.shader = it
        }
    }

    private fun createTitleShader(): BitmapShader? {
        val title = title.takeIf { !it.isNullOrEmpty() }?.toString() ?: return null

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint.textSize = expandedTitleTextSize
        val fontMetrics = Paint.FontMetrics()
        textPaint.getFontMetrics(fontMetrics)

        textPaint.color = currentTitleTextColor
        val textWidth = ceil(textPaint.measureText(title)).toInt()
        val textHeight = ceil(fontMetrics.descent - fontMetrics.ascent).toInt()
        if (textWidth <= 0 || textHeight <= 0) return null

        titleTexture = Bitmap.createBitmap(textWidth + 1, textHeight + 1, Bitmap.Config.ARGB_8888)
        Canvas(titleTexture!!).drawText(title, 0.0f, textHeight - fontMetrics.descent, textPaint)
        return BitmapShader(titleTexture!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    private fun clearTitlePaintShaders() {
        titlePaint.shader = null
        titleShader = null
        titleTexture?.recycle()
        titleTexture = null
    }

    fun addFocusChangeListener(listener: ((AcqEditText) -> Unit)) {
        this.focusChangeListeners.add(listener)
    }

    fun removeFocusChangeListener(listener: ((AcqEditText) -> Unit)) {
        this.focusChangeListeners.remove(listener)
    }

    //region bauble
    private val leftBaubles = mutableListOf<View>()
    private val rightBaubles = mutableListOf<View>()

    fun addLeftBauble(bauble: View, index: Int) {
        leftBaubles.add(index, bauble)
        val viewIndex = when {
            index > 0 -> indexOfChild(leftBaubles[index - 1]) + 1
            else -> 0
        }
        addView(bauble, viewIndex)
    }

    fun addRightBauble(bauble: View, index: Int) {
        rightBaubles.add(index, bauble)
        val viewIndex = when {
            index > 0 -> indexOfChild(rightBaubles[index - 1]) + 1
            else -> indexOfChild(editText) + 1
        }
        addView(bauble, viewIndex)
    }

    override fun onViewRemoved(child: View?) {
        super.onViewRemoved(child)
        check(child != editText) { "Edit text can't be removed" }
        leftBaubles.remove(child)
        rightBaubles.remove(child)
    }
    //endregion

    fun setViewClickListener(listener: ((View) -> Unit)?) =
        when (listener) {
            null -> {
                editText.setOnClickListener(null)
                editText.isClickable = false
            }
            else -> editText.setOnClickListener(listener)
        }

    fun requestViewFocus(): Boolean = when {
        textEditable -> requestInputFocus()
        else -> {
            pseudoFocused = true
            true
        }
    }

    fun clearViewFocus() {
        clearInputFocus()
        pseudoFocused = false
    }

    fun requestInputFocus(): Boolean {
        if (!textEditable) return false

        editText.isFocusableInTouchMode = true
        editText.forceLayout() // view can be zero-sized and canTakeFocus() will return false
        return editText.requestFocus()
    }

    fun clearInputFocus() = editText.clearFocus()

    fun isViewFocused(): Boolean = editText.isFocused || pseudoFocused

    fun setText(text: CharSequence?, resetCursor: Boolean = false) {
        if (editText.text != text) {
            editText.setText(text)
        }
        if (resetCursor) {
            editText.setSelection(editText.text?.length ?: 0)
        }
    }

    fun setSelection(start: Int, end: Int = start) = editText.setSelection(start, end)

    fun setCollapsedTitleTextSizeRes(@DimenRes textSizeRes: Int) {
        collapsedTitleTextSize = context.resources.getDimensionPixelSize(textSizeRes)
    }

    fun setCollapsedTitleBottomMarginRes(@DimenRes bottomMarginRes: Int) {
        collapsedTitleBottomMargin = context.resources.getDimensionPixelSize(bottomMarginRes)
    }

    fun setTitleTextColor(@ColorInt titleTextColor: Int) {
        this.titleTextColor = ColorStateList.valueOf(titleTextColor)
    }

    fun setTitleTextColorRes(@ColorRes titleTextColorRes: Int) {
        titleTextColor = ResourcesCompat.getColorStateList(context.resources, titleTextColorRes, context.theme)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean = p is LayoutParams

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams =
        LayoutParams(context, attrs)

    public override fun generateLayoutParams(p: ViewGroup.LayoutParams?): LayoutParams = when (p) {
        null -> generateDefaultLayoutParams()
        is LayoutParams -> LayoutParams(p)
        is MarginLayoutParams -> LayoutParams(p)
        else -> LayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    @Suppress("UtilityClassWithPublicConstructor")
    class LayoutParams : MarginLayoutParams {

        @JvmOverloads
        constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: ViewGroup.LayoutParams) : super(source)

        constructor(source: MarginLayoutParams) : super(source)
    }

    companion object {

        private val TITLE_INTERPOLATOR = FastOutSlowInInterpolator()
        private const val ANIMATION_DURATION = 200L
        private const val TITLE_TOP_OFFSET = 1
        private const val ALPHA_MAX = 255
        private const val HORIZONTAL_PADDING = 12

        /**
         * Relative width at which title ending starts to fade out
         */
        const val FADED_ELLIPSIZE_RATIO = 0.8f

        private const val DEFAULT_COLLAPSED_TITLE_TEXT_SIZE = 13 // sp
        private const val DEFAULT_TITLE_BOTTOM_MARGIN = 4 // dp
        private const val DEFAULT_TITLE_TEXT_COLOR = Color.BLACK

        private fun createFadedEllipsizeShader(width: Int): Shader = LinearGradient(
            0f, 0f, width.toFloat(), 0f, intArrayOf(Color.BLACK, Color.TRANSPARENT),
            floatArrayOf(FADED_ELLIPSIZE_RATIO, 1f), Shader.TileMode.CLAMP
        )

        private val PSEUDO_FOCUS_STATE = R.attr.acq_sf_state_pseudo_focus
        private val FOCUS_STATE = android.R.attr.state_focused

        private fun View.params() = layoutParams as LayoutParams
    }
}
