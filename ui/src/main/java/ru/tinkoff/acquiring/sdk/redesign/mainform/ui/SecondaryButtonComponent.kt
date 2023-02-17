package ru.tinkoff.acquiring.sdk.redesign.mainform.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.databinding.AcqMainFormSecondaryButtonBinding
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormUi
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.ui.component.UiComponent

/**
 * Created by i.golovachev
 */
class SecondaryButtonComponent(val binding: AcqMainFormSecondaryButtonBinding) :
    UiComponent<SecondaryButtonComponent.State?> {

    override fun render(state: State?) {
        with(binding) {
            if (state == null) {
                paymentName.text = null
                paymentSubtitle.text = null
                paymentTypeIcon.setImageDrawable(null)
                paymentTypeIcon.background = null
            } else {
                paymentName.text = state.title
                paymentSubtitle.text = state.subtitle
                paymentTypeIcon.setImageDrawable(state.icon)
                paymentTypeIcon.background = state.iconBg
            }
        }
    }

    fun subtitle(subtitle: String?) {
        binding.paymentSubtitle.text = subtitle
    }

    class State(
        val paymethod: Paymethod,
        val icon: Drawable,
        val iconBg: Drawable?,
        val title: String?,
        val subtitle: String?
    )
}

fun MainPaymentFormUi.Secondary.mapButtonState(context: Context) = when (this) {
    is MainPaymentFormUi.Secondary.Cards -> SecondaryButtonComponent.State(
        paymethod = Paymethod.Cards,
        icon = ContextCompat.getDrawable(context, R.drawable.acq_add_new_card)!!,
        iconBg = null,
        title = context.resources.getString(R.string.acq_secondary_card_title),
        subtitle = context.resources.getQuantityString(
            R.plurals.acq_secondary_cards_subtitle,
            count,
            count
        )
    )
    MainPaymentFormUi.Secondary.Spb -> SecondaryButtonComponent.State(
        paymethod = Paymethod.Cards,
        icon = ContextCompat.getDrawable(context, R.drawable.acq_ic_secondary_sbp)!!,
        iconBg = ContextCompat.getDrawable(context, R.drawable.acq_shimmer_circle_bg)!!,
        title = context.resources.getString(R.string.acq_secondary_sbp_title),
        subtitle = context.resources.getString(R.string.acq_secondary_sbp_subtitle)
    )
    MainPaymentFormUi.Secondary.Tpay -> SecondaryButtonComponent.State(
        paymethod = Paymethod.Cards,
        icon = ContextCompat.getDrawable(context, R.drawable.acq_icon_tinkoff_pay)!!,
        iconBg = null,
        title = context.resources.getString(R.string.acq_secondary_tinkoff_pay_title),
        subtitle = context.resources.getString(R.string.acq_secondary_tinkoff_pay_subtitle)
    )
    MainPaymentFormUi.Secondary.Yandex -> throw IllegalStateException("not supported")
}

