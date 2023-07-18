package ru.tinkoff.acquiring.sdk.smartfield

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.widget.ImageView
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.redesign.common.carddatainput.CardNumberFormatter
import ru.tinkoff.acquiring.sdk.utils.SimpleTextWatcher
import ru.tinkoff.acquiring.sdk.viewmodel.CardLogoProvider

internal class BaubleCardLogo {

    private lateinit var textFieldView: AcqTextFieldView
    private lateinit var view: ImageView

    @SuppressLint("InflateParams")
    fun attach(textFieldView: AcqTextFieldView) {
        this.textFieldView = textFieldView

        val context = textFieldView.context
        view = LayoutInflater.from(context).inflate(R.layout.acq_item_card_logo, null) as ImageView

        textFieldView.addLeftBauble(view)
        textFieldView.editText.addTextChangedListener(SimpleTextWatcher.after { update() })

        update()
    }

    private fun update() {
        view.setImageResource(CardLogoProvider.getCardLogo(
            CardNumberFormatter.normalize(textFieldView.text)))
    }
}