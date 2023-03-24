package ru.tinkoff.acquiring.sdk.payment.pooling

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus

class GetStatusPolingTest {

    @Test
    fun start() = runBlocking {
        GetStatusPoling { ResponseStatus.AUTHORIZED }
            .start(paymentId = 1L)
            .collect { println(it) }
    }

    @Test
    fun start2() = runBlocking {
        val status = ResponseStatus.REJECTED
        GetStatusPoling { status }
            .start(paymentId = 1L)
            .catch {
                Assert.assertEquals(it.message, "PaymentState = $status")
            }
            .collect {}
    }

    @Test
    fun star3() = runBlocking {
        GetStatusPoling { null }
            .start(paymentId = 1L)
            .catch {
                Assert.assertEquals(it.message, "timeout, retries count is over")
            }
            .collect { println(it) }
    }
}