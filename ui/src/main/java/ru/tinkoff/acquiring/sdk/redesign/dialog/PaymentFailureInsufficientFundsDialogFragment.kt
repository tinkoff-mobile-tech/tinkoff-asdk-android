package ru.tinkoff.acquiring.sdk.redesign.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.LoaderButton
import ru.tinkoff.acquiring.sdk.utils.lazyView

class PaymentFailureInsufficientFundsDialogFragment : BottomSheetDialogFragment() {

    private val buttonBackToPayment: LoaderButton by lazyView(R.id.acq_button_back_to_payment)
    private val buttonOk: LoaderButton by lazyView(R.id.acq_button_ok)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, theme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.acq_fragment_payment_failure_insufficient_funds, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonBackToPayment.setOnClickListener { onBackToPayment() }
        buttonOk.setOnClickListener { onOk() }
    }

    private fun onBackToPayment() {
        ((parentFragment as? OnBackToPayment) ?: (activity as? OnBackToPayment))
            ?.onPaymentFailureBackToPayment(this)
    }

    private fun onOk() {
        ((parentFragment as? OnOk) ?: (activity as? OnOk))?.onPaymentFailureOk(this)
    }

    fun interface OnBackToPayment {
        fun onPaymentFailureBackToPayment(fragment: PaymentFailureInsufficientFundsDialogFragment)
    }

    fun interface OnOk {
        fun onPaymentFailureOk(fragment: PaymentFailureInsufficientFundsDialogFragment)
    }
}