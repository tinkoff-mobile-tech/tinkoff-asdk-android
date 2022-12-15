package yandex

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import org.mockito.kotlin.*
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.payment.YandexPaymentProcess
import ru.tinkoff.acquiring.sdk.requests.FinishAuthorizeRequest
import ru.tinkoff.acquiring.sdk.requests.InitRequest
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.responses.FinishAuthorizeResponse
import ru.tinkoff.acquiring.sdk.responses.InitResponse

class YandexPaymentProcessEnv(
    val ioDispatcher: CoroutineDispatcher,
    val yandexToken: String? = "yandexToken",
    val initRequestMock: InitRequest = mock(),
    val faRequestMock: FinishAuthorizeRequest = mock(),
    val sdk: AcquiringSdk = mock(),
    val context: Context = mock(),
    val process: YandexPaymentProcess = YandexPaymentProcess(sdk, context, ioDispatcher)
) {

    init {
        whenever(sdk.init(any())).doReturn(initRequestMock)
        whenever(sdk.finishAuthorize(any())).doReturn(faRequestMock)
    }

    fun shutdownMock() {
        reset(initRequestMock)
        reset(context)
    }

    suspend fun setInitResult(paymentId: Long) {
        val response = InitResponse(paymentId = paymentId)
        val result = Result.success(response)

        whenever(initRequestMock.performSuspendRequest())
            .doReturn(result)
    }

    fun setFAResult(response : FinishAuthorizeResponse = FinishAuthorizeResponse()) {
        val result = Result.success(response)

        whenever(faRequestMock.performRequestAsync(any()))
            .doReturn(CompletableDeferred(result))
    }
}