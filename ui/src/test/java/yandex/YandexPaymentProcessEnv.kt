package yandex

import android.app.Application
import android.content.Context
import kotlinx.coroutines.*
import org.mockito.kotlin.*
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.payment.YandexPaymentProcess
import ru.tinkoff.acquiring.sdk.requests.FinishAuthorizeRequest
import ru.tinkoff.acquiring.sdk.requests.InitRequest
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.responses.FinishAuthorizeResponse
import ru.tinkoff.acquiring.sdk.responses.InitResponse
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsDataCollector
import ru.tinkoff.acquiring.sdk.ui.activities.ThreeDsActivity

private val yandexPay3dsDataMap = mapOf(
    "threeDSCompInd" to "Y",
    "language" to "ru-RU",
    "timezone" to "",
    "screen_height" to "120",
    "screen_width" to "120",
    "cresCallbackUrl" to ThreeDsActivity.TERM_URL_V2
).toMutableMap()

class YandexPaymentProcessEnv(
    // const
    val yandexToken: String? = "yandexToken",
    val paymentId: Long = 1,
    val paReq: String = "paReq",
    val md: String = "md",
    val tdsServerTransId: String = "tdsServerTransId",
    val acsTransId : String= "acsTransId",

    // env
    val ioDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,

    // mocks
    val initRequestMock: InitRequest = mock(),
    val faRequestMock: FinishAuthorizeRequest = mock(),
    val sdk: AcquiringSdk = mock(),
    val context: Context = mock(),
    val application: Application = mock{
          on { applicationContext } doReturn  context
    },
    val threeDsDataCollector: ThreeDsDataCollector = mock { on { invoke(any(), any()) } doReturn  yandexPay3dsDataMap },
    val process: YandexPaymentProcess = YandexPaymentProcess(sdk, application, threeDsDataCollector, ioDispatcher)
) {

    init {
        whenever(sdk.init(any())).doReturn(initRequestMock)
        whenever(sdk.finishAuthorize(any())).doReturn(faRequestMock)
    }

    fun shutdownMock() {
        reset(initRequestMock)
        reset(context)
    }

    suspend fun setInitResult(response: InitResponse) {
        val result = Result.success(response)

        whenever(initRequestMock.performSuspendRequest())
            .doReturn(result)
    }

    suspend fun setInitResult(throwable: Throwable) {
        val result = Result.failure<InitResponse>(throwable)

        whenever(initRequestMock.performSuspendRequest())
            .doReturn(result)
    }

    suspend fun setInitResult(definePaymentId: Long? = null) {
        val response = InitResponse(paymentId = definePaymentId ?: paymentId)
        val result = Result.success(response)

        whenever(initRequestMock.performSuspendRequest())
            .doReturn(result)
    }

    suspend fun setFAResult(response : FinishAuthorizeResponse = FinishAuthorizeResponse()) {
        val result = Result.success(response)

        whenever(faRequestMock.performSuspendRequest())
            .doReturn(result)
    }

    suspend fun setFAResult(throwable: Throwable) {
        val result = Result.failure<FinishAuthorizeResponse>(throwable)

        whenever(faRequestMock.performSuspendRequest())
            .doReturn(result)
    }
}

internal fun YandexPaymentProcessEnv.runWithEnv(
    given: suspend YandexPaymentProcessEnv.() -> Unit,
    `when`: suspend YandexPaymentProcessEnv.() -> Unit,
    then: suspend YandexPaymentProcessEnv.() -> Unit
) {
    runBlocking {
        launch { given.invoke(this@runWithEnv) }.join()
        launch { `when`.invoke(this@runWithEnv) }.join()
        launch { then.invoke(this@runWithEnv) }.join()
    }
}