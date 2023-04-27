package ru.tinkoff.acquiring.sdk.payment.pooling

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus

class GetStatusPolingTest {

    @Test
    fun `test when AUTHORIZED`() = runBlocking {
        GetStatusPooling { ResponseStatus.AUTHORIZED }
            .start(paymentId = 1L)
            .collect { println(it) }
    }

    @Test
    fun `test when REJECTED`() = runBlocking {
        val status = ResponseStatus.REJECTED
        GetStatusPooling { status }
            .start(paymentId = 1L)
            .catch {
                Assert.assertEquals(it.message, "PaymentState = $status")
            }
            .collect {}
    }

    @Test
    fun `test when non terimate status`() = runBlocking {
        GetStatusPooling { null }
            .start(paymentId = 1L, delayMs = 10)
            .catch {
                Assert.assertEquals(it.message, "timeout, retries count is over")
            }
            .collect { println(it) }
    }
}