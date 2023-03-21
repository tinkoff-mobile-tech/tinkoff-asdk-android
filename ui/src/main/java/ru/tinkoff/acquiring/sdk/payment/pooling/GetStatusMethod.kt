package ru.tinkoff.acquiring.sdk.payment.pooling

import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest

/**
 * Created by i.golovachev
 */
fun interface GetStatusMethod {
    suspend operator fun invoke(paymentId: Long): ResponseStatus?

    class Impl(private val acquiringSdk: AcquiringSdk) : GetStatusMethod {

        override suspend fun invoke(paymentId: Long): ResponseStatus? =
            // ignore errors
            acquiringSdk.getState { this.paymentId = paymentId }.performSuspendRequest()
                .getOrNull()?.status
    }
}