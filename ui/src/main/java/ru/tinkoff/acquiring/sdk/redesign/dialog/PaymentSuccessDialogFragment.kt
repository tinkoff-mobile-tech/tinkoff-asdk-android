package ru.tinkoff.acquiring.sdk.redesign.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.models.result.AsdkResult
import ru.tinkoff.acquiring.sdk.ui.customview.LoaderButton
import ru.tinkoff.acquiring.sdk.utils.lazyView

internal class PaymentSuccessDialogFragment : BottomSheetDialogFragment() {

    private val buttonOk: LoaderButton by lazyView(R.id.acq_button_ok)

    var paymentSuccessClick: OnPaymentSuccessClick? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, theme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.acq_fragment_payment_success, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonOk.setOnClickListener { paymentSuccessClick?.invoke(this) }
    }


    fun interface OnPaymentSuccessClick {
        operator fun invoke(fragment: PaymentSuccessDialogFragment)
    }
}