package ru.tinkoff.acquiring.sdk.payment

import android.app.Application
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
import ru.tinkoff.acquiring.sdk.payment.methods.InitConfigurator.configure
import ru.tinkoff.acquiring.sdk.requests.InitRequest
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsAppBasedTransaction
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsDataCollector
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.utils.getIpAddress

/**
 * Created by i.golovachev
 */
class YandexPaymentProcess(
    private val sdk: AcquiringSdk,
    private val app: Application,
    private val threeDsDataCollector: ThreeDsDataCollector,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Возвращает текущее состояние процесса оплаты
     */
    private val _state = MutableStateFlow<YandexPaymentState?>(null)
    val state = _state.asStateFlow()

    private lateinit var scope: CoroutineScope

    private lateinit var paymentSource: YandexPay
    private var initRequest: InitRequest? = null
    private var email: String? = null
    private var paymentId: Long? = null

    private var paymentResult: PaymentResult? = null
    private var sdkState: AsdkState? = null
    private var error: Throwable? = null

    private var isChargeWasRejected = false
    private var rejectedPaymentId: Long? = null

    fun create(paymentOptions: PaymentOptions, yandexPayToken: String) {
        initScope()
        this.initRequest = sdk.init {
            configure(paymentOptions)
        }
        this.paymentSource = YandexPay(yandexPayToken)
    }

    fun create(paymentId: Long, yandexPayToken: String) {
        initScope()
        this.paymentSource = YandexPay(yandexPayToken)
        this.paymentId = paymentId
    }

    /**
     * Запускает полный или подтверждающий процесс оплаты в зависимости от созданного процесса
     * @return сконфигурированный объект для проведения оплаты
     */
    suspend fun start() = scope.launch {
        sendToListener(YandexPaymentState.Started)
        initRequest?.let { callInitRequest(it) } ?: callFinishAuthorizeRequest(
            paymentId!!, paymentSource, email,
            data = threeDsDataCollector.invoke(app.applicationContext, null)
        )
    }

    /**
     * Останавливает процесс оплаты
     */
    fun stop() {
        scope.coroutineContext.cancelChildren()
        sendToListener(YandexPaymentState.Stopped)
    }

    private fun initScope() {
        scope = CoroutineScope(
            ioDispatcher + CoroutineExceptionHandler { _, throwable -> handleException(throwable) }
        )
    }

    private fun sendToListener(state: YandexPaymentState?) {
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
        callFinishAuthorizeRequest(
            initResult.paymentId!!, paymentSource, email,
            data = threeDsDataCollector.invoke(app.applicationContext, null)
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

    /**
     * Рекомендуется сопоставлять жизненный цикл процесса с жизненным
     * циклом приложения, что бы процесс не прерывался при пересоздании экрана.
     */
    companion object {

        @Volatile
        private var _instance: YandexPaymentProcess? = null

        val instance: YandexPaymentProcess get() {
            return checkNotNull(_instance) {
                "YandexPaymentProcess is not initialize yet"
            }
        }

        fun init(
            sdk: AcquiringSdk,
            context: Application,
        ) {
            if (_instance == null) {
                synchronized(this) {
                    if (_instance == null) {
                        _instance = YandexPaymentProcess(sdk, context, ThreeDsHelper.CollectData)
                    }
                }
            }
        }
    }
}
