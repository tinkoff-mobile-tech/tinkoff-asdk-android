package ru.tinkoff.acquiring.sdk.payment

import android.app.Application
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.models.PaymentSource
import ru.tinkoff.acquiring.sdk.models.ThreeDsState
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.models.paysources.CardSource
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.payment.PaymentProcess.Companion.configure
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsAppBasedTransaction
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsDataCollector
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager
import ru.tinkoff.acquiring.sdk.utils.getIpAddress

/**
 * Created by i.golovachev
 */
class PaymentByCardProcess internal constructor(
    private val sdk: AcquiringSdk,
    private val application: Application,
    private val threeDsDataCollector: ThreeDsDataCollector,
    private val coroutineManager: CoroutineManager = CoroutineManager()
) {

    private lateinit var paymentSource: CardData
    private val _state = MutableStateFlow<PaymentByCardState>(PaymentByCardState.Created)
    val state = _state.asStateFlow()

    fun start(
        cardData: CardData,
        paymentOptions: PaymentOptions,
        email: String? = null
    ) {
        _state.value = PaymentByCardState.Started(paymentOptions, email)
        coroutineManager.launchOnBackground {
            try {
                callInitRequest(cardData, paymentOptions, email)
            } catch (e: Throwable) {
                handleException(e)
            }
        }
    }

    fun stop() {
        coroutineManager.cancelAll()
    }

    private suspend fun callInitRequest(
        cardData: CardData,
        paymentOptions: PaymentOptions,
        email: String?
    ) {
        this.paymentSource = cardData
        val init = sdk.init {
            configure(paymentOptions)
            if (paymentOptions.features.duplicateEmailToReceipt && !email.isNullOrEmpty()) {
                receipt?.email = email
            }
        }.execute()

        callCheck3DsVersion(init.paymentId!!, cardData, paymentOptions, email)
    }

    private suspend fun callCheck3DsVersion(
        paymentId: Long,
        paymentSource: CardSource,
        paymentOptions: PaymentOptions,
        email: String? = null
    ) {

        val check3Ds = sdk.check3DsVersion {
            this.paymentId = paymentId
            this.paymentSource = paymentSource
        }.execute()

        val data = mutableMapOf<String, String>()
        if (check3Ds.serverTransId != null) {
            coroutineManager.withMain {
                data.putAll(threeDsDataCollector(application, check3Ds))
            }
        }
        val threeDsVersion = check3Ds.version
        var threeDsTransaction: ThreeDsAppBasedTransaction? = null

        if (ThreeDsHelper.isAppBasedFlow(threeDsVersion)) {
            try {
                coroutineManager.withMain {
                    threeDsTransaction = ThreeDsHelper.CreateAppBasedTransaction(
                        application, threeDsVersion!!, check3Ds.paymentSystem!!, data
                    )
                }
            } catch (e: Throwable) {
                handleException(e)
                return
            }
        }

        callFinishAuthorizeRequest(
            paymentId,
            paymentSource,
            paymentOptions,
            email,
            data,
            threeDsVersion,
            threeDsTransaction
        )
    }

    private suspend fun callFinishAuthorizeRequest(
        paymentId: Long,
        paymentSource: PaymentSource,
        paymentOptions: PaymentOptions,
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

        val response = finishRequest.execute()
        val threeDsData = response.getThreeDsData(threeDsVersion)

        _state.value = if (threeDsData.isThreeDsNeed) {
            PaymentByCardState.ThreeDsUiNeeded(
                ThreeDsState(threeDsData, threeDsTransaction),
                paymentOptions
            )
        } else {
            PaymentByCardState.Success(
                response.paymentId!!,
                null,
                response.rebillId
            )
        }
    }

    private fun handleException(throwable: Throwable) {
        if (throwable is AcquiringApiException && throwable.response != null &&
            throwable.response!!.errorCode == AcquiringApi.API_ERROR_CODE_3DSV2_NOT_SUPPORTED
        ) {
            // todo
        } else {
            _state.update { PaymentByCardState.Error(throwable, null) }
        }
    }

    companion object {

        private var value: PaymentByCardProcess? = null

        fun get() = value!!

        @Synchronized
        fun init(
            sdk: AcquiringSdk,
            application: Application,
            threeDsDataCollector: ThreeDsDataCollector = ThreeDsHelper.CollectData
        ) {
            value = PaymentByCardProcess(sdk, application, threeDsDataCollector)
        }
    }
}

sealed interface PaymentByCardState {
    object Created : PaymentByCardState

    class Started(
        val paymentOptions: PaymentOptions,
        val email: String? = null
    ) : PaymentByCardState

    class ThreeDsUiNeeded(val threeDsState: ThreeDsState, val paymentOptions: PaymentOptions) :
        PaymentByCardState

    class Success(val paymentId: Long, val cardId: String?, val rebillId: String?) :
        PaymentByCardState {
        internal val result = PaymentResult(paymentId, cardId, rebillId)
    }

    class Error(val throwable: Throwable, val paymentId: Long?) : PaymentByCardState
}