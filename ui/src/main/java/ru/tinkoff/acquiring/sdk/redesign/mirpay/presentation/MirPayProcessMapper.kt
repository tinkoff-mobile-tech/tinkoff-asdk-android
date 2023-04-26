package ru.tinkoff.acquiring.sdk.redesign.mirpay.presentation

import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkTimeoutException
import ru.tinkoff.acquiring.sdk.payment.MirPayPaymentState
import ru.tinkoff.acquiring.sdk.redesign.dialog.PaymentStatusSheetState
import ru.tinkoff.acquiring.sdk.redesign.mirpay.MirPayLauncher
import ru.tinkoff.acquiring.sdk.redesign.mirpay.nav.MirPayNavigation

internal class MirPayProcessMapper {

    fun mapState(it: MirPayPaymentState): PaymentStatusSheetState? {
        return when (it) {
            is MirPayPaymentState.PaymentFailed -> {
                val secondButton = (it.throwable as? AcquiringSdkTimeoutException)?.let {
                    R.string.acq_commonsheet_timeout_failed_flat_button
                } ?: R.string.acq_commonsheet_failed_primary_button
                PaymentStatusSheetState.Error(
                    title = R.string.acq_commonsheet_timeout_failed_title,
                    subtitle = R.string.acq_commonsheet_timeout_failed_description,
                    throwable = it.throwable,
                    secondButton = secondButton
                )
            }
            is MirPayPaymentState.Success -> PaymentStatusSheetState.Success(
                title = R.string.acq_commonsheet_paid_title,
                mainButton = R.string.acq_commonsheet_clear_primarybutton,
                paymentId = it.paymentId,
            )
            is MirPayPaymentState.Started,
            is MirPayPaymentState.CheckingStatus,
            is MirPayPaymentState.LeaveOnBankApp,
            is MirPayPaymentState.Created -> PaymentStatusSheetState.Progress(
                title = R.string.acq_commonsheet_payment_waiting_title,
                secondButton = R.string.acq_commonsheet_payment_waiting_flat_button
            )
            is MirPayPaymentState.Stopped,
            is MirPayPaymentState.NeedChooseOnUi -> null
        }
    }

    fun mapEvent(it: MirPayPaymentState): MirPayNavigation.Event? {
        return when (it) {
            is MirPayPaymentState.NeedChooseOnUi -> MirPayNavigation.Event.GoToMirPay(it.deeplink)
            is MirPayPaymentState.CheckingStatus,
            is MirPayPaymentState.Created,
            is MirPayPaymentState.LeaveOnBankApp,
            is MirPayPaymentState.PaymentFailed,
            is MirPayPaymentState.Started,
            is MirPayPaymentState.Stopped,
            is MirPayPaymentState.Success -> null
        }
    }

    fun mapResult(it: MirPayPaymentState): MirPayLauncher.Result? {
        return when (it) {
            is MirPayPaymentState.LeaveOnBankApp,
            is MirPayPaymentState.NeedChooseOnUi -> null
            is MirPayPaymentState.Started,
            is MirPayPaymentState.CheckingStatus,
            is MirPayPaymentState.Created,
            is MirPayPaymentState.Stopped -> MirPayLauncher.Canceled
            is MirPayPaymentState.PaymentFailed -> MirPayLauncher.Error(it.throwable, null)
            is MirPayPaymentState.Success -> MirPayLauncher.Success(it.paymentId, null, null)
        }
    }
}
