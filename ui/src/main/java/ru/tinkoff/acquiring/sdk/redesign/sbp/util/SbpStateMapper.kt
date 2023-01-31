package ru.tinkoff.acquiring.sdk.redesign.sbp.util

import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkTimeoutException
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.payment.SbpPaymentState
import ru.tinkoff.acquiring.sdk.redesign.dialog.PaymentSheetStatus
import ru.tinkoff.acquiring.sdk.redesign.sbp.ui.SpbBankListState

/**
 * Created by i.golovachev
 */
class SbpStateMapper {

    fun mapUiState(it: SbpPaymentState) = when (it) {
        is SbpPaymentState.GetBankListFailed -> SpbBankListState.Error(it.throwable)
        is SbpPaymentState.NeedChooseOnUi ->
            if (it.bankList.isEmpty()) {
                SpbBankListState.Empty
            } else {
                SpbBankListState.Content(it.bankList, it.deeplink)
            }
        else -> null
    }

    fun mapStatusForm(it: SbpPaymentState): PaymentSheetStatus? {
        return when (it) {
            is SbpPaymentState.LeaveOnBankApp -> {
                PaymentSheetStatus.Progress(
                    title = R.string.acq_commonsheet_payment_waiting_title,
                    secondButton = R.string.acq_commonsheet_payment_waiting_flat_button
                )
            }
            is SbpPaymentState.CheckingStatus -> {
                val status = it.status
                if (status == ResponseStatus.FORM_SHOWED) {
                    PaymentSheetStatus.Progress(
                        title = R.string.acq_commonsheet_payment_waiting_title,
                        secondButton = R.string.acq_commonsheet_payment_waiting_flat_button
                    )
                } else {
                    PaymentSheetStatus.Progress(
                        title = R.string.acq_commonsheet_processing_title,
                        subtitle = R.string.acq_commonsheet_processing_description
                    )
                }
            }
            is SbpPaymentState.Success ->
                PaymentSheetStatus.Success(paymentId = it.paymentId)
            is SbpPaymentState.PaymentFailed ->
                if (it.throwable is AcquiringSdkTimeoutException) {
                    PaymentSheetStatus.Error(
                        title = R.string.acq_commonsheet_timeout_failed_title,
                        subtitle = R.string.acq_commonsheet_timeout_failed_description,
                        throwable = it.throwable,
                        secondButton = R.string.acq_commonsheet_timeout_failed_flat_button
                    )
                } else {
                    PaymentSheetStatus.Error(
                        title = R.string.acq_commonsheet_failed_title,
                        subtitle = R.string.acq_commonsheet_failed_description,
                        throwable = it.throwable,
                        mainButton = R.string.acq_commonsheet_failed_primary_button
                    )
                }
            is SbpPaymentState.Stopped -> PaymentSheetStatus.Hide
            else -> null
        }
    }
}