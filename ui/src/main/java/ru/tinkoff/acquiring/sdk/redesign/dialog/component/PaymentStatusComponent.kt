package ru.tinkoff.acquiring.sdk.redesign.dialog.component

import androidx.core.view.isVisible
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.databinding.AcqPaymentStatusFormBinding
import ru.tinkoff.acquiring.sdk.redesign.dialog.PaymentStatusSheetState
import ru.tinkoff.acquiring.sdk.ui.component.UiComponent

/**
 * Created by i.golovachev
 */
internal class PaymentStatusComponent(
    val viewBinding: AcqPaymentStatusFormBinding,
    private val initialVisible: Boolean = false,
    private var onMainButtonClick: (PaymentStatusSheetState) -> Unit = {},
    private var onSecondButtonClick: (PaymentStatusSheetState) -> Unit = {},
) : UiComponent<PaymentStatusSheetState> {

    private val ctx = viewBinding.root.context
    private val icon = viewBinding.acqPaymentStatusFormIcon
    private val title = viewBinding.acqPaymentStatusFormTitle
    private val subtitle = viewBinding.acqPaymentStatusFormSubtitle
    private val progress = viewBinding.acqPaymentStatusFormmProgress
    private val mainButton = viewBinding.acqPaymentStatusFormMainButton
    private val secondButton = viewBinding.acqPaymentStatusFormSecondButton
    var isVisible: Boolean = initialVisible
        set(value) {
            field = value
            viewBinding.root.isVisible = value
        }

    override fun render(state: PaymentStatusSheetState) {
        set(
            icon = defineIcon(state),
            title = state.title,
            subtitle = state.subtitle,
            mainButton = state.mainButton,
            secondButton = state.secondButton,
        )

        this.mainButton.setOnClickListener { onMainButtonClick(state) }
        this.secondButton.setOnClickListener { onSecondButtonClick(state) }
    }

    private fun set(
        icon: Int?,
        title: Int?,
        subtitle: Int?,
        mainButton: Int?,
        secondButton: Int?,
        progress: Boolean = icon == null,
    ) {
        if (icon != null)
            this.icon.setImageResource(icon)

        this.icon.isVisible = icon != null

        if (title != null)
            this.title.setText(title)

        this.title.isVisible = title != null

        if (subtitle != null)
            this.subtitle.setText(subtitle)

        this.subtitle.isVisible = title != null

        if (mainButton != null)
            this.mainButton.text = getString(mainButton)

        this.mainButton.isVisible = mainButton != null

        if (secondButton != null)
            this.secondButton.text = getString(secondButton)

        this.secondButton.isVisible = secondButton != null

        this.progress.isVisible = progress
    }

    private fun getString(res: Int) = ctx.getString(res)
    private fun defineIcon(state: PaymentStatusSheetState) = when (state) {
        is PaymentStatusSheetState.Error -> R.drawable.acq_ic_cross_circle
        is PaymentStatusSheetState.NotYet -> null
        is PaymentStatusSheetState.Progress -> null
        is PaymentStatusSheetState.Hide -> null
        is PaymentStatusSheetState.Success -> R.drawable.acq_ic_check_circle_positive
    }
}