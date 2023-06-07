package ru.tinkoff.acquiring.sdk.redesign.tpay.presentation

import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkTimeoutException
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.payment.MirPayPaymentState
import ru.tinkoff.acquiring.sdk.payment.TpayPaymentState
import ru.tinkoff.acquiring.sdk.redesign.dialog.PaymentStatusSheetState
import ru.tinkoff.acquiring.sdk.redesign.tpay.TpayLauncher
import ru.tinkoff.acquiring.sdk.redesign.tpay.nav.TpayNavigation

internal class TpayProcessMapper {

    fun mapState(it: TpayPaymentState): PaymentStatusSheetState? {
        return when (it) {
            is TpayPaymentState.PaymentFailed -> it.toPaymentStatusSheetState()
            is TpayPaymentState.Started,
            is TpayPaymentState.Created -> PaymentStatusSheetState.Progress(
                title = R.string.acq_commonsheet_payment_waiting_title,
                secondButton = R.string.acq_commonsheet_payment_waiting_flat_button
            )
            is TpayPaymentState.Success -> PaymentStatusSheetState.Success(
                title = R.string.acq_commonsheet_paid_title,
                mainButton = R.string.acq_commonsheet_clear_primarybutton,
                paymentId = it.paymentId,
                cardId = it.cardId,
                rebillId = it.rebillId
            )
            is TpayPaymentState.CheckingStatus -> PaymentStatusSheetState.Progress(
                title = R.string.acq_commonsheet_payment_waiting_title,
                secondButton = R.string.acq_commonsheet_payment_waiting_flat_button
            )
            is TpayPaymentState.LeaveOnBankApp -> PaymentStatusSheetState.Progress(
                title = R.string.acq_commonsheet_payment_waiting_title,
                secondButton = R.string.acq_commonsheet_payment_waiting_flat_button
            )
            is TpayPaymentState.Stopped,
            is TpayPaymentState.NeedChooseOnUi -> null
        }
    }

    private fun TpayPaymentState.PaymentFailed.toPaymentStatusSheetState(): PaymentStatusSheetState {
        return if (throwable is AcquiringSdkTimeoutException) {
            PaymentStatusSheetState.Error(
                title = R.string.acq_commonsheet_timeout_failed_title,
                subtitle = R.string.acq_commonsheet_timeout_failed_description,
                throwable = throwable,
                mainButton = R.string.acq_commonsheet_timeout_failed_flat_button
            )
        } else {
            PaymentStatusSheetState.Error(
                title = R.string.acq_commonsheet_payment_failed_title,
                subtitle = R.string.acq_commonsheet_payment_failed_description,
                throwable = throwable,
                mainButton = R.string.acq_commonsheet_payment_failed_primary_button
            )
        }
    }

    fun mapEvent(it: TpayPaymentState): TpayNavigation.Event? {
        return when (it) {
            is TpayPaymentState.NeedChooseOnUi -> TpayNavigation.Event.GoToTinkoff(it.deeplink)
            is TpayPaymentState.CheckingStatus,
            is TpayPaymentState.Created,
            is TpayPaymentState.LeaveOnBankApp,
            is TpayPaymentState.PaymentFailed,
            is TpayPaymentState.Started,
            is TpayPaymentState.Stopped,
            is TpayPaymentState.Success -> null
        }
    }

    fun mapResult(it: TpayPaymentState): TpayLauncher.Result? {
        return when (it) {
            is TpayPaymentState.LeaveOnBankApp,
            is TpayPaymentState.NeedChooseOnUi -> null
            is TpayPaymentState.Started,
            is TpayPaymentState.CheckingStatus,
            is TpayPaymentState.Created,
            is TpayPaymentState.Stopped -> TpayLauncher.Canceled
            is TpayPaymentState.PaymentFailed -> TpayLauncher.Error(it.throwable, null)
            is TpayPaymentState.Success -> TpayLauncher.Success(it.paymentId, null, null)
        }
    }
}
