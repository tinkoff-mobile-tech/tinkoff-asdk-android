package ru.tinkoff.acquiring.sdk.redesign.mainform.ui

import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.databinding.AcqMainFormPrimaryButtonBinding
import ru.tinkoff.acquiring.sdk.databinding.AcqMainFormPrimaryButtonComponentBinding
import ru.tinkoff.acquiring.sdk.redesign.common.emailinput.EmailInputComponent
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormUi
import ru.tinkoff.acquiring.sdk.redesign.payment.model.CardChosenModel
import ru.tinkoff.acquiring.sdk.redesign.payment.ui.ChosenCardComponent
import ru.tinkoff.acquiring.sdk.ui.component.UiComponent

/**
 * Created by i.golovachev
 */
internal class PrimaryButtonComponent(
    private val viewBinding: AcqMainFormPrimaryButtonComponentBinding,
    private val email: String? = null,
    private val onCvcCompleted: (String) -> Unit = {},
    private val onEmailInput: (String) -> Unit = {},
    private val onEmailVisibleChange: (Boolean) -> Unit = {},
    private val onTpayClick: () -> Unit = { // TODO tpay flow
    },
    private val onSpbClick: () -> Unit = {},
    private val onNewCardClick: () -> Unit = {},
    private val onChooseCardClick: () -> Unit = {}
) : UiComponent<MainPaymentFormUi.Primary> {

    private val primaryButtonContainer = viewBinding.primary
    private val textView: TextView = primaryButtonContainer.acqPrimaryButtonText
    private val imageView: ImageView = primaryButtonContainer.acqPrimaryButtonImage
    private val emailInputComponent = EmailInputComponent(viewBinding.emailInput.root,
        onEmailChange = { onEmailInput(it) },
        onEmailVisibleChange = { onEmailVisibleChange(it) }
    ).apply {
        render(EmailInputComponent.State(email, email != null))
    }

    private val savedCardComponent = ChosenCardComponent(viewBinding.chosenCard.root,
        onCvcCompleted = { cvc, _ -> onCvcCompleted(cvc) },
        onChangeCard = { onChooseCardClick() }
    )

    override fun render(state: MainPaymentFormUi.Primary) {
        val hasCard = checkChooseCard(state)
        emailInputComponent.root.isVisible = hasCard
        savedCardComponent.root.isVisible = hasCard

        when (state) {
            is MainPaymentFormUi.Primary.Card ->
                if (hasCard) {
                    setCardState(state.selectedCard!!)
                    setState(
                        bgColor = R.drawable.acq_button_yellow_bg,
                        textColor = R.color.acq_colorTinkoffPayText,
                        buttonText = R.string.common_signin_button_text,
                        icon = null,
                        onClick = {}
                    )
                } else {
                    setState(
                        bgColor = R.drawable.acq_button_yellow_bg,
                        textColor = R.color.acq_colorTinkoffPayText,
                        buttonText = R.string.common_signin_button_text,
                        icon = null,
                        onClick = onNewCardClick
                    )
                }
            is MainPaymentFormUi.Primary.Spb -> setState(
                bgColor = R.drawable.acq_button_spb_bg,
                textColor = R.color.acq_colorMain,
                buttonText = R.string.common_signin_button_text,
                icon = R.drawable.acq_ic_sbp_primary_button_logo,
                onClick = onSpbClick
            )
            is MainPaymentFormUi.Primary.Tpay -> setState(
                bgColor = R.drawable.acq_button_yellow_bg,
                textColor = R.color.acq_colorTinkoffPayText,
                buttonText = R.string.common_signin_button_text,
                icon = R.drawable.acq_icon_tinkoff_pay,
                onClick = onTpayClick
            )
        }
    }

    fun renderEnable(buttonEnable: Boolean) {
        primaryButtonContainer.root.isEnabled = buttonEnable
    }

    private fun setState(
        bgColor: Int,
        textColor: Int,
        buttonText: Int,
        icon: Int?,
        onClick: () -> Unit
    ) {
        textView.setTextColor(ContextCompat.getColor(viewBinding.root.context, textColor))
        textView.setText(buttonText)

        primaryButtonContainer.root.setBackgroundResource(bgColor)
        primaryButtonContainer.root.setOnClickListener { onClick() }

        imageView.isVisible = icon != null
        icon?.let(imageView::setImageResource)

    }

    private fun setCardState(card: CardChosenModel) {
        savedCardComponent.render(card)
    }

    private fun checkChooseCard(primary: MainPaymentFormUi.Primary) =
        (primary as? MainPaymentFormUi.Primary.Card)?.selectedCard != null
}