package ru.tinkoff.acquiring.sdk.redesign.tpay.presentation

import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkTimeoutException
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.payment.TpayPaymentState
import ru.tinkoff.acquiring.sdk.redesign.dialog.PaymentStatusSheetState
import ru.tinkoff.acquiring.sdk.redesign.tpay.Tpay
import ru.tinkoff.acquiring.sdk.redesign.tpay.nav.TpayNavigation

internal class TpayProcessMapper() {

    fun mapState(it: TpayPaymentState): PaymentStatusSheetState? {
        return when (it) {
            TpayPaymentState.Created -> PaymentStatusSheetState.Progress(
                title = R.string.acq_commonsheet_processing_title,
                subtitle = R.string.acq_commonsheet_processing_description
            )
            is TpayPaymentState.PaymentFailed -> if (it.throwable is AcquiringSdkTimeoutException) {
                PaymentStatusSheetState.Error(
                    title = R.string.acq_commonsheet_timeout_failed_title,
                    subtitle = R.string.acq_commonsheet_timeout_failed_description,
                    throwable = it.throwable,
                    secondButton = R.string.acq_commonsheet_timeout_failed_flat_button
                )
            } else {
                PaymentStatusSheetState.Error(
                    title = R.string.acq_commonsheet_failed_title,
                    subtitle = R.string.acq_commonsheet_failed_description,
                    throwable = it.throwable,
                    mainButton = R.string.acq_commonsheet_failed_primary_button
                )
            }
            is TpayPaymentState.Started -> PaymentStatusSheetState.Progress(
                title = R.string.acq_commonsheet_processing_title,
                subtitle = R.string.acq_commonsheet_processing_description
            )
            is TpayPaymentState.Success -> PaymentStatusSheetState.Success(
                title = R.string.acq_commonsheet_paid_title,
                mainButton = R.string.acq_commonsheet_clear_primarybutton,
                paymentId = it.paymentId,
                cardId = it.cardId,
                rebillId = it.rebillId
            )
            is TpayPaymentState.NeedChooseOnUi -> {
                null
            }
            is TpayPaymentState.CheckingStatus -> {
                val status = it.status
                if (status == ResponseStatus.FORM_SHOWED) {
                    PaymentStatusSheetState.Progress(
                        title = R.string.acq_commonsheet_payment_waiting_title,
                        secondButton = R.string.acq_commonsheet_payment_waiting_flat_button
                    )
                } else {
                    PaymentStatusSheetState.Progress(
                        title = R.string.acq_commonsheet_processing_title,
                        subtitle = R.string.acq_commonsheet_processing_description
                    )
                }
            }
            is TpayPaymentState.LeaveOnBankApp -> PaymentStatusSheetState.Progress(
                title = R.string.acq_commonsheet_payment_waiting_title,
                secondButton = R.string.acq_commonsheet_payment_waiting_flat_button
            )
            is TpayPaymentState.Stopped -> null
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

    fun mapResult(it: TpayPaymentState): Tpay.Result? {
        return when (it) {
            is TpayPaymentState.Started,
            is TpayPaymentState.LeaveOnBankApp,
            is TpayPaymentState.NeedChooseOnUi,
            is TpayPaymentState.CheckingStatus -> null
            is TpayPaymentState.Created,
            is TpayPaymentState.Stopped -> Tpay.Canceled
            is TpayPaymentState.PaymentFailed -> Tpay.Error(it.throwable, null)
            is TpayPaymentState.Success -> Tpay.Success(it.paymentId, null, null)
        }
    }
}