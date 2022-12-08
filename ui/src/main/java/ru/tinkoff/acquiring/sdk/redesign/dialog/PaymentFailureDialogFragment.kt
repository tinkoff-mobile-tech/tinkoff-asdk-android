package ru.tinkoff.acquiring.sdk.redesign.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.LoaderButton
import ru.tinkoff.acquiring.sdk.utils.lazyView

class PaymentFailureDialogFragment : BottomSheetDialogFragment() {

    private val buttonChooseAnotherMethod: LoaderButton
            by lazyView(R.id.acq_button_choose_another_method)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, theme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.acq_fragment_payment_failure, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonChooseAnotherMethod.setOnClickListener { onChooseAnotherMethod() }
    }

    private fun onChooseAnotherMethod() {
        ((parentFragment as? OnChooseAnotherMethod) ?: (activity as? OnChooseAnotherMethod))
            ?.onPaymentFailureChooseAnotherMethod(this)
    }

    fun interface OnChooseAnotherMethod {
        fun onPaymentFailureChooseAnotherMethod(fragment: PaymentFailureDialogFragment)
    }
}