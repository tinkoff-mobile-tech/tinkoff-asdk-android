package ru.tinkoff.acquiring.sdk.smartfield

import android.content.res.ColorStateList
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.cardscanners.CardScanner
import ru.tinkoff.acquiring.sdk.cardscanners.delegate.CardScannerWrapper
import ru.tinkoff.acquiring.sdk.cardscanners.delegate.isEnabled
import ru.tinkoff.acquiring.sdk.utils.SimpleTextWatcher
import ru.tinkoff.acquiring.sdk.utils.dpToPx

internal class BaubleClearOrScanButton {

    private lateinit var textFieldView: AcqTextFieldView
    private lateinit var clear: ImageView
    private lateinit var scan: ImageView
    private var cardScanner: CardScannerWrapper? = null

    fun attach(textFieldView: AcqTextFieldView, scanner: CardScannerWrapper?) {
        this.textFieldView = textFieldView
        this.cardScanner = scanner

        val context = textFieldView.context
        clear = ImageView(context).apply {
            setImageResource(R.drawable.acq_ic_clear)
            imageTintList = ColorStateList.valueOf(ResourcesCompat.getColor(
                context.resources, R.color.acq_colorTextTertiary, context.theme))
            layoutParams = ViewGroup.LayoutParams(context.dpToPx(16), context.dpToPx(16))
        }
        clear.setOnClickListener { textFieldView.text = "" }
        textFieldView.addRightBauble(clear)

        scan = ImageView(context).apply {
            setImageResource(R.drawable.acq_ic_card_frame)
            layoutParams = ViewGroup.LayoutParams(context.dpToPx(16), context.dpToPx(16))
        }
        scan.setOnClickListener { scanner?.start() }
        textFieldView.addRightBauble(scan)

        textFieldView.addViewFocusChangeListener { update() }
        textFieldView.editText.addTextChangedListener(SimpleTextWatcher.after { update() })

        update()
    }

    private fun update() {
        clear.isVisible = textFieldView.isEnabled
                && textFieldView.text.isNullOrBlank().not()
                && textFieldView.isViewFocused()
        scan.isVisible = textFieldView.isEnabled
                && cardScanner.isEnabled()
                && textFieldView.text.isNullOrBlank()
    }
}
