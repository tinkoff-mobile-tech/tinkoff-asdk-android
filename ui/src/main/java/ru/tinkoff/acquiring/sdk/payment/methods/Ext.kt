package ru.tinkoff.acquiring.sdk.payment.methods

import ru.tinkoff.acquiring.sdk.responses.InitResponse
import ru.tinkoff.acquiring.sdk.utils.checkNotNull

/**
 * @author k.shpakovskiy
 */
fun InitResponse.requiredPaymentId(): Long {
    return paymentId.checkNotNull { "paymentId must be not null" }
}
