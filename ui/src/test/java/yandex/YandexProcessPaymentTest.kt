package yandex

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito.times
import org.mockito.kotlin.verify
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.YandexPaymentState
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.responses.FinishAuthorizeResponse

/**
 * Created by i.golovachev
 */
class YandexProcessPaymentTest {

    private val processEnv = YandexPaymentProcessEnv(Dispatchers.Unconfined)

    @Test
    //#2354687
    fun `When Init complete Then FA called`() = processEnv.runWithEnv(
        given = {
            setInitResult(1L)
            setFAResult()
        },
        `when` = {
            process.create(PaymentOptions(), yandexToken!!)
            process.start().join()
        },
        then = {
            verify(processEnv.faRequestMock, times(1)).performSuspendRequest()
        }
    )

    @Test
    //#2354729
    fun `When FA complete and return paReq Then 3dsv1 redirected`() = processEnv.runWithEnv(
        given = {
            setInitResult(1L)
            setFAResult(
                FinishAuthorizeResponse(
                    paReq = "paReq",
                    md = "md",
                    paymentId = 1,
                    status = ResponseStatus.THREE_DS_CHECKING
                )
            )
        },
        `when` = {
            process.create(PaymentOptions(), yandexToken!!)
            process.start().join()
        },
        then = {
            val value = process.state.value
            val asdkState = (value as YandexPaymentState.ThreeDsUiNeeded).asdkState
            Assert.assertFalse(
                asdkState.data.is3DsVersion2
            )
        }
    )

    @Test
    //#2354750
    fun `When FA complete and return TdsServerTransId Then 3dsv2 redirected`() = processEnv.runWithEnv(
        given = {
            setInitResult(1L)
            setFAResult(
                FinishAuthorizeResponse(
                    tdsServerTransId = "tdsServerTransId",
                    acsTransId = "acsTransId",
                    paymentId = 1,
                    status = ResponseStatus.THREE_DS_CHECKING
                )
            )
        },
        `when` = {
            process.create(PaymentOptions(), yandexToken!!)
            process.start().join()
        },
        then = {
            val value = process.state.value
            val asdkState = (value as YandexPaymentState.ThreeDsUiNeeded).asdkState
            Assert.assertTrue(
                asdkState.data.is3DsVersion2
            )
        }
    )

    @Test
    //#2354714
    fun `When FA throw error Then give error state`() = processEnv.runWithEnv(
        given = {
            setInitResult()
            setFAResult(IllegalStateException())
        },
        `when` = {
            process.create(PaymentOptions(), yandexToken!!)
            process.start().join()
        },
        then = {
            val value = process.state.value
            Assert.assertTrue(
               value is YandexPaymentState.Error
            )
        }
    )

    @Test
    //#2354688
    fun `When Init throw error Then give error state`() = processEnv.runWithEnv(
        given = {
            setInitResult(IllegalStateException())
        },
        `when` = {
            process.create(PaymentOptions(), yandexToken!!)
            process.start().join()
        },
        then = {
            val value = process.state.value
            Assert.assertTrue(
                value is YandexPaymentState.Error
            )
        }
    )

    @Test
    //#2354715
    fun `When FA can pass without 3ds Then give success state`() = processEnv.runWithEnv(
        given = {
            setInitResult()
            setFAResult(
                FinishAuthorizeResponse(
                    paymentId = 1,
                    status = ResponseStatus.CONFIRMED
                )
            )
        },
        `when` = {
            process.create(PaymentOptions(), yandexToken!!)
            process.start().join()
        },
        then = {
            val value = process.state.value
            Assert.assertTrue(
                value is YandexPaymentState.Success
            )
        }
    )

    @Test
    //#2354715
    fun `When FA return AUTHORIZED Then give success state`() = processEnv.runWithEnv(
        given = {
            setInitResult()
            setFAResult(
                FinishAuthorizeResponse(
                    paymentId = 1,
                    status = ResponseStatus.AUTHORIZED
                )
            )
        },
        `when` = {
            process.create(PaymentOptions(), yandexToken!!)
            process.start().join()
        },
        then = {
            val value = process.state.value
            Assert.assertTrue(
                value is YandexPaymentState.Success
            )
        }
    )
}