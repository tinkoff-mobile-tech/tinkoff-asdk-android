package ru.tinkoff.acquiring.sdk.redesign.mainform.ui

import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.acq_main_form_primary_button_component.view.*
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.databinding.AcqMainFormPrimaryButtonComponentBinding
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm
import ru.tinkoff.acquiring.sdk.redesign.payment.model.CardChosenModel
import ru.tinkoff.acquiring.sdk.ui.component.UiComponent

/**
 * Created by i.golovachev
 */
internal class PrimaryButtonComponent(
    private val viewBinding: AcqMainFormPrimaryButtonComponentBinding,
    private val onTpayClick: () -> Unit = {},
    private val onMirPayClick: () -> Unit = {},
    private val onSpbClick: () -> Unit = {},
    private val onNewCardClick: () -> Unit = {},
    private val onPayClick: () -> Unit = {}
) : UiComponent<MainPaymentForm.Primary> {

    private val ctx = viewBinding.root.context
    private val primaryButtonContainer = viewBinding.primary
    private val textView: TextView = primaryButtonContainer.acqPrimaryButtonText
    private val imageView: ImageView = primaryButtonContainer.acqPrimaryButtonImage
    override fun render(state: MainPaymentForm.Primary) {
        val hasCard = checkChooseCard(state)

        when (state) {
            is MainPaymentForm.Primary.Card ->
                if (hasCard) {
                    setCardState(state.selectedCard!!)
                    setState(
                        bgColor = R.drawable.acq_button_yellow_bg,
                        textColor = R.color.acq_colorTinkoffPayText,
                        buttonText = "",
                        icon = null,
                        onClick = onPayClick
                    )
                } else {
                    setState(
                        bgColor = R.drawable.acq_button_yellow_bg,
                        textColor = R.color.acq_colorTinkoffPayText,
                        buttonText = ctx.getString(R.string.acq_primary_with_card),
                        icon = null,
                        onClick = onNewCardClick
                    )
                }
            is MainPaymentForm.Primary.Spb -> setState(
                bgColor = R.drawable.acq_button_spb_bg,
                textColor = R.color.acq_colorMain,
                buttonText = ctx.getString(R.string.acq_primary_with_sbp),
                icon = R.drawable.acq_ic_sbp_primary_button_logo,
                onClick = onSpbClick
            )
            is MainPaymentForm.Primary.Tpay -> setState(
                bgColor = R.drawable.acq_button_yellow_bg,
                textColor = R.color.acq_colorTinkoffPayText,
                buttonText = ctx.getString(R.string.acq_primary_with_tinkoff_pay),
                icon = R.drawable.acq_icon_tinkoff_pay,
                onClick = onTpayClick
            )
            is MainPaymentForm.Primary.MirPay -> setState(
                bgColor = R.drawable.acq_button_black_bg,
                textColor = R.color.acq_colorMirPayText,
                buttonText = ctx.getString(R.string.acq_primary_with_mir_pay),
                icon = R.drawable.acq_ic_wallet_mir_pay,
                onClick = onMirPayClick
            )
        }
    }

    private fun setState(
        bgColor: Int,
        textColor: Int,
        buttonText: String,
        icon: Int?,
        onClick: () -> Unit
    ) {
        textView.setTextColor(ContextCompat.getColor(viewBinding.root.context, textColor))
        textView.setText(buttonText)
        primaryButtonContainer.root.isVisible = true
        primaryButtonContainer.root.setBackgroundResource(bgColor)
        primaryButtonContainer.root.setOnClickListener { onClick() }

        imageView.isVisible = icon != null
        icon?.let(imageView::setImageResource)

    }

    private fun setCardState(card: CardChosenModel) {
        // todo navigations перевыбор
        primaryButtonContainer.root.isVisible = false
    }

    private fun checkChooseCard(primary: MainPaymentForm.Primary) =
        (primary as? MainPaymentForm.Primary.Card)?.selectedCard != null

    fun isVisible(isVisible: Boolean) {
        viewBinding.root.isVisible = isVisible
    }
}
