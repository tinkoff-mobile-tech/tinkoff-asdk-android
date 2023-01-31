package ru.tinkoff.acquiring.sdk.redesign.dialog

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import ru.tinkoff.acquiring.sdk.R

interface OnPaymentSheetCloseListener {
    fun onClose(state: PaymentSheetStatus)
}

fun <T> T.createPaymentSheetWrapper(): PaymentStatusSheet where T : FragmentActivity, T : OnPaymentSheetCloseListener {
    return PaymentStatusSheet()
}

fun <T> T.createPaymentSheetWrapper(): PaymentStatusSheet where T : Fragment, T : OnPaymentSheetCloseListener {
    return PaymentStatusSheet()
}

sealed class PaymentSheetStatus(
    open val title: Int?,
    open val subtitle: Int? = null,
    open val mainButton: Int? = null,
    open val secondButton: Int? = null
) {

    object NotYet : PaymentSheetStatus(null)

    data class Progress(
        override val title: Int,
        override val subtitle: Int? = null,
        override val secondButton: Int? = null
    ) : PaymentSheetStatus(title, subtitle, null, secondButton)

    class Error(
        title: Int, subtitle: Int? = null, mainButton: Int? = null,
        secondButton: Int? = null, val throwable: Throwable
    ) : PaymentSheetStatus(title, subtitle, mainButton, secondButton)

    class Success(
        title: Int = R.string.acq_commonsheet_paid_title,
        subtitle: Int? = null,
        mainButton: Int? = R.string.acq_commonsheet_clear_primarybutton,
        val paymentId: Long
    ) : PaymentSheetStatus(title, subtitle, mainButton)

    object Hide : PaymentSheetStatus(null)
}