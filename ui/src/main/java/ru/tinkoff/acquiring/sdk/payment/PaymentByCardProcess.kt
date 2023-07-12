package ru.tinkoff.acquiring.sdk.payment

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.models.ThreeDsState
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.CardSource
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.payment.methods.*
import ru.tinkoff.acquiring.sdk.payment.methods.Check3DsVersionMethodsSdkImpl
import ru.tinkoff.acquiring.sdk.payment.methods.FinishAuthorizeMethodsSdkImpl
import ru.tinkoff.acquiring.sdk.payment.methods.InitMethodsSdkImpl
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsDataCollector
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager
import ru.tinkoff.acquiring.sdk.utils.checkNotNull

/**
 * Created by i.golovachev
 */
class PaymentByCardProcess internal constructor(
    private val initMethods: InitMethods,
    private val check3DsVersionMethods: Check3DsVersionMethods,
    private val finishAuthorizeMethods: FinishAuthorizeMethods,
    private val coroutineManager: CoroutineManager = CoroutineManager()
) {

    private lateinit var paymentSource: CardSource
    private val _state = MutableStateFlow<PaymentByCardState>(PaymentByCardState.Created)
    val state = _state

    fun start(
        cardData: CardSource,
        paymentOptions: PaymentOptions,
        email: String? = null
    ) {
        _state.value = PaymentByCardState.Started(paymentOptions, email)
        coroutineManager.launchOnBackground {
            try {
                startFlow(cardData, paymentOptions, email)
            } catch (e: Throwable) {
                handleException(e)
            }
        }
    }

    fun goTo3ds() {
        _state.value = PaymentByCardState.ThreeDsInProcess
    }

    fun stop() {
        coroutineManager.cancelAll()
    }

    internal fun set3dsResult(paymentResult: PaymentResult) {
        _state.value =
            PaymentByCardState.Success(
                paymentResult.paymentId ?: 0,
                paymentResult.cardId,
                paymentResult.rebillId
            )
    }

    fun set3dsResult(error: Throwable?) {
        _state.value =
            PaymentByCardState.Error(error ?: AcquiringSdkException(IllegalStateException()), null)
    }

    fun recreate() {
        _state.value = PaymentByCardState.Created
    }

    private suspend fun startFlow(
        card: CardSource,
        paymentOptions: PaymentOptions,
        email: String?,
    ) {
        this.paymentSource = card
        val paymentId = initMethods
                .init(paymentOptions, email)
                .paymentId
                .checkNotNull { "paymentId must be not null" }

        val data3ds = check3DsVersionMethods.callCheck3DsVersion(
            paymentId, card, paymentOptions, email
        )
        val finish = finishAuthorizeMethods.finish(
            paymentId,
            card,
            paymentOptions,
            email,
            data3ds.additionalData,
            data3ds.threeDsVersion,
            data3ds.threeDsTransaction
        )
        _state.value = when (finish) {
            is FinishAuthorizeMethods.Result.Need3ds -> PaymentByCardState.ThreeDsUiNeeded(
                finish.threeDsState,
                paymentOptions
            )
            is FinishAuthorizeMethods.Result.Success -> PaymentByCardState.Success(
                finish.paymentId,
                finish.cardId,
                finish.rebillId
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
            value = PaymentByCardProcess(
                InitMethodsSdkImpl(sdk),
                Check3DsVersionMethodsSdkImpl(sdk, application, threeDsDataCollector),
                FinishAuthorizeMethodsSdkImpl(sdk),
                CoroutineManager(),
            )
        }
    }
}

// кажется, проще будет привести все состояния к одному типу.
sealed interface PaymentByCardState {
    object Created : PaymentByCardState

    class Started(
        val paymentOptions: PaymentOptions,
        val email: String? = null,
        val paymentId: Long? = null
    ) : PaymentByCardState

    class ThreeDsUiNeeded(val threeDsState: ThreeDsState, val paymentOptions: PaymentOptions) :
        PaymentByCardState

    object ThreeDsInProcess : PaymentByCardState

    class CvcUiNeeded(val paymentOptions: PaymentOptions, val rejectedPaymentId: String) : PaymentByCardState

    object CvcUiInProcess : PaymentByCardState

    class Success(val paymentId: Long, val cardId: String?, val rebillId: String?) :
        PaymentByCardState {
        internal val result = PaymentResult(paymentId, cardId, rebillId)
    }

    class Error(val throwable: Throwable, val paymentId: Long?) : PaymentByCardState
}
