package ru.tinkoff.acquiring.sdk.payment.base

import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions

sealed class AcqPaymentState(
    val paymentId: Long?
) {

    object Created : AcqPaymentState(null)

    class Started(
        val paymentOptions: PaymentOptions,
        val email: String? = null,
        paymentId: Long?  = null,
    )
}