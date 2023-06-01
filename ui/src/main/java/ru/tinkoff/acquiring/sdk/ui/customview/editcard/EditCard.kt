/*
 * Copyright © 2020 Tinkoff Bank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.tinkoff.acquiring.sdk.ui.customview.editcard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.graphics.*
import android.os.Parcelable
import android.text.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.EditCard.EditCardField.*
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.EditCard.EditCardMode.*
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.editable.CardNumberEditable
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.editable.ExpireDateEditable
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.editable.SecureCodeEditable
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.keyboard.SecureKeyboard
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.validators.CardValidator
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author Mariya Chernyadieva
 */
internal class EditCard @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), TextWatcher, View.OnLongClickListener {

    var cardNumber: String = ""
        get() = cardNumberEditable.text.toString()
        set(value) {
            val number = CardFormatter.getRawNumber(value)
            field = number
            this.cardNumberEditable.text = number
            if (checkNumberIsMasked(number)) {
                addFlags(FLAG_MASKED_NUMBER)
            }
            updateCardInputFilter()
            updateView()
        }

    var cardDate: String = ""
        get() = CardFormatter.formatDate(expireDateEditable.text.toString())
        set(value) {
            val formattedDate = CardFormatter.formatDate(value)
            this.expireDateEditable.text = CardFormatter.getRawDate(formattedDate)
            field = CardFormatter.formatDate(formattedDate)

            updateView()
        }

    var cardCvc: String = ""
        get() = secureCodeEditable.text.toString()
        set(value) {
            val formattedCvc = CardFormatter.formatSecurityCode(value)
            field = formattedCvc
            this.secureCodeEditable.text = formattedCvc

            updateView()
        }

    var cardNumberHint: String = ""
        set(value) {
            field = value
            invalidate()
        }
    var cardDateHint: String = ""
        set(value) {
            field = value
            invalidate()
        }
    var cardCvcHint: String = ""
        set(value) {
            field = value
            invalidate()
        }
    var textColorInvalid: Int = 0
        set(value) {
            field = value
            invalidate()
        }
    var textColorHint: Int = 0
        set(value) {
            field = value
            hintPaint.color = value
            invalidate()
        }
    var cursorColor: Int = 0
        set(value) {
            field = value
            cursorPaint.color = value
            invalidate()
        }
    var textColor: Int = 0
        set(value) {
            field = value
            cardNumberPaint.color = value
            lastNumberBlockPaint.color = value
            datePaint.color = value
            cvcPaint.color = value
            invalidate()
        }
    var textSize: Float = 0f
        set(value) {
            field = value
            cardNumberPaint.textSize = value
            lastNumberBlockPaint.textSize = value
            datePaint.textSize = value
            cvcPaint.textSize = value
            hintPaint.textSize = value
            requestLayout()
            invalidate()
        }
    var fontFamily: String? = null
        set(value) {
            field = value
            updateTextTypeface()
            invalidate()
        }
    var textStyle: Int = 0
        set(value) {
            field = value
            updateTextTypeface()
            invalidate()
        }

    var isScanButtonVisible: Boolean = false
        set(value) {
            field = value
            scanButton?.isVisible = value
        }
    var useSecureKeyboard: Boolean = false
        set(value) {
            field = value
            if (value) {
                attachSecureKeyboard()
            } else secureKeyboard.hide()
        }

    var scanButtonClickListener: EditCardScanButtonClickListener? = null
    var textChangedListener: EditCardTextChangedListener? = null
    var cardSystemIconsHolder: EditCardSystemIconsHolder

    @DrawableRes
    var nextIconResId: Int = 0
        set(value) {
            field = value
            val bitmap = value.getBitmapFromVectorDrawableRes(context)
            nextButton = if (bitmap != null) BitmapButton(bitmap) else null
            requestLayout()
            invalidate()
        }

    @DrawableRes
    var scanIconResId: Int = 0
        set(value) {
            field = value
            val bitmap = value.getBitmapFromVectorDrawableRes(context)
            scanButton = if (bitmap != null) BitmapButton(bitmap) else null
            requestLayout()
            invalidate()
        }

    var validateNotExpired: Boolean = false

    private var cursorPosition: Int = 0
    private var editableField: EditCardField = CARD_NUMBER
    private var viewState: Int = FULL_CARD_NUMBER_STATE
    private var savedViewState: Int = -1
    private var flags: Int = 0
    private var mode: EditCardMode

    private val inputConnection: EditCardInputConnection
    private val inputManager: InputMethodManager?
    private val clipboard: ClipboardManager?
    private val cardNumberEditable: CardNumberEditable
    private val expireDateEditable: ExpireDateEditable
    private val secureCodeEditable: SecureCodeEditable

    private val lastNumberBlockPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val cardNumberPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val selectionPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cursorPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hintPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val datePaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val cvcPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val logoPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var isCvcSymbolHidden: Boolean = false
    private var showedCvcIndex: Int = -1

    private var lastNumberBlockPositionXMovable: Float = 0f
    private var lastNumberBlockPositionX: Float = 0f
    private var lastNumberBlockAreaRange: ClosedRange<Float> = 0f..0f

    private var dateHintPositionX: Float = 0f
    private var dateHintWidth: Float = 0f
    private var dateAreaRange: ClosedRange<Float> = 0f..0f

    private var cvcHintPositionX: Float = 0f
    private var cvcHintWidth: Float = 0f
    private var cvcAreaRange: ClosedRange<Float> = 0f..0f

    private val cardLogoPositionRect: RectF = RectF()
    private val cardLogoRect: RectF = RectF()
    private val logoMatrix: Matrix = Matrix()
    private var cardLogo: Bitmap? = null

    private var cardNumberOffsetLeft: Float = 0f
    private var centerViewHeight: Float = 0f
    private val cardNumberMask: String

    private val passwordHidingRunnable: Runnable
    private val cursorBlinkRunnable: Runnable

    private var nextButton: BitmapButton? = null
    private var scanButton: BitmapButton? = null

    private val viewCoordinates = IntArray(2)
    private val selectionRect: RectF = RectF()
    private var popupMenu: EditCardPopupMenu

    private val viewConfiguration = ViewConfiguration.get(context)
    private val scrollDistance = viewConfiguration.scaledTouchSlop
    private var touchPoint1: Float = 0f
    private var touchPoint2: Float = 0f

    private var secureKeyboard: SecureKeyboard

    init {
        val attrsArray = context.obtainStyledAttributes(attrs, R.styleable.EditCard, defStyleAttr, 0)
        try {
            attrsArray.apply {
                val backgroundResId = getResourceId(R.styleable.EditCard_android_background, 0)
                if (backgroundResId == 0) {
                    val backgroundColor = getColor(R.styleable.EditCard_android_background, R.styleable.EditCard_android_background)
                    setBackgroundColor(backgroundColor)
                } else {
                    setBackgroundResource(backgroundResId)
                }

                val typedValue = TypedValue()
                context.theme.resolveAttribute(R.attr.colorAccent, typedValue, true)

                val defaultTextSize = (18 * resources.displayMetrics.scaledDensity).roundToInt()
                textSize = getDimensionPixelSize(R.styleable.EditCard_android_textSize, defaultTextSize).toFloat()
                textStyle = getInt(R.styleable.EditCard_android_textStyle, Typeface.NORMAL)
                textColor = getColor(R.styleable.EditCard_android_textColor, R.styleable.EditCard_android_textColor)
                fontFamily = getString(R.styleable.EditCard_android_fontFamily)
                textColorHint = getColor(R.styleable.EditCard_android_textColorHint, R.styleable.EditCard_android_textColorHint)
                textColorInvalid = getColor(R.styleable.EditCard_acqTextColorInvalid, Color.RED)
                cursorColor = getColor(R.styleable.EditCard_acqCursorColor, typedValue.data)

                cardNumberHint = getString(R.styleable.EditCard_acqNumberHint) ?: "Card number"
                cardDateHint = getString(R.styleable.EditCard_acqDateHint) ?: "MM/YY"
                cardCvcHint = getString(R.styleable.EditCard_acqCvcHint) ?: "CVC"

                nextIconResId = getResourceId(R.styleable.EditCard_acqNextIcon, R.drawable.acq_icon_next)
                scanIconResId = getResourceId(R.styleable.EditCard_acqScanIcon, R.drawable.acq_icon_scan_card)

                mode = EditCardMode.fromInt(getInteger(R.styleable.EditCard_acqMode, DEFAULT.value))
            }
        } finally {
            attrsArray.recycle()
        }

        secureKeyboard = SecureKeyboard(context, attrs)

        isFocusable = true
        isFocusableInTouchMode = true

        cardSystemIconsHolder = DefaultCardIconsHolder(context)
        popupMenu = EditCardPopupMenu(context)
        popupMenu.setOnPopupMenuItemClickListener(getMenuItemClickListener())
        setOnLongClickListener(this)

        cardNumberPaint.textSize = textSize
        cardNumberPaint.color = textColor

        lastNumberBlockPaint.textSize = textSize
        lastNumberBlockPaint.color = textColor

        datePaint.textSize = textSize
        datePaint.color = textColor

        cvcPaint.textSize = textSize
        cvcPaint.color = textColor

        hintPaint.textSize = textSize
        hintPaint.color = textColorHint

        updateTextTypeface()

        cursorPaint.color = Color.TRANSPARENT
        cursorPaint.style = Paint.Style.STROKE
        cursorPaint.strokeWidth = 5f

        selectionPaint.color = cursorColor
        selectionPaint.alpha = 100

        cardNumberMask = "0".repeat(CardPaymentSystem.UNKNOWN.range.last)

        scanButton?.isVisible = isScanButtonVisible

        if (isInEditMode) {
            if (mode == NUMBER_ONLY) {
                scanButton?.isVisible = true
            } else {
                cardLogo = BitmapFactory.decodeResource(resources, R.drawable.acq_visalogo)
                addFlags(FLAG_CARD_SYSTEM_LOGO)
            }
        }

        cardNumberEditable = CardNumberEditable()
        expireDateEditable = ExpireDateEditable()
        expireDateEditable.filters = arrayOf(InputFilter.LengthFilter(CardValidator.MAX_DATE_LENGTH - 1))
        secureCodeEditable = SecureCodeEditable()
        secureCodeEditable.filters = arrayOf(InputFilter.LengthFilter(CardValidator.MAX_CVC_LENGTH))
        inputConnection = EditCardInputConnection(this)
        switchEditable(CARD_NUMBER)

        inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?

        cursorBlinkRunnable = object : Runnable {
            override fun run() {
                cursorPaint.color = if (cursorPaint.color == cursorColor) Color.TRANSPARENT else cursorColor
                invalidate()
                postDelayed(this, 560)
            }
        }

        passwordHidingRunnable = Runnable {
            isCvcSymbolHidden = true
            invalidate()
        }
    }

    override fun onCheckIsTextEditor(): Boolean {
        return !useSecureKeyboard
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        outAttrs.inputType = EditorInfo.TYPE_NUMBER_VARIATION_NORMAL or EditorInfo.TYPE_CLASS_NUMBER
        return inputConnection
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val defaultHeight = 20.dpToPx(context).toInt()
        val defaultWidth = cardNumberPaint.measureText(cardNumberMask).toInt() + (nextButton?.getWidth() ?: 0) * 2 + getCardLogoWidth()
        val measuredPaddingTop = if (paddingTop == 0) 8.dpToPx(context).toInt() else paddingTop
        val measuredPaddingBottom = if (paddingBottom == 0) 8.dpToPx(context).toInt() else paddingBottom
        val measuredPaddingRight = if (paddingRight == 0) 8.dpToPx(context).toInt() else paddingRight
        val measuredPaddingLeft = if (paddingLeft == 0) 8.dpToPx(context).toInt() else paddingLeft
        val heightPaddings = measuredPaddingBottom + measuredPaddingTop
        val widthPaddings = measuredPaddingLeft + measuredPaddingRight

        val widthWithPaddings = defaultWidth + widthPaddings
        val heightWithPaddings = defaultHeight + heightPaddings

        heightSize = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(heightSize, heightWithPaddings)
            MeasureSpec.UNSPECIFIED -> heightWithPaddings
            else -> heightWithPaddings
        }

        widthSize = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(widthSize, widthWithPaddings)
            MeasureSpec.UNSPECIFIED -> widthWithPaddings
            else -> widthWithPaddings
        }

        centerViewHeight = heightSize / 2f

        if (checkFlags(FLAG_CARD_SYSTEM_LOGO)) {
            cardNumberOffsetLeft = measuredPaddingLeft.toFloat() + getCardLogoWidth().toFloat()
            if (viewState != CARD_NUMBER_ANIMATION_STATE) {
                lastNumberBlockPositionXMovable = cardNumberOffsetLeft
            }
        } else {
            cardNumberOffsetLeft = measuredPaddingLeft.toFloat()
        }

        if (viewState == DATE_CVC_STATE) {
            lastNumberBlockPositionX = calculateLastBlockPosition()
            lastNumberBlockAreaRange = calculateLastBlockArea()
        }

        val buttonOffset = 4.dpToPx(context).toInt()

        if (nextButton != null) {
            val nextButtonPositionTop = (heightSize - nextButton!!.getHeight()) / 2
            val nextButtonPositionLeft = widthSize - nextButton!!.getWidth() - measuredPaddingRight
            nextButton!!.setLayoutPosition(nextButtonPositionLeft, nextButtonPositionTop, buttonOffset)
        }

        if (scanButton != null) {
            val scanButtonPositionTop = (heightSize - scanButton!!.getHeight()) / 2
            val scanButtonPositionLeft = widthSize - scanButton!!.getWidth() - measuredPaddingRight
            scanButton!!.setLayoutPosition(scanButtonPositionLeft, scanButtonPositionTop, buttonOffset)
        }

        val cardLogoLeft = measuredPaddingLeft.toFloat()
        val cardLogoTop = centerViewHeight - getCardLogoHeight() / 2f
        val cardLogoRight = getCardLogoWidth().toFloat()
        val cardLogoBottom = centerViewHeight + getCardLogoHeight() / 2f
        cardLogoPositionRect.set(cardLogoLeft, cardLogoTop, cardLogoRight, cardLogoBottom)

        dateHintWidth = hintPaint.measureText(cardDateHint)
        dateHintPositionX = widthSize / 2f
        dateAreaRange = dateHintPositionX..dateHintPositionX + dateHintWidth

        cvcHintWidth = hintPaint.measureText(cardCvcHint)
        cvcHintPositionX = widthSize - cvcHintWidth - measuredPaddingRight
        cvcAreaRange = cvcHintPositionX..cvcHintPositionX + cvcHintWidth


        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onDraw(canvas: Canvas) {
        if (isInEditMode) {
            if (mode == NUMBER_ONLY) {
                drawHint(canvas, cardNumberHint)
                drawButtons(canvas)
            } else {
                drawCardLogo(canvas)
                drawLastNumberBlock(canvas)
                drawDate(canvas)
                if (mode != WITHOUT_CVC && mode != RECURRENT) {
                    drawCvc(canvas)
                }
            }
            return
        }

        if (cardNumber.isEmpty()) {
            drawHint(canvas, cardNumberHint)
            drawCursor(canvas, 0f)
            drawButtons(canvas)
        } else {
            when (viewState) {
                FULL_CARD_NUMBER_STATE, CARD_LOGO_ANIMATION_STATE -> {
                    drawCardNumber(canvas)
                    drawButtons(canvas)
                }
                CARD_NUMBER_ANIMATION_STATE -> {
                    drawCardNumber(canvas)
                    drawLastNumberBlock(canvas)
                    drawDate(canvas)
                    if (mode != WITHOUT_CVC && mode != RECURRENT) {
                        drawCvc(canvas)
                    }
                }
                DATE_CVC_STATE -> {
                    drawLastNumberBlock(canvas)
                    drawDate(canvas)
                    if (mode != WITHOUT_CVC && mode != RECURRENT) {
                        drawCvc(canvas)
                    }
                }
            }

            if (checkFlags(FLAG_SELECTED_TEXT)) {
                drawSelector(canvas)
            }
        }

        if (checkFlags(FLAG_CARD_SYSTEM_LOGO) || viewState == CARD_LOGO_ANIMATION_STATE) {
            drawCardLogo(canvas)
        }
    }

    override fun onFocusChanged(hasFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(hasFocus, direction, previouslyFocusedRect)
        setKeyboardVisible(hasFocus)
        if (hasFocus) {
            startCursorBlinking()
        } else {
            stopCursorBlinking()
            popupMenu.dismiss()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        isFocusable = enabled
        if (!enabled) {
            stopCursorBlinking()
        }
        popupMenu.dismiss()
        setKeyboardVisible(false)
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        return when {
            keyCode == KeyEvent.KEYCODE_BACK && event.action == MotionEvent.ACTION_UP -> {
                if (useSecureKeyboard && secureKeyboard.isShowing()) {
                    secureKeyboard.hide()
                    true
                } else super.onKeyPreIme(keyCode, event)
            }
            else -> super.onKeyPreIme(keyCode, event)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (popupMenu.isShowing) {
            removeFlag(FLAG_SELECTED_TEXT)
            startCursorBlinking()
            popupMenu.dismiss()
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> {
                setKeyboardVisible(false)
                clearFocus()
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val currentCursorPosition = inputConnection.editable.getSpanEnd(Selection.SELECTION_END)
                val newCursorPosition = if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (currentCursorPosition <= 0) 0 else currentCursorPosition - 1
                } else {
                    if (currentCursorPosition >= inputConnection.editable.length) inputConnection.editable.length
                    else currentCursorPosition + 1
                }
                setCursor(newCursorPosition)
                cursorPaint.color = cursorColor
                invalidate()
                true
            }
            KeyEvent.KEYCODE_BACK -> super.onKeyDown(keyCode, event)
            else -> inputConnection.sendKeyEvent(event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (popupMenu.isShowing) {
            removeFlag(FLAG_SELECTED_TEXT)
            startCursorBlinking()
            popupMenu.dismiss()
        }
        return super.onKeyUp(keyCode, event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!this.isEnabled) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                removeFlag(FLAG_SCROLL)
                touchPoint1 = event.y
                popupMenu.dismiss()
                removeFlag(FLAG_SELECTED_TEXT)

                getLocationOnScreen(viewCoordinates)
                popupMenu.setPosition(event.rawX.toInt(), viewCoordinates[1])
            }
            MotionEvent.ACTION_MOVE -> {
                touchPoint2 = event.y
                val distance = touchPoint1 - touchPoint2
                if (distance.absoluteValue >= scrollDistance) {
                    addFlags(FLAG_SCROLL)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!checkFlags(FLAG_SCROLL, FLAG_SELECTED_TEXT)) {
                    val completed = handleTouch(event)
                    return if (completed) completed else super.onTouchEvent(event)
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onLongClick(v: View?): Boolean {
        return if (isShowingMaskedNumber()) {
            false
        } else if (!popupMenu.isShowing) {
            showContextMenu()
        } else {
            true
        }
    }

    override fun performLongClick(): Boolean {
        return if (isShowingMaskedNumber()) {
            false
        } else if (!popupMenu.isShowing) {
            showContextMenu()
        } else {
            super.performLongClick()
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?) {
        super.onCreateContextMenu(menu)

        val isTextNotEmpty = inputConnection.editable.text.isNotEmpty()
        val isNotEmptyAndSecure = isTextNotEmpty && inputConnection.editable !is SecureCodeEditable
        val isClipboardNotEmpty = clipboard != null && !clipboard.hasPrimaryClip() ||
                (clipboard?.primaryClipDescription != null && clipboard.primaryClipDescription!!.hasMimeType(MIMETYPE_TEXT_PLAIN))

        popupMenu.addItem(android.R.string.cut, android.R.id.cut, isNotEmptyAndSecure)
        popupMenu.addItem(android.R.string.copy, android.R.id.copy, isNotEmptyAndSecure)
        popupMenu.addItem(android.R.string.paste, android.R.id.paste, isClipboardNotEmpty)

        popupMenu.show(this)

        addFlags(FLAG_SELECTED_TEXT)
        inputConnection.setSelection(0, inputConnection.editable.text.length)
        stopCursorBlinking()
    }

    override fun afterTextChanged(editable: Editable?) {
        when (editable) {
            is CardNumberEditable -> {
                val paymentSystem = CardPaymentSystem.resolve(cardNumber)
                if (!paymentSystem.showLogo) {
                    if (editable.isEmpty()) {
                        post { hideLogoIfNeed() }
                    } else {
                        postDelayed({ hideLogoIfNeed() }, 150)
                    }
                } else {
                    when {
                        !checkFlags(FLAG_CARD_SYSTEM_LOGO) && viewState == CARD_LOGO_ANIMATION_STATE -> {
                            addFlags(FLAG_CARD_SYSTEM_LOGO)
                        }
                        !checkFlags(FLAG_CARD_SYSTEM_LOGO) && viewState != CARD_LOGO_ANIMATION_STATE -> {
                            if (defineCardLogo()) {
                                showCardSystemLogo()
                            }
                        }
                        checkFlags(FLAG_CARD_SYSTEM_LOGO) -> {
                            if (isValid(CARD_NUMBER) && mode != NUMBER_ONLY && !checkFlags(FLAG_PASTED_TEXT) &&
                                shouldAutoSwitchFromCardNumber()) {
                                showDateAndCvc()
                            }
                        }
                    }
                }
            }
            is ExpireDateEditable -> {
                if (viewState == FULL_CARD_NUMBER_STATE) {
                    showDateAndCvc()
                } else if ((isValid(EXPIRE_DATE) && mode != WITHOUT_CVC && mode != RECURRENT)
                        || mode == EDIT_CVC_ONLY) {
                    switchEditable(SECURE_CODE)
                    setCursor(cardCvc.length)
                }
            }
        }
        removeFlag(FLAG_PASTED_TEXT)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        handleTextChanged(s, before, count)
        setCursor(start)
        invalidate()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val savedState = EditCardSavedState(super.onSaveInstanceState())
        savedState.apply {
            editableState = this@EditCard.editableField
            viewState = this@EditCard.viewState
            savedViewState = this@EditCard.viewState
            flags = this@EditCard.flags
            mode = this@EditCard.mode
            cursorPosition = this@EditCard.inputConnection.editable.getSpanEnd(Selection.SELECTION_END)
            cardNumber = this@EditCard.cardNumber
            cardDate = this@EditCard.cardDate
        }
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as EditCardSavedState
        super.onRestoreInstanceState(savedState.superState)

        flags = savedState.flags ?: 0
        removeFlag(FLAG_SELECTED_TEXT)

        mode = savedState.mode ?: DEFAULT

        cardNumber = savedState.cardNumber ?: ""
        cardNumberEditable.text = cardNumber
        updateVisibilityOfValidations(CARD_NUMBER)

        cardDate = savedState.cardDate ?: ""
        expireDateEditable.text = CardFormatter.getRawDate(cardDate)
        updateVisibilityOfValidations(EXPIRE_DATE)

        cardCvc = ""
        secureCodeEditable.text = cardCvc

        switchEditable(savedState.editableState ?: CARD_NUMBER)
        switchViewState(savedState.viewState ?: FULL_CARD_NUMBER_STATE)
        savedViewState = savedState.viewState ?: -1
        setCursor(savedState.cursorPosition ?: 0)

        if (checkFlags(FLAG_CARD_SYSTEM_LOGO)) {
            defineCardLogo()
        }
    }

    fun clearInput() {
        this.cardNumber = ""
        this.cardDate = ""
        this.cardCvc = ""
    }

    fun maskCardNumber(visibleCharsRight: Int) {
        this.cardNumber = maskNumber(cardNumber, visibleCharsRight)
    }

    fun isFilledAndCorrect(): Boolean {
        return when (mode) {
            DEFAULT -> isValid(CARD_NUMBER) && isValid(EXPIRE_DATE) && isValid(SECURE_CODE)
            EDIT_CVC_ONLY -> isValid(SECURE_CODE)
            NUMBER_ONLY -> isValid(CARD_NUMBER)
            WITHOUT_CVC, RECURRENT -> isValid(CARD_NUMBER) && isValid(EXPIRE_DATE)
        }
    }

    fun setMode(newMode: EditCardMode) {
        this.mode = newMode
        updateView()
    }

    fun setOnScanButtonClickListener(listener: () -> Unit) {
        this.scanButtonClickListener = object : EditCardScanButtonClickListener {
            override fun onScanButtonClick() = listener()
        }
    }

    fun setOnTextChangedListener(listener: (field: EditCardField, text: CharSequence) -> Unit) {
        this.textChangedListener = object : EditCardTextChangedListener {
            override fun onTextChanged(field: EditCardField, text: CharSequence) = listener(field, text)
        }
    }

    private fun updateView() {
        updateVisibilityOfValidations(CARD_NUMBER)
        if (isValid(CARD_NUMBER) && mode != NUMBER_ONLY && savedViewState != FULL_CARD_NUMBER_STATE) {
            switchViewState(DATE_CVC_STATE)
            switchEditable(EXPIRE_DATE)
            setCursor(cardDate.length)
            updateVisibilityOfValidations(EXPIRE_DATE)
            if ((isValid(EXPIRE_DATE) && mode != WITHOUT_CVC && mode != RECURRENT)
                    || mode == EDIT_CVC_ONLY) {
                switchEditable(SECURE_CODE)
                setCursor(cardCvc.length)
            }
        } else {
            switchViewState(FULL_CARD_NUMBER_STATE)
            if (!checkFlags(FLAG_MASKED_NUMBER)) {
                switchEditable(CARD_NUMBER)
                setCursor(cardNumber.length)
            }
        }

        if (cardNumber.isNotEmpty() && defineCardLogo()) {
            addFlags(FLAG_CARD_SYSTEM_LOGO)
        } else {
            removeFlag(FLAG_CARD_SYSTEM_LOGO)
        }

        if (mode == RECURRENT) {
            this.isEnabled = false
            stopCursorBlinking()
        }

        requestLayout()
        invalidate()
    }

    private fun updateTextTypeface() {
        val typeface = Typeface.create(fontFamily, textStyle)
        cardNumberPaint.typeface = typeface
        lastNumberBlockPaint.typeface = typeface
        datePaint.typeface = typeface
        cvcPaint.typeface = typeface
        hintPaint.typeface = typeface
    }

    private fun handleTextChanged(text: CharSequence, before: Int, count: Int) {
        when (editableField) {
            CARD_NUMBER -> {
                updateCardInputFilter()
                defineCardLogo()
                textChangedListener?.onTextChanged(CARD_NUMBER, text)
            }
            EXPIRE_DATE -> {
                textChangedListener?.onTextChanged(EXPIRE_DATE, CardFormatter.formatDate(text.toString()))
            }
            SECURE_CODE -> {
                showedCvcIndex = if (checkFlags(FLAG_PASTED_TEXT)) cardCvc.length - 1 else before
                isCvcSymbolHidden = count == 0
                removeCallbacks(passwordHidingRunnable)
                postDelayed(passwordHidingRunnable, 1300)
                textChangedListener?.onTextChanged(SECURE_CODE, text)
            }
        }
        updateVisibilityOfValidations(editableField)
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        if (viewState == FULL_CARD_NUMBER_STATE && (nextButton != null && nextButton!!.isVisible && nextButton!!.isButtonClick(event.x, event.y))) {
            showDateAndCvc()
            return false
        }

        if (viewState == FULL_CARD_NUMBER_STATE && (scanButton != null && scanButton!!.isVisible && scanButton!!.isButtonClick(event.x, event.y))) {
            scanButtonClickListener?.onScanButtonClick()
            return true
        }

        startCursorBlinking()
        requestFocus()
        setKeyboardVisible(true)

        when (viewState) {
            FULL_CARD_NUMBER_STATE -> {
                if (!checkFlags(FLAG_MASKED_NUMBER) && mode != EDIT_CVC_ONLY && mode != RECURRENT) {
                    calculateNewCursorPosition(event.x)
                    invalidate()
                }
            }
            DATE_CVC_STATE -> {
                when (event.x) {
                    in dateAreaRange -> {
                        if (mode != EDIT_CVC_ONLY && mode != RECURRENT) {
                            switchEditable(EXPIRE_DATE)
                            calculateNewCursorPosition(event.x)
                            invalidate()
                        }
                    }
                    in cvcAreaRange -> {
                        if (mode != WITHOUT_CVC && mode != RECURRENT) {
                            switchEditable(SECURE_CODE)
                            calculateNewCursorPosition(event.x)
                            invalidate()
                        }
                    }
                    in lastNumberBlockAreaRange -> {
                        if (mode != EDIT_CVC_ONLY && mode != RECURRENT) {
                            showFullCardNumber()
                        }
                    }
                }
            }
        }
        return false
    }

    private fun updateVisibilityOfValidations(field: EditCardField) {
        val fieldValid = !isFilled(field) || isValid(field)
        val updatedTextColor = if (fieldValid) textColor else textColorInvalid
        when (field) {
            CARD_NUMBER -> {
                nextButton?.isVisible = isValid(field) && mode != NUMBER_ONLY
                scanButton?.isVisible = if (nextButton != null && nextButton!!.isVisible) false else isScanButtonVisible
                cardNumberPaint.color = updatedTextColor
            }
            EXPIRE_DATE -> datePaint.color = updatedTextColor
            SECURE_CODE -> cvcPaint.color = updatedTextColor
        }
        invalidate()
    }

    private fun isValid(field: EditCardField): Boolean {
        return when (field) {
            CARD_NUMBER -> CardValidator.validateCardNumber(cardNumber) || checkFlags(FLAG_MASKED_NUMBER)
            EXPIRE_DATE -> CardValidator.validateExpireDate(cardDate, validateNotExpired)
            SECURE_CODE -> CardValidator.validateSecurityCode(cardCvc)
        }
    }

    private fun shouldAutoSwitchFromCardNumber(): Boolean {
        val paymentSystem = CardPaymentSystem.resolve(cardNumber)
        return cardNumber.length == paymentSystem.range.last
    }

    private fun isFilled(field: EditCardField): Boolean {
        return when (field) {
            CARD_NUMBER -> {
                val paymentSystem = CardPaymentSystem.resolve(cardNumber)
                cardNumber.length in paymentSystem.range
            }
            EXPIRE_DATE -> cardDate.length == CardValidator.MAX_DATE_LENGTH
            SECURE_CODE -> cardCvc.length == CardValidator.MAX_CVC_LENGTH
        }
    }

    private fun drawTextCenter(
            canvas: Canvas,
            paint: Paint,
            text: CharSequence,
            start: Int = 0,
            end: Int = text.length,
            x: Float = cardNumberOffsetLeft
    ) {
        val y = (this.height / 2) - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(text, start, end, x, y, paint)
    }

    private fun drawCardNumber(canvas: Canvas) {
        val cardFormat = CardFormatter.resolveCardFormat(cardNumber)
        val textWidth = cardNumberPaint.measureText(cardNumber)
        val offset = textWidth / cardNumber.length / 2
        var blockOffset = cardNumberOffsetLeft

        cardFormat.forEachBlockUntil(cardNumber.length - 1) { start, end ->
            drawTextCenter(canvas, cardNumberPaint, cardNumber, start, end, blockOffset)
            blockOffset += cardNumberPaint.measureText(cardNumber, start, end) + offset
        }

        val textWidthToPosition = cardNumberPaint.measureText(cardNumber, 0, cursorPosition)
        val cursorXPosition = textWidthToPosition + (offset * cardFormat.getBlockNumber(cursorPosition))

        if (!checkFlags(FLAG_MASKED_NUMBER)) {
            drawCursor(canvas, cursorXPosition)
        }
    }

    private fun drawButtons(canvas: Canvas) {
        if (nextButton != null && nextButton!!.isVisible) {
            nextButton!!.drawButton(canvas, iconPaint)
        } else {
            if (scanButton != null && scanButton!!.isVisible) {
                scanButton!!.drawButton(canvas, iconPaint)
            }
        }
    }

    private fun drawLastNumberBlock(canvas: Canvas) {
        if (isInEditMode) {
            drawTextCenter(canvas, lastNumberBlockPaint, "0777", 0, x = cardNumberOffsetLeft)
        } else {
            val lastNumberBlock = cardNumber.subSequence(cardNumber.length - 4, cardNumber.length).toString()
            drawTextCenter(canvas, lastNumberBlockPaint, lastNumberBlock, x = lastNumberBlockPositionXMovable)
        }
    }

    private fun drawDate(canvas: Canvas) {
        if (cardDate.isEmpty()) {
            drawHint(canvas, cardDateHint, dateHintPositionX)
            if (editableField == EXPIRE_DATE) {
                drawCursor(canvas, dateHintPositionX)
            }
        } else {
            if (cardDate.length > 1) {
                drawTextCenter(canvas, datePaint, cardDate, 0, 2, dateHintPositionX)

                val delimiterPositionX = datePaint.measureText(cardDate, 0, 2) + dateHintPositionX
                drawTextCenter(canvas, datePaint, cardDate, 2, cardDate.length, delimiterPositionX)
            } else {
                drawTextCenter(canvas, datePaint, cardDate, x = dateHintPositionX)
            }

            if (editableField == EXPIRE_DATE) {
                val delimiterCursor = if (cursorPosition != cardDate.length && cardDate.length > 2 && cursorPosition in 2 until 5) 1 else 0
                val cursorPositionX = dateHintPositionX + datePaint.measureText(cardDate, 0, cursorPosition + delimiterCursor)
                drawCursor(canvas, cursorPositionX)
            }
        }
    }

    private fun drawCvc(canvas: Canvas) {
        if (cardCvc.isEmpty()) {
            drawHint(canvas, cardCvcHint, cvcHintPositionX)
            if (editableField == SECURE_CODE) {
                drawCursor(canvas, cvcHintPositionX)
            }
        } else {
            var symbolPositionX = cvcHintPositionX
            var symbolWidth: Float
            val dotRadius = cvcPaint.measureText("0") / 2.5f
            val offset = dotRadius / 2

            cardCvc.forEachIndexed { index, char ->
                symbolWidth = cvcPaint.measureText(char.toString())
                if (!isCvcSymbolHidden && showedCvcIndex == index) {
                    drawTextCenter(canvas, cvcPaint, char.toString(), x = symbolPositionX)
                    symbolPositionX += symbolWidth
                } else {
                    symbolPositionX += dotRadius
                    canvas.drawCircle(symbolPositionX, centerViewHeight, dotRadius, cvcPaint)
                    symbolPositionX += dotRadius + offset
                }
            }

            if (editableField == SECURE_CODE && cursorPosition <= CardValidator.MAX_CVC_LENGTH) {
                val cursorPositionX = cvcHintPositionX + cvcPaint.measureText(cardCvc, 0, cursorPosition) + dotRadius - offset * 1.5f
                drawCursor(canvas, cursorPositionX)
            }
        }
    }

    private fun drawHint(canvas: Canvas, hint: String, xPosition: Float = cardNumberOffsetLeft) {
        drawTextCenter(canvas, hintPaint, hint, x = xPosition)
    }

    private fun drawCursor(canvas: Canvas, positionX: Float) {
        val cursorAdditionalSize = 1.dpToPx(context)
        val cursorOffsetY = centerViewHeight - cardNumberPaint.textSize / 2f - cursorAdditionalSize
        val cursorHeight = centerViewHeight + cardNumberPaint.textSize / 2f + cursorAdditionalSize
        val correctPositionX = when (editableField) {
            EXPIRE_DATE, SECURE_CODE -> positionX
            else -> cardNumberOffsetLeft + positionX
        }
        canvas.drawLine(correctPositionX, cursorOffsetY, correctPositionX, cursorHeight, cursorPaint)
    }

    private fun drawCardLogo(canvas: Canvas) {
        cardLogo?.let { logo ->
            cardLogoRect.set(0f, 0f, logo.width.toFloat(), logo.height.toFloat())
            logoMatrix.setRectToRect(cardLogoRect, cardLogoPositionRect, Matrix.ScaleToFit.CENTER)
            canvas.drawBitmap(logo, logoMatrix, logoPaint)
        }
    }

    private fun drawSelector(canvas: Canvas) {
        val verticalOffset = 2.dpToPx(context)

        val top = centerViewHeight - cardNumberPaint.textSize / 2f - verticalOffset
        var left = 0f
        var right = 0f
        val bottom = cardNumberPaint.textSize + top + verticalOffset * 2

        when (editableField) {
            CARD_NUMBER -> {
                left = cardNumberOffsetLeft
                val textWidth = cardNumberPaint.measureText(cardNumber)
                val offset = textWidth / cardNumber.length / 2
                val offsetCount = CardFormatter.resolveCardFormat(cardNumber).getBlockNumber(cardNumber.length - 1)

                right = textWidth + left + offsetCount * offset
            }
            EXPIRE_DATE -> {
                left = dateHintPositionX
                right = datePaint.measureText(cardDate) + left
            }
            SECURE_CODE -> {
                left = cvcHintPositionX
                right = cvcPaint.measureText(cardCvc) + left
            }
        }

        selectionRect.set(left, top, right, bottom)
        canvas.drawRect(selectionRect, selectionPaint)
    }

    private fun showDateAndCvc() {
        stopCursorBlinking()

        val cardNumberAlphaAnimator = ValueAnimator.ofInt(Color.alpha(textColor), 0).apply {
            duration = ALPHA_DATE_CVC_ANIMATION_DURATION
            addUpdateListener {
                cardNumberPaint.alpha = it.animatedValue as Int
                invalidate()
            }
        }

        lastNumberBlockPositionX = calculateLastBlockPosition()
        val lastBlockTranslateXAnimator = ValueAnimator.ofFloat(lastNumberBlockPositionX, cardNumberOffsetLeft).apply {
            duration = TRANSLATE_LAST_BLOCK_ANIMATION_DURATION
            startDelay = 140
            addUpdateListener {
                lastNumberBlockPositionXMovable = it.animatedValue as Float
                invalidate()
            }
        }

        val dateCvcAlphaAnimator = ValueAnimator.ofInt(0, 255).apply {
            duration = ALPHA_DATE_CVC_ANIMATION_DURATION
            startDelay = 200
            addUpdateListener {
                val updatedValue = it.animatedValue as Int
                setHintAlpha(updatedValue)
                setDateAlpha(updatedValue)
                setCvcAlpha(updatedValue)
                invalidate()
            }
        }

        AnimatorSet().apply {
            playTogether(cardNumberAlphaAnimator, lastBlockTranslateXAnimator, dateCvcAlphaAnimator)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    // Setting starting values of delayed animations
                    setHintAlpha(0)
                    setDateAlpha(0)
                    setCvcAlpha(0)
                    lastNumberBlockPositionXMovable = lastNumberBlockPositionX

                    switchEditable(EXPIRE_DATE)
                    setCursor(cardDate.length)
                    switchViewState(CARD_NUMBER_ANIMATION_STATE)
                }

                override fun onAnimationEnd(animation: Animator) {
                    // AnimatorSet triggers onAnimationStart callback AFTER the initial tick of inner animations, which
                    // causes problems when animation scale set to zero on device, hence we manually set target values
                    // of delayed animations in onAnimationEnd
                    setHintAlpha(255)
                    setDateAlpha(255)
                    setCvcAlpha(255)
                    lastNumberBlockPositionXMovable = cardNumberOffsetLeft

                    if (hasFocus()) {
                        startCursorBlinking()
                    }
                    lastNumberBlockAreaRange = calculateLastBlockArea()
                    cardNumberPaint.color = textColor
                    switchViewState(DATE_CVC_STATE)
                    updateVisibilityOfValidations(EXPIRE_DATE)
                    invalidate()
                }
            })
        }.start()
    }

    private fun setHintAlpha(alpha: Int) {
        hintPaint.alpha = minOf(alpha, Color.alpha(textColorHint))
    }

    private fun setDateAlpha(alpha: Int) {
        datePaint.alpha = minOf(alpha, Color.alpha(textColor))
    }

    private fun setCvcAlpha(alpha: Int) {
        cvcPaint.alpha = minOf(alpha, Color.alpha(textColor))
    }

    private fun showFullCardNumber() {
        stopCursorBlinking()

        val cardNumberAlphaAnimator = ValueAnimator.ofInt(0, Color.alpha(textColor)).apply {
            duration = ALPHA_CARD_NUMBER_ANIMATION_DURATION
            startDelay = 140
            addUpdateListener {
                cardNumberPaint.alpha = it.animatedValue as Int
                invalidate()
            }
        }

        val lastBlockTranslateXAnimator = ValueAnimator.ofFloat(cardNumberOffsetLeft, lastNumberBlockPositionX).apply {
            duration = TRANSLATE_LAST_BLOCK_ANIMATION_DURATION
            addUpdateListener {
                lastNumberBlockPositionXMovable = it.animatedValue as Float
                invalidate()
            }
        }

        val hintAlphaAnimator = ValueAnimator.ofInt(Color.alpha(textColorHint), 0).apply {
            duration = ALPHA_DATE_CVC_ANIMATION_DURATION
            addUpdateListener {
                hintPaint.alpha = it.animatedValue as Int
                invalidate()
            }
        }

        val dateCvcAlphaAnimator = ValueAnimator.ofInt(Color.alpha(textColor), 0).apply {
            duration = ALPHA_DATE_CVC_ANIMATION_DURATION
            addUpdateListener {
                datePaint.alpha = it.animatedValue as Int
                cvcPaint.alpha = it.animatedValue as Int
                invalidate()
            }
        }

        AnimatorSet().apply {
            playTogether(cardNumberAlphaAnimator, lastBlockTranslateXAnimator, dateCvcAlphaAnimator, hintAlphaAnimator)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    cardNumberPaint.alpha = 0
                    switchViewState(CARD_NUMBER_ANIMATION_STATE)
                    if (!checkFlags(FLAG_MASKED_NUMBER)) {
                        switchEditable(CARD_NUMBER)
                        setCursor(cardNumber.length)
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (hasFocus()) {
                        startCursorBlinking()
                    }
                    cardNumberPaint.color = textColor
                    switchViewState(FULL_CARD_NUMBER_STATE)
                    updateVisibilityOfValidations(CARD_NUMBER)

                    hintPaint.color = textColorHint
                    datePaint.color = textColor
                    cvcPaint.color = textColor
                    invalidate()
                }
            })
        }.start()
    }

    private fun showCardSystemLogo() {
        stopCursorBlinking()

        val logoAlphaAnimator = ValueAnimator.ofInt(0, 255).apply {
            duration = CARD_LOGO_ANIMATION_DURATION
            addUpdateListener {
                logoPaint.alpha = it.animatedValue as Int
                invalidate()
            }
        }

        val targetCardNumberPositionX = cardNumberOffsetLeft + getCardLogoWidth().toFloat()
        val cardNumberTranslateXAnimator = ValueAnimator.ofFloat(cardNumberOffsetLeft, targetCardNumberPositionX).apply {
            duration = CARD_LOGO_ANIMATION_DURATION
            interpolator = OvershootInterpolator()
            addUpdateListener {
                cardNumberOffsetLeft = it.animatedValue as Float
                invalidate()
            }
        }

        AnimatorSet().apply {
            playSequentially(cardNumberTranslateXAnimator, logoAlphaAnimator)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    logoPaint.alpha = 0
                    switchViewState(CARD_LOGO_ANIMATION_STATE)
                    addFlags(FLAG_CARD_SYSTEM_LOGO)
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (hasFocus()) {
                        startCursorBlinking()
                    }
                    switchViewState(FULL_CARD_NUMBER_STATE)
                    logoPaint.alpha = 255
                    invalidate()

                    if (!checkFlags(FLAG_CARD_SYSTEM_LOGO)) {
                        hideCardSystemLogo()
                    }
                }
            })
        }.start()
    }

    private fun hideCardSystemLogo() {
        stopCursorBlinking()

        val logoAlphaAnimator = ValueAnimator.ofInt(255, 0).apply {
            duration = CARD_LOGO_ANIMATION_DURATION
            addUpdateListener {
                logoPaint.alpha = it.animatedValue as Int
                invalidate()
            }
        }

        val targetCardNumberPositionX = cardNumberOffsetLeft - getCardLogoWidth().toFloat()
        val translateXAnimator = ValueAnimator.ofFloat(cardNumberOffsetLeft, targetCardNumberPositionX).apply {
            duration = CARD_LOGO_ANIMATION_DURATION
            interpolator = OvershootInterpolator()
            addUpdateListener {
                cardNumberOffsetLeft = it.animatedValue as Float
                invalidate()
            }
        }

        AnimatorSet().apply {
            playSequentially(logoAlphaAnimator, translateXAnimator)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    logoPaint.alpha = 255
                    switchViewState(CARD_LOGO_ANIMATION_STATE)
                    removeFlag(FLAG_CARD_SYSTEM_LOGO)
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (hasFocus()) {
                        startCursorBlinking()
                    }
                    switchViewState(FULL_CARD_NUMBER_STATE)
                    logoPaint.alpha = 255
                    invalidate()

                    if (checkFlags(FLAG_CARD_SYSTEM_LOGO)) {
                        defineCardLogo()
                        showCardSystemLogo()
                    }
                }
            })
        }.start()
    }

    private fun calculateNewCursorPosition(clickedPositionX: Float) {
        var newCursorPosition = 0
        var fieldOffset = 0f
        var textWidth = 0f
        var text = ""

        when (editableField) {
            CARD_NUMBER -> {
                text = cardNumber
                if (text.isNotEmpty()) {
                    textWidth = cardNumberPaint.measureText(cardNumber)
                    val halfSymbolWidth = textWidth / cardNumber.length / 2

                    textWidth += halfSymbolWidth * CardFormatter.resolveCardFormat(cardNumber)
                        .getBlockNumber(cardNumber.length - 1)
                    fieldOffset = cardNumberOffsetLeft
                }
            }
            EXPIRE_DATE -> {
                text = cardDate
                if (text.isNotEmpty()) {
                    textWidth = cardNumberPaint.measureText(cardDate)

                    if (cardDate.length > 1) {
                        val delimiterWidth = cardNumberPaint.measureText(CardFormatter.DATE_DELIMITER)
                        textWidth += delimiterWidth
                    }

                    fieldOffset = dateHintPositionX
                }
            }
            SECURE_CODE -> {
                text = cardCvc
                if (text.isNotEmpty()) {
                    textWidth = cardNumberPaint.measureText(cardCvc)
                    fieldOffset = cvcHintPositionX
                }
            }
        }

        if (text.isNotEmpty()) {
            newCursorPosition = ((clickedPositionX - fieldOffset) / ((textWidth) / text.length)).roundToInt()

            if (newCursorPosition > text.length) {
                newCursorPosition = text.length
            } else if (newCursorPosition < 0) {
                newCursorPosition = 0
            }
        }

        setCursor(newCursorPosition)
    }

    private fun calculateLastBlockPosition(): Float {
        val length = cardNumber.length
        val lastBlockStart = length - 4
        val textWidth = cardNumberPaint.measureText(cardNumber)
        val offset = textWidth / length / 2
        val offsetCount = CardFormatter.resolveCardFormat(cardNumber).getBlockNumber(lastBlockStart)

        return cardNumberPaint.measureText(cardNumber, 0, lastBlockStart) + offset * offsetCount + cardNumberOffsetLeft
    }

    private fun calculateLastBlockArea(): ClosedFloatingPointRange<Float> {
        val lastBlockWidth = cardNumberPaint.measureText(cardNumber, cardNumber.length - 4, cardNumber.length)
        return 0f..lastBlockWidth + getCardLogoWidth()
    }

    private fun getCardLogoWidth(): Int {
        val proportionConst = 2.4
        return (cardNumberPaint.textSize * proportionConst).toInt()
    }

    private fun getCardLogoHeight(): Int {
        return cardNumberPaint.textSize.toInt()
    }

    private fun defineCardLogo(): Boolean {
        if (!isInEditMode) {
            val newLogo = cardSystemIconsHolder.getCardSystemLogo(cardNumber)
            if (newLogo != null) {
                cardLogo = newLogo
            }
        }
        return cardLogo != null
    }

    private fun attachSecureKeyboard() {
        val root = (context as Activity).window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val keyboard: SecureKeyboard? = root.findViewById(R.id.edit_card_secure_keyboard)

        if (keyboard == null) {
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.gravity = Gravity.BOTTOM
            secureKeyboard.id = R.id.edit_card_secure_keyboard
            secureKeyboard.layoutParams = params
            secureKeyboard.keyClickListener = inputConnection
            root.addView(secureKeyboard)
        } else {
            secureKeyboard = keyboard
            secureKeyboard.keyClickListener = inputConnection
        }
    }

    private fun setKeyboardVisible(visible: Boolean) {
        if (useSecureKeyboard) {
            attachSecureKeyboard()
        }

        if (visible) {
            if (useSecureKeyboard) {
                secureKeyboard.show()
            } else inputManager?.showSoftInput(this, 0)
        } else {
            if (useSecureKeyboard) {
                secureKeyboard.hide()
            } else inputManager?.hideSoftInputFromWindow(this.applicationWindowToken, 0)
        }
    }

    private fun setCursor(position: Int) {
        inputConnection.setSelection(position, position)
        cursorPosition = position
    }

    private fun stopCursorBlinking() {
        removeCallbacks(cursorBlinkRunnable)
        cursorPaint.color = Color.TRANSPARENT
        invalidate()
    }

    private fun startCursorBlinking() {
        removeCallbacks(cursorBlinkRunnable)
        post(cursorBlinkRunnable)
    }

    private fun hideLogoIfNeed() {
        val showLogo = CardPaymentSystem.resolve(cardNumber).showLogo
        if (!showLogo && checkFlags(FLAG_CARD_SYSTEM_LOGO) && viewState != CARD_LOGO_ANIMATION_STATE) {
            hideCardSystemLogo()
        }
    }

    private fun updateCardInputFilter() {
        val paymentSystem = CardPaymentSystem.resolve(cardNumber)
        cardNumberEditable.filters = arrayOf(InputFilter.LengthFilter(paymentSystem.range.last))
    }

    private fun switchEditable(newEditableField: EditCardField) {
        val editable = when (newEditableField) {
            CARD_NUMBER -> cardNumberEditable
            EXPIRE_DATE -> expireDateEditable
            SECURE_CODE -> secureCodeEditable
        }
        inputConnection.setCurrentEditable(editable)
        editableField = newEditableField
    }

    private fun switchViewState(newSate: Int) {
        this.viewState = newSate
    }

    private fun getMenuItemClickListener(): EditCardPopupMenu.OnPopupMenuItemClickListener {
        return object : EditCardPopupMenu.OnPopupMenuItemClickListener {
            override fun onItemClick(view: View) {
                when (view.id) {
                    android.R.id.paste -> {
                        val pasteText = clipboard?.primaryClip?.getItemAt(0)?.text
                        if (pasteText != null) {
                            val formattedText = when (editableField) {
                                CARD_NUMBER -> CardFormatter.getRawNumber(pasteText.toString())
                                EXPIRE_DATE -> CardFormatter.getRawDate(CardFormatter.formatDate(pasteText.toString()))
                                SECURE_CODE -> CardFormatter.formatSecurityCode(pasteText.toString())
                            }
                            addFlags(FLAG_PASTED_TEXT)
                            inputConnection.editable.updateText(formattedText)
                        }
                    }
                    android.R.id.cut -> {
                        clipboard?.setPrimaryClip(ClipData.newPlainText(null, prepareTextForCopy()))
                        inputConnection.editable.updateText("")
                    }
                    android.R.id.copy -> {
                        clipboard?.setPrimaryClip(ClipData.newPlainText(null, prepareTextForCopy()))
                    }
                }
                popupMenu.dismiss()
                removeFlag(FLAG_SELECTED_TEXT)
                startCursorBlinking()
            }
        }
    }

    private fun prepareTextForCopy(): String {
        var text = inputConnection.editable.text.toString()
        if (editableField == EXPIRE_DATE) {
            text = CardFormatter.formatDate(text)
        }
        return text
    }

    private fun isShowingMaskedNumber(): Boolean {
        return viewState == FULL_CARD_NUMBER_STATE && checkFlags(FLAG_MASKED_NUMBER)
    }

    private fun maskNumber(cardNumber: String, visibleCharsRight: Int): String {
        addFlags(FLAG_MASKED_NUMBER)

        val length: Int = cardNumber.length
        var dataToShow = String(CharArray(length - visibleCharsRight)).replace('\u0000', MASK_CHAR)
        dataToShow += cardNumber.substring(length - visibleCharsRight, length)
        return dataToShow
    }

    private fun checkNumberIsMasked(cardNumber: String): Boolean {
        return cardNumber.contains(MASK_CHAR)
    }

    private fun checkFlags(vararg flags: Int): Boolean {
        var contains = false
        flags.forEach {
            if (this.flags and it == it) {
                contains = true
            }
        }
        return contains
    }

    private fun removeFlag(flag: Int) {
        flags = flags and flag.inv()
    }

    private fun addFlags(vararg flags: Int) {
        flags.forEach {
            this.flags = this.flags or it
        }
    }

    companion object {

        private const val FLAG_CARD_SYSTEM_LOGO = 1 shl 5
        private const val FLAG_SELECTED_TEXT = 1 shl 6
        private const val FLAG_MASKED_NUMBER = 1 shl 7
        private const val FLAG_PASTED_TEXT = 1 shl 8
        private const val FLAG_SCROLL = 1 shl 9

        private const val FULL_CARD_NUMBER_STATE = 0
        private const val CARD_NUMBER_ANIMATION_STATE = 1
        private const val CARD_LOGO_ANIMATION_STATE = 2
        private const val DATE_CVC_STATE = 3

        private const val TRANSLATE_LAST_BLOCK_ANIMATION_DURATION = 250L
        private const val ALPHA_CARD_NUMBER_ANIMATION_DURATION = 250L
        private const val ALPHA_DATE_CVC_ANIMATION_DURATION = 200L
        private const val CARD_LOGO_ANIMATION_DURATION = 150L

        private const val MASK_CHAR = '*'
    }

    enum class EditCardMode(val value: Int) {
        DEFAULT(1),
        WITHOUT_CVC(1 shl 1),
        NUMBER_ONLY(1 shl 2),
        EDIT_CVC_ONLY(1 shl 3),
        RECURRENT(1 shl 4);

        companion object {
            fun fromInt(value: Int): EditCardMode {
                return values().associateBy { it.value }[value] ?: DEFAULT
            }
        }
    }

    enum class EditCardField(val value: Int) {
        CARD_NUMBER(0),
        EXPIRE_DATE(1),
        SECURE_CODE(2);

        companion object {
            fun fromInt(value: Int): EditCardField {
                return values().associateBy { it.value }[value] ?: CARD_NUMBER
            }
        }
    }
}
