package ru.tinkoff.acquiring.sdk.redesign.payment.ui

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.redesign.common.carddatainput.CvcComponent
import ru.tinkoff.acquiring.sdk.redesign.payment.model.CardChosenModel
import ru.tinkoff.acquiring.sdk.ui.component.UiComponent
import ru.tinkoff.acquiring.sdk.viewmodel.CardLogoProvider


/**
 * Created by i.golovachev
 */
internal class ChosenCardComponent(
    val root: ViewGroup,
    private val onChangeCard: (CardChosenModel) -> Unit = {},
    private val onCvcCompleted: (String, Boolean) -> Unit = { _, _ -> }
) : UiComponent<CardChosenModel> {

    private val cardLogo: ImageView = root.findViewById(R.id.acq_card_choosen_item_logo)
    private val cardName: TextView = root.findViewById(R.id.acq_card_choosen_item)
    private val cardChange: TextView = root.findViewById(R.id.acq_card_change)
    private val cardCvc: CvcComponent = CvcComponent(
        root.findViewById(R.id.cvc_container), onDataChange = { b, s ->
            onCvcCompleted(s, b)
        }
    )
    override fun render(state: CardChosenModel) = with(state) {
        cardLogo.setImageResource(CardLogoProvider.getCardLogo(pan))
        cardName.text = root.context.getString(
            R.string.acq_cardlist_bankname, bankName.orEmpty(), tail
        )
        root.setOnClickListener { onChangeCard(state) }
    }

    fun renderCvcOnly(state: CardChosenModel)  = with(state) {
        cardLogo.setImageResource(CardLogoProvider.getCardLogo(pan))
        cardName.text = root.context.getString(
            R.string.acq_cardlist_bankname, bankName.orEmpty(), tail
        )
        cardChange.isVisible = false
        root.setOnClickListener {  }
    }

    fun clearCvc() {
        cardCvc.render(null)
    }

    fun enableCvc(isEnable: Boolean) {
        cardCvc.enable(isEnable)
    }

    fun showKeyboard() {

    }
}