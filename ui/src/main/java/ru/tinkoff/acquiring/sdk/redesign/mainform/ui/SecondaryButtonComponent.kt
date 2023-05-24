package ru.tinkoff.acquiring.sdk.redesign.mainform.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.databinding.AcqMainFormSecondaryButtonBinding
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm
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
                paymentSubtitle.isVisible = false
                paymentTypeIcon.setImageDrawable(null)
            } else {
                paymentName.text = state.title
                paymentSubtitle.text = state.subtitle
                paymentSubtitle.isGone = state.subtitle.isNullOrBlank()
                paymentTypeIcon.setImageDrawable(state.icon)
            }
        }
    }

    fun subtitle(subtitle: String?) {
        binding.paymentSubtitle.text = subtitle
    }

    class State(
        val paymethod: Paymethod,
        val icon: Drawable,
        val title: String?,
        val subtitle: String?
    )
}

internal fun MainPaymentForm.Secondary.mapButtonState(context: Context) = when (this) {
    is MainPaymentForm.Secondary.Cards -> SecondaryButtonComponent.State(
        paymethod = Paymethod.Cards,
        icon = ContextCompat.getDrawable(context, R.drawable.acq_add_new_card)!!,
        title = context.resources.getString(R.string.acq_secondary_card_title),
        subtitle = null
    )
    MainPaymentForm.Secondary.Spb -> SecondaryButtonComponent.State(
        paymethod = Paymethod.SBP,
        icon = ContextCompat.getDrawable(context, R.drawable.acq_ic_secnod_sbp)!!,
        title = context.resources.getString(R.string.acq_secondary_sbp_title),
        subtitle = context.resources.getString(R.string.acq_secondary_sbp_subtitle)
    )
    MainPaymentForm.Secondary.Tpay -> SecondaryButtonComponent.State(
        paymethod = Paymethod.TinkoffPay,
        icon = ContextCompat.getDrawable(context, R.drawable.acq_icon_tinkoff_pay_alt)!!,
        title = context.resources.getString(R.string.acq_secondary_tinkoff_pay_title),
        subtitle = context.resources.getString(R.string.acq_secondary_tinkoff_pay_subtitle)
    )
    MainPaymentForm.Secondary.MirPay -> SecondaryButtonComponent.State(
        paymethod = Paymethod.MirPay,
        icon = ContextCompat.getDrawable(context, R.drawable.acq_ic_second_mir_pay)!!,
        title = context.resources.getString(R.string.acq_secondary_mir_pay_title),
        subtitle = context.resources.getString(R.string.acq_secondary_mir_pay_subtitle)
    )
    MainPaymentForm.Secondary.Yandex -> throw IllegalStateException("not supported")
}
