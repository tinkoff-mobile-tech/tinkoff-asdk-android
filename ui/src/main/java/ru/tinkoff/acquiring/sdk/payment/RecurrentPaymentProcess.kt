package ru.tinkoff.acquiring.sdk.payment

import android.app.Application
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.AttachedCard
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.payment.methods.*
import ru.tinkoff.acquiring.sdk.responses.ChargeResponse
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsDataCollector
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by i.golovachev
 */
class RecurrentPaymentProcess internal constructor(
    private val initMethods: InitMethods,
    private val chargeMethods: ChargeMethods,
    private val check3DsVersionMethods: Check3DsVersionMethods,
    private val finishAuthorizeMethods: FinishAuthorizeMethods,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val coroutineManager: CoroutineManager = CoroutineManager() // TODO нужны только диспы отсюда
) {

    private val _state = MutableStateFlow<PaymentByCardState>(PaymentByCardState.Created)
    private val _paymentIdOrNull get() = (_state.value as? PaymentByCardState.Started)?.paymentId // TODO после слияния состояний
    private val rejectedPaymentId  = MutableStateFlow<String?>(null)
    val state = _state.asStateFlow()

    fun start(
        cardData: AttachedCard,
        paymentOptions: PaymentOptions,
        email: String? = null
    ) {
        scope.launch(coroutineManager.io) {
            try {
                startPaymentFlow(cardData, paymentOptions, email)
            } catch (e: Throwable) {
                handleException(paymentOptions, e, _paymentIdOrNull, true)
            }
        }
    }

    fun startWithCvc(
        cvc: String,
        rebillId: String,
        rejectedId: String,
        paymentOptions: PaymentOptions,
        email: String?
    ) {
        check(_state.value is PaymentByCardState.CvcUiNeeded)
        scope.launch(coroutineManager.io) {
            try {
                startRejectedFlow(cvc, rebillId, rejectedId, paymentOptions, email)
            } catch (e: Throwable) {
                handleException(paymentOptions, e, _paymentIdOrNull, false)
            }
        }
    }

    fun recreate() {
        _state.value = PaymentByCardState.Created
    }

    fun onThreeDsUiInProcess() {
        _state.value = PaymentByCardState.ThreeDsInProcess
    }

    internal fun set3dsResult(paymentResult: PaymentResult) {
        _state.value =
            PaymentByCardState.Success(
                paymentResult.paymentId ?: 0,
                paymentResult.cardId,
                paymentResult.rebillId
            )
    }

    fun set3dsResult(paymentId: Long? , cardId: String?, rebillId: String) {
        _state.value =
            PaymentByCardState.Success(
                paymentId ?: 0,
                cardId,
                rebillId
            )
    }

    fun set3dsResult(error: Throwable?) {
        _state.value =
            PaymentByCardState.Error(error ?: AcquiringSdkException(IllegalStateException()), null)
    }

    private suspend fun startPaymentFlow(
        cardData: AttachedCard,
        paymentOptions: PaymentOptions,
        email: String?
    ) {
        val init = initMethods.init(paymentOptions, email)
        val paymentId = checkNotNull(init.paymentId) { "paymentId must be not null" }
        _state.value = PaymentByCardState.Started(paymentOptions, email, paymentId)
        val charge = chargeMethods.charge(init.paymentId, cardData.rebillId)
        charge.emitSuccess(cardData.rebillId)
    }

    private suspend fun startRejectedFlow(
        cvc: String,
        rebillId: String,
        rejectedId: String,
        paymentOptions: PaymentOptions,
        email: String?
    ) {
        _state.value  = PaymentByCardState.CvcUiInProcess
        val card =
            AttachedCard(chargeMethods.getCardByRebillId(rebillId, paymentOptions).cardId, cvc)
        val init = chargeMethods.init(paymentOptions, email, rejectedId)
        val paymentId = checkNotNull(init.paymentId) { "paymentId must be not null" }
        _state.value = PaymentByCardState.Started(paymentOptions, email, paymentId)
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
                card.cardId,
                rebillId
            )
        }
    }

    private suspend fun handleException(
        paymentOptions: PaymentOptions,
        throwable: Throwable,
        paymentId: Long?,
        needCheckRejected: Boolean
    ) {
        if (throwable is CancellationException) return
        withContext(NonCancellable) {
            _state.emit(
                if (needCheckRejected && checkRejectError(throwable)) {
                    PaymentByCardState.CvcUiNeeded(paymentOptions, saveRejectedId())
                } else {
                    PaymentByCardState.Error(throwable, paymentId)
                }
            )
        }
    }

    private fun checkRejectError(it: Throwable): Boolean {
        return it is AcquiringApiException && it.response!!.errorCode == AcquiringApi.API_ERROR_CODE_CHARGE_REJECTED
    }

    private fun ChargeResponse.emitSuccess(rebillId: String?) {
        _state.value =
            PaymentByCardState.Success(
                paymentId = checkNotNull(paymentId) { "paymentId must be not null" },
                cardId = cardId,
                rebillId = checkNotNull(rebillId) { "rebillId must be not null" },
            )
    }

    private fun saveRejectedId(): String {
        val value = checkNotNull(_paymentIdOrNull?.toString())
        rejectedPaymentId.value = value
        return value
    }

    companion object {

        private var value: RecurrentPaymentProcess? = null

        @JvmStatic
        fun get() = value!!

        @JvmStatic
        @Synchronized
        fun init(
            sdk: AcquiringSdk,
            application: Application,
            threeDsDataCollector: ThreeDsDataCollector = ThreeDsHelper.CollectData
        ) {
            value = RecurrentPaymentProcess(
                InitMethodsSdkImpl(sdk),
                ChargeMethodsSdkImpl(sdk),
                Check3DsVersionMethodsSdkImpl(sdk, application, threeDsDataCollector),
                FinishAuthorizeMethodsSdkImpl(sdk),
                CoroutineScope(Job()),
                CoroutineManager(),
            )
        }
    }
}