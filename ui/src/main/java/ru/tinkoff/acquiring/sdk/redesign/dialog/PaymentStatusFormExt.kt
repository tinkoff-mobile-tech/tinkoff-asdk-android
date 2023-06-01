package ru.tinkoff.acquiring.sdk.redesign.dialog

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import ru.rtln.tds.sdk.log.Logger
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult

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
        fragmentManager.executePendingTransactions()
        try {
            show(fragmentManager, tag)
        } catch (e: IllegalStateException) {
            AcquiringSdk.log(e)
        }
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
        override val title: Int?,
        override val subtitle: Int? = null,
        override val secondButton: Int? = null
    ) : PaymentStatusSheetState(title, subtitle, null, secondButton)

    class Error(
        title: Int,
        subtitle: Int? = null,
        mainButton: Int? = null,
        secondButton: Int? = null,
        val throwable: Throwable
    ) : PaymentStatusSheetState(title, subtitle, mainButton, secondButton)

    class Success(
        title: Int = R.string.acq_commonsheet_paid_title,
        subtitle: Int? = null,
        mainButton: Int? = R.string.acq_commonsheet_clear_primarybutton,
        var paymentId: Long,
        var cardId: String? = null,
        var rebillId: String? = null
    ) : PaymentStatusSheetState(title, subtitle, mainButton)

    object Hide : PaymentStatusSheetState(null)
}

internal fun PaymentStatusSheetState.Success.getPaymentResult() =
    PaymentResult(paymentId, cardId, rebillId)
