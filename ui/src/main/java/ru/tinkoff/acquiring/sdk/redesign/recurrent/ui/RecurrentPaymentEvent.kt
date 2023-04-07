package ru.tinkoff.acquiring.sdk.redesign.recurrent.ui

import ru.tinkoff.acquiring.sdk.payment.PaymentByCardState

sealed interface RecurrentPaymentEvent {

    class CloseWithError(val throwable: Throwable, val paymentId: Long?) : RecurrentPaymentEvent {
        constructor(error: PaymentByCardState.Error) : this(error.throwable, error.paymentId)
    }

    class CloseWithCancel(val paymentId: Long? = null) : RecurrentPaymentEvent

    class CloseWithSuccess(val paymentId: Long, val rebillId: String) : RecurrentPaymentEvent {

        constructor(success: PaymentByCardState.Success) : this(paymentId = success.paymentId, rebillId = checkNotNull(success.rebillId))
    }
}