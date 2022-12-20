package ru.tinkoff.acquiring.yandexpay

import com.yandex.pay.core.data.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.requests.GetTerminalPayMethodsRequest
import ru.tinkoff.acquiring.sdk.responses.GetTerminalPayMethodsResponse
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.PaymethodData
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo
import ru.tinkoff.acquiring.sdk.utils.Money
import ru.tinkoff.acquiring.yandexpay.models.GatewayAcqId
import ru.tinkoff.acquiring.yandexpay.models.mapYandexPayData

/**
 * Created by i.golovachev
 */
class GetTerminalPayMethodsTest {

    @Test
    // #2347844
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
    // 2347847
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
    //#2355347
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

    @Test
    //#2355347
    fun `get terminal pay return data with yandex and more others`() = runBlocking {
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
                    ),
                    PaymethodData(
                        paymethod = Paymethod.TinkoffPay,
                        params = mapOf(
                            "version" to "2.0",
                        )
                    ),
                    PaymethodData(
                        paymethod = Paymethod.SBP,
                    )
                )
            )
        )
        val requests = mock<GetTerminalPayMethodsRequest> {
            on { performRequestAsync(any()) } doReturn CompletableDeferred(Result.success(response))
        }
        val getTerminalPayMethodsResponse =
            requests.performRequestAsync(GetTerminalPayMethodsResponse::class.java)

        Assert.assertNotNull(
            getTerminalPayMethodsResponse.await().getOrNull()?.terminalInfo?.mapYandexPayData()
        )
    }

    @Test
    //#2348017
    fun `When get terminal pay Then pass is in yandex data`() = runBlocking {
        val response = GetTerminalPayMethodsResponse(
            TerminalInfo(
                paymethods = listOf(
                    PaymethodData(
                        paymethod = Paymethod.YandexPay,
                        params = mapOf(
                            "ShowcaseId" to "15a919d7-c990-412c-b5eb-8d1ffe60e68a",
                            "MerchantName" to "Horns and hooves",
                            "MerchantOrigin" to "https://horns-and-hooves.ru",
                            "MerchantId" to "123456",
                        )
                    )
                )
            )
        )
        val requests = mock<GetTerminalPayMethodsRequest> {
            on { performRequestAsync(any()) } doReturn CompletableDeferred(Result.success(response))
        }
        val getTerminalPayMethodsResponse =
            requests.performRequestAsync(GetTerminalPayMethodsResponse::class.java)
        val data = checkNotNull(
            getTerminalPayMethodsResponse.await().getOrNull()?.terminalInfo?.mapYandexPayData()
        )
        val paymentOptions = PaymentOptions()

        paymentOptions.orderOptions {
            orderId = "orderId"
            amount = Money.ofCoins(1000)
            description = "test"
        }


        Assert.assertEquals(data.allowedAuthMethods, listOf(AuthMethod.PanOnly))
        Assert.assertEquals(data.type, PaymentMethodType.Card)
        Assert.assertEquals(data.gateway, Gateway.from(GatewayAcqId.tinkoff.name))
        Assert.assertEquals(
            data.allowedCardNetworks, listOf(
                CardNetwork.Visa,
                CardNetwork.MasterCard,
                CardNetwork.MIR
            )
        )
        Assert.assertEquals(data.gatewayMerchantIdYandex, GatewayMerchantID.from("123456"))
    }
}