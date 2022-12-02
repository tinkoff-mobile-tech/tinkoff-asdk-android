package ru.tinkoff.acquiring.yandexpay

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.tinkoff.acquiring.sdk.requests.GetTerminalPayMethodsRequest
import ru.tinkoff.acquiring.sdk.responses.GetTerminalPayMethodsResponse
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.PaymethodData
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo
import ru.tinkoff.acquiring.yandexpay.models.mapYandexPayData

/**
 * Created by i.golovachev
 */
class GetTerminalPayMethodsTest {

    @Test
    fun `get terminal without yandex pay`() = runBlocking {
        val response = GetTerminalPayMethodsResponse(TerminalInfo())
        val requests = mock<GetTerminalPayMethodsRequest> {
            on { performRequestAsync(any()) } doReturn CompletableDeferred(
                Result.success(response)
            )
        }
        val getTerminalPayMethodsResponse =
            requests.performRequestAsync(GetTerminalPayMethodsResponse::class.java)

        Assert.assertNull(
            getTerminalPayMethodsResponse.await().getOrThrow().terminalInfo?.mapYandexPayData()
        )
    }

    @Test
    fun `get terminal pay with yandex pay`() = runBlocking {
        val response = GetTerminalPayMethodsResponse(
            TerminalInfo(
                paymethods = listOf(
                    PaymethodData(
                        paymethod = Paymethod.YandexPay,
                        params = mapOf(
                            "ShowcaseId" to "ShowcaseId",
                            "MerchantName" to "MerchantName",
                            "MerchantOrigin" to "MerchantOrigin",
                            "MerchantId" to "MerchantId",
                        )
                    )
                )
            )
        )
        val requests = mock<GetTerminalPayMethodsRequest> {
            on { performRequestAsync(any()) } doReturn CompletableDeferred(
                Result.success(response)
            )
        }
        val getTerminalPayMethodsResponse =
            requests.performRequestAsync(GetTerminalPayMethodsResponse::class.java)

        Assert.assertNotNull(
            getTerminalPayMethodsResponse.await().getOrNull()?.terminalInfo?.mapYandexPayData()
        )
    }

    @Test
    fun `get terminal pay with error`() = runBlocking {
        val requests = mock<GetTerminalPayMethodsRequest> {
            on { performRequestAsync(any()) } doReturn CompletableDeferred(
                Result.failure(InternalError())
            )
        }
        val getTerminalPayMethodsResponse =
            requests.performRequestAsync(GetTerminalPayMethodsResponse::class.java)

        Assert.assertNotNull(getTerminalPayMethodsResponse.await().exceptionOrNull())
    }
}