package ru.tinkoff.acquiring.sdk.payment.pooling

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkTimeoutException
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.utils.emitNotNull

class GetStatusPooling(private val getStatusMethod: GetStatusMethod) {

    constructor(asdk: AcquiringSdk) : this(GetStatusMethod.Impl(asdk))

    fun start(retriesCount: Int?, paymentId: Long) =
        start(retriesCount ?: POLLING_RETRIES_COUNT, paymentId)

    fun start(retriesCount: Int = POLLING_RETRIES_COUNT, paymentId: Long, delayMs: Long = POLLING_DELAY_MS ): Flow<ResponseStatus> {
        return flow {
            var tries = 0
            while (retriesCount > tries) {
                val status: ResponseStatus? = getStatusMethod(paymentId)
                emitNotNull(status)
                when (status) {
                    in ResponseStatus.successStatuses -> {
                        return@flow
                    }
                    ResponseStatus.REJECTED -> {
                        throw AcquiringSdkException(IllegalStateException("PaymentState = $status"), paymentId)
                    }
                    ResponseStatus.DEADLINE_EXPIRED -> {
                        throw AcquiringSdkTimeoutException(IllegalStateException("PaymentState = $status"), paymentId, status)
                    }
                    else -> {
                        tries += 1
                    }
                }
                delay(delayMs)
            }

            throw AcquiringSdkTimeoutException(IllegalStateException("timeout, retries count is over"), paymentId, null)
        }
    }

    companion object {
        private const val POLLING_DELAY_MS = 3000L
        private const val POLLING_RETRIES_COUNT = 10
    }
}
