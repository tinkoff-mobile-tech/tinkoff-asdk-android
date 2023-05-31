package ru.tinkoff.acquiring.sdk.smartfield

import android.content.res.ColorStateList
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.utils.SimpleTextWatcher
import ru.tinkoff.acquiring.sdk.utils.dpToPx

internal class BaubleClearButton {

    private lateinit var textFieldView: AcqTextFieldView
    private lateinit var view: ImageView

    fun attach(textFieldView: AcqTextFieldView) {
        this.textFieldView = textFieldView
        
        val context = textFieldView.context
        view = ImageView(context).apply {
            setImageResource(R.drawable.acq_ic_clear)
            imageTintList = ColorStateList.valueOf(ResourcesCompat.getColor(
                context.resources, R.color.acq_colorTextTertiary, context.theme))
            layoutParams = ViewGroup.LayoutParams(context.dpToPx(16), context.dpToPx(16))
        }
        view.setOnClickListener { textFieldView.text = "" }

        textFieldView.addRightBauble(view)
        textFieldView.addViewFocusChangeListener { update() }
        textFieldView.editText.addTextChangedListener(SimpleTextWatcher.after { update() })

        update()
    }

    private fun update() {
        view.isVisible = textFieldView.isEnabled && textFieldView.isViewFocused()
    }
}
