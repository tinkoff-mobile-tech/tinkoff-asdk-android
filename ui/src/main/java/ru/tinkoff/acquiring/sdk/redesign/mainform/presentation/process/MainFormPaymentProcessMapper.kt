package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.process

import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardState
import ru.tinkoff.acquiring.sdk.redesign.dialog.PaymentStatusSheetState
import ru.tinkoff.acquiring.sdk.redesign.mainform.navigation.MainFormNavController

/**
 * Created by i.golovachev
 */
internal class MainFormPaymentProcessMapper(private val mainFormNavController: MainFormNavController) { // PaymentStatusSheetState
    suspend operator fun invoke(it: PaymentByCardState): PaymentStatusSheetState? {
        return when (it) {
            PaymentByCardState.Created -> null
            is PaymentByCardState.Error -> PaymentStatusSheetState.Error(
                title = R.string.acq_commonsheet_failed_title,
                mainButton = R.string.acq_commonsheet_failed_primary_button,
                throwable = it.throwable
            )
            is PaymentByCardState.Started -> null
            is PaymentByCardState.Success -> PaymentStatusSheetState.Success(
                title = R.string.acq_commonsheet_paid_title,
                mainButton = R.string.acq_commonsheet_clear_primarybutton,
                paymentId = it.paymentId,
                cardId = it.cardId,
                rebillId = it.rebillId
            )
            PaymentByCardState.ThreeDsInProcess -> {
                mainFormNavController.clear()
                null
            }
            is PaymentByCardState.ThreeDsUiNeeded -> {
                mainFormNavController.to3ds(it.paymentOptions, it.threeDsState)
                null
            }
            is PaymentByCardState.CvcUiNeeded -> null
            is PaymentByCardState.CvcUiInProcess -> null
        }
    }
}