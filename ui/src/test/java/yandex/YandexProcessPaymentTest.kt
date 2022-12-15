package yandex

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito.times
import org.mockito.kotlin.verify
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest

/**
 * Created by i.golovachev
 */
class YandexProcessPaymentTest {

    private val processEnv = YandexPaymentProcessEnv(Dispatchers.Unconfined)

    @Test
    fun first() = runWithEnv(
        given = {
            //setInitResult(1L)
            setFAResult()
        },
        `when` = {
            process.create(PaymentOptions(), yandexToken!!)
            process.start().join()
        },
        then = {
            verify(faRequestMock, times(2))
        }
    )

    @Test
    fun seconsd() = runBlocking {
        processEnv.setInitResult(1L)
        processEnv.setFAResult()
        processEnv.process.create(PaymentOptions(), processEnv.yandexToken!!)
        val job = processEnv.process.start()
        job.join()
        verify(processEnv.faRequestMock, times(2)).performSuspendRequest()
        Unit
    }

    private fun runWithEnv(
        given: YandexPaymentProcessEnv.() -> Unit,
        `when`: suspend YandexPaymentProcessEnv.() -> Unit,
        then: YandexPaymentProcessEnv.() -> Unit
    ) {
        runBlocking{
            processEnv.apply(given)
            `when`.invoke(processEnv)
            processEnv.apply(then)
        }
    }
}