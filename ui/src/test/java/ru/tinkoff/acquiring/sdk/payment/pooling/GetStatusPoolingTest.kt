package ru.tinkoff.acquiring.sdk.payment.pooling

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus

class GetStatusPoolingTest {

    @Test
    fun start() = runBlocking {
        GetStatusPooling { ResponseStatus.AUTHORIZED }
            .start(paymentId = 1L)
            .collect { println(it) }
    }

    @Test
    fun start2() = runBlocking {
        val status = ResponseStatus.REJECTED
        GetStatusPooling { status }
            .start(paymentId = 1L)
            .catch {
                Assert.assertEquals(it.message, "PaymentState = $status")
            }
            .collect {}
    }

    @Test
    fun star3() = runBlocking {
        GetStatusPooling { null }
            .start(paymentId = 1L)
            .catch {
                Assert.assertEquals(it.message, "timeout, retries count is over")
            }
            .collect { println(it) }
    }
}