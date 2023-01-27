package ru.tinkoff.acquiring.sdk.redesign.dialog

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import ru.tinkoff.acquiring.sdk.R

interface OnPaymentSheetCloseListener {
    fun onClose(state: PaymentStatusSheetState)
}

fun <T> T.createPaymentSheetWrapper(): PaymentStatusSheet where T : FragmentActivity, T : OnPaymentSheetCloseListener {
    return PaymentStatusSheet()
}

fun <T> T.createPaymentSheetWrapper(): PaymentStatusSheet where T : Fragment, T : OnPaymentSheetCloseListener {
    return PaymentStatusSheet()
}

fun PaymentStatusSheet.showIfNeed(
    fragmentManager: FragmentManager,
    tag: String? = null
): PaymentStatusSheet {
    if (isAdded.not()) {
        show(fragmentManager, tag)
    }

    return this
}

sealed class PaymentStatusSheetState(
    open val title: Int?,
    open val subtitle: Int? = null,
    open val mainButton: Int? = null,
    open val secondButton: Int? = null
) {

    object NotYet : PaymentStatusSheetState(null)

    data class Progress(
        override val title: Int,
        override val subtitle: Int? = null,
        override val secondButton: Int? = null
    ) : PaymentStatusSheetState(title, subtitle, null, secondButton)

    class Error(
        title: Int, subtitle: Int? = null, mainButton: Int? = null,
        secondButton: Int? = null, val throwable: Throwable
    ) : PaymentStatusSheetState(title, subtitle, mainButton, secondButton)

    class Success(
        title: Int = R.string.acq_commonsheet_paid_title,
        subtitle: Int? = null,
        mainButton: Int? = R.string.acq_commonsheet_clear_primarybutton,
        val resultData: java.io.Serializable
    ) : PaymentStatusSheetState(title, subtitle, mainButton)

    object Hide : PaymentStatusSheetState(null)
}