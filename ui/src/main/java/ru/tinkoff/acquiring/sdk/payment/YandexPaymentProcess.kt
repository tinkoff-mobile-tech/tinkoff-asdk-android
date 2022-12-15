package ru.tinkoff.acquiring.sdk.payment

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.PaymentSource
import ru.tinkoff.acquiring.sdk.models.RejectedState
import ru.tinkoff.acquiring.sdk.models.ThreeDsState
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.YandexPay
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.payment.PaymentProcess.Companion.configure
import ru.tinkoff.acquiring.sdk.requests.InitRequest
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsAppBasedTransaction
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsDataCollector
import ru.tinkoff.acquiring.sdk.utils.getIpAddress

/**
 * Created by i.golovachev
 */
class YandexPaymentProcess(
    private val sdk: AcquiringSdk,
    private val context: Context,
    private val threeDsDataCollector: ThreeDsDataCollector,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Возвращает текущее состояние процесса оплаты
     */
    private val _state = MutableStateFlow<YandexPaymentState?>(null)
    val state = _state.asStateFlow()

    private val scope = CoroutineScope(
        ioDispatcher + CoroutineExceptionHandler { _, throwable -> handleException(throwable) }
    )

    private lateinit var paymentSource: YandexPay
    private var initRequest: InitRequest? = null
    private var email: String? = null

    private var paymentResult: PaymentResult? = null
    private var sdkState: AsdkState? = null
    private var error: Throwable? = null

    private var isChargeWasRejected = false
    private var rejectedPaymentId: Long? = null

    fun create(paymentOptions: PaymentOptions, yandexPayToken: String) {
        this.initRequest = sdk.init {
            configure(paymentOptions)
        }
        this.paymentSource = YandexPay(yandexPayToken)
    }

    /**
     * Запускает полный или подтверждающий процесс оплаты в зависимости от созданного процесса
     * @return сконфигурированный объект для проведения оплаты
     */
    suspend fun start() = scope.launch {
        sendToListener(YandexPaymentState.Started)
        delay(100000)
        callInitRequest(initRequest!!)
    }

    /**
     * Останавливает процесс оплаты
     */
    fun stop() {
        scope.cancel()
        sendToListener(YandexPaymentState.Stopped)
    }

    private  fun sendToListener(state: YandexPaymentState?) {
        this._state.update { state }
    }

    private fun handleException(throwable: Throwable) {
        if (throwable is AcquiringApiException && throwable.response != null &&
            throwable.response!!.errorCode == AcquiringApi.API_ERROR_CODE_3DSV2_NOT_SUPPORTED
        ) {
            sendToListener(YandexPaymentState.ThreeDsRejected)
        } else {
            error = throwable
            val paymentId = (_state.value as? YandexPaymentState.Registred)?.paymentId
            sendToListener(YandexPaymentState.Error(paymentId, throwable))
        }
    }

    private suspend fun callInitRequest(request: InitRequest) {
        if (isChargeWasRejected && rejectedPaymentId != null || sdkState is RejectedState) {
            request.data = modifyRejectedData(request)
        }

        val initResult = request.performSuspendRequest().getOrThrow()
        delay(1000000)
        callFinishAuthorizeRequest(
            initResult.paymentId!!, paymentSource, email,
            data = threeDsDataCollector.invoke(context,null)
        )
    }


    private fun modifyRejectedData(request: InitRequest): Map<String, String> {
        val map = HashMap<String, String>()
        map[AcquiringApi.RECURRING_TYPE_KEY] = AcquiringApi.RECURRING_TYPE_VALUE
        map[AcquiringApi.FAIL_MAPI_SESSION_ID] = rejectedPaymentId?.toString()
            ?: (sdkState as? RejectedState)?.rejectedPaymentId.toString()

        val data = request.data?.toMutableMap() ?: mutableMapOf()
        data.putAll(map)

        return data.toMap()
    }

    private suspend fun callFinishAuthorizeRequest(
        paymentId: Long,
        paymentSource: PaymentSource,
        email: String? = null,
        data: Map<String, String>? = null,
        threeDsVersion: String? = null,
        threeDsTransaction: ThreeDsAppBasedTransaction? = null
    ) {
        val ipAddress = if (data != null) getIpAddress() else null

        val finishRequest = sdk.finishAuthorize {
            this.paymentId = paymentId
            this.email = email
            this.paymentSource = paymentSource
            this.data = data
            ip = ipAddress
            sendEmail = email != null
        }

        val response = finishRequest.performSuspendRequest().getOrThrow()
        val threeDsData = response.getThreeDsData(threeDsVersion)

        if (threeDsData.isThreeDsNeed) {
            ThreeDsState(threeDsData, threeDsTransaction).also {
                sdkState = it
                sendToListener(YandexPaymentState.ThreeDsUiNeeded(it))
            }
        } else {
            paymentResult = PaymentResult(response.paymentId, null, response.rebillId)
            sendToListener(
                YandexPaymentState.Success(
                    response.paymentId!!,
                    null,
                    response.rebillId
                )
            )
        }
    }
}