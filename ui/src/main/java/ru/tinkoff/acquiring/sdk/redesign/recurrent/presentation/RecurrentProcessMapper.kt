package ru.tinkoff.acquiring.sdk.redesign.recurrent.presentation

import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardState
import ru.tinkoff.acquiring.sdk.redesign.dialog.PaymentStatusSheetState
import ru.tinkoff.acquiring.sdk.redesign.recurrent.nav.RecurrentPaymentNavigation
import ru.tinkoff.acquiring.sdk.redesign.recurrent.ui.RecurrentPaymentEvent

internal class RecurrentProcessMapper(
    private val navigation: RecurrentPaymentNavigation.Impl
) { // PaymentStatusSheetState
    operator fun invoke(it: PaymentByCardState): PaymentStatusSheetState? {
        return when (it) {
            PaymentByCardState.Created -> PaymentStatusSheetState.Progress(null)
            is PaymentByCardState.Error -> PaymentStatusSheetState.Error(
                title = R.string.acq_commonsheet_failed_title,
                mainButton = R.string.acq_commonsheet_failed_primary_button,
                throwable = it.throwable
            )
            is PaymentByCardState.Started -> PaymentStatusSheetState.Progress(null)
            is PaymentByCardState.Success -> PaymentStatusSheetState.Success(
                title = R.string.acq_commonsheet_paid_title,
                mainButton = R.string.acq_commonsheet_clear_primarybutton,
                paymentId = it.paymentId,
                cardId = it.cardId,
                rebillId = it.rebillId
            )
            PaymentByCardState.ThreeDsInProcess -> {
                null
            }
            is PaymentByCardState.ThreeDsUiNeeded -> {
                navigation.eventChannel.trySend(
                    RecurrentPaymentEvent.To3ds(
                        it.paymentOptions,
                        it.threeDsState
                    )
                )
                null
            }
            is PaymentByCardState.CvcUiInProcess -> null
            is PaymentByCardState.CvcUiNeeded -> null
        }
    }
}