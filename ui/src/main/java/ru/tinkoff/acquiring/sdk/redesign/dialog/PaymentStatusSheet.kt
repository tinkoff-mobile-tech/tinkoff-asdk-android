package ru.tinkoff.acquiring.sdk.redesign.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.LoaderButton

class PaymentStatusSheet internal constructor(): BottomSheetDialogFragment() {
    private lateinit var icon: ImageView
    private lateinit var progress: CircularProgressIndicator
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var mainButton: LoaderButton
    private lateinit var secondButton: LoaderButton

    @Suppress("UNCHECKED_CAST")
    private val onCloseListener: OnPaymentSheetCloseListener
        get() {
            val listener = (parentFragment as? OnPaymentSheetCloseListener)
                ?: (activity as? OnPaymentSheetCloseListener)
            return checkNotNull(listener) {
                "parent of fragment not implemented OnPaymentSheetCloseListener"
            }
        }

    var state: PaymentStatusSheetState? = null
        set(value) {
            field = value
            if (value != null && isResumed) {
                showState(value)
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(STYLE_NO_FRAME, R.style.BottomSheetDialog)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.acq_payment_status_form, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.AcqBottomSheetAnim
        icon = view.findViewById(R.id.acq_payment_status_form_icon)
        progress = view.findViewById(R.id.acq_payment_status_formm_progress)
        title = view.findViewById(R.id.acq_payment_status_form_title)
        subtitle = view.findViewById(R.id.acq_payment_status_form_subtitle)
        mainButton = view.findViewById(R.id.acq_payment_status_form_main_button)
        secondButton = view.findViewById(R.id.acq_payment_status_form_second_button)
        state?.let(::showState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        state?.let {
            onCloseListener.onClose(it)
        }
    }

    private fun set(
        icon: Int?,
        title: Int?,
        subtitle: Int?,
        mainButton: Int?,
        secondButton: Int?,
        progress: Boolean = icon == null,
        isCancelable: Boolean = progress.not() && secondButton == null
    ) {
        if (icon != null)
            this.icon.setImageResource(icon)

        this.icon.isVisible = icon != null

        if (title != null)
            this.title.setText(title)

        this.title.isVisible = title != null

        if (subtitle != null)
            this.subtitle.setText(subtitle)
        else
            this.subtitle.text = null

        this.subtitle.isVisible = title != null && subtitle != null

        if (mainButton != null)
            this.mainButton.text = getString(mainButton)

        this.mainButton.isVisible = mainButton != null

        if (secondButton != null)
            this.secondButton.text = getString(secondButton)

        this.secondButton.isVisible = secondButton != null

        this.progress.isVisible = progress

        this.isCancelable = isCancelable
    }

    private fun showState(state: PaymentStatusSheetState) {
        set(
            icon = defineIcon(state),
            title = state.title,
            subtitle = state.subtitle,
            mainButton = state.mainButton,
            secondButton = state.secondButton,
        )

        this.mainButton.setOnClickListener { onCloseListener.onClose(state) }
        this.secondButton.setOnClickListener { onCloseListener.onClose(state) }
    }

    private fun defineIcon(state: PaymentStatusSheetState) = when (state) {
        is PaymentStatusSheetState.Error -> R.drawable.acq_ic_cross_circle
        is PaymentStatusSheetState.NotYet -> null
        is PaymentStatusSheetState.Progress -> null
        is PaymentStatusSheetState.Hide -> null
        is PaymentStatusSheetState.Success -> R.drawable.acq_ic_check_circle_positive
    }
}
