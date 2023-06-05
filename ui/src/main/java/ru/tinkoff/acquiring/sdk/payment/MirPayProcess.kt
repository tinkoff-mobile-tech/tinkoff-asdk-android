package ru.tinkoff.acquiring.sdk.payment

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkTimeoutException
import ru.tinkoff.acquiring.sdk.exceptions.getErrorCodeIfApiError
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.methods.MirPayMethodsImpl
import ru.tinkoff.acquiring.sdk.payment.methods.MirPayMethods
import ru.tinkoff.acquiring.sdk.payment.pooling.GetStatusPooling

/**
 * @author k.shpakovskiy
 */
class MirPayProcess internal constructor(
    private val getStatusPooling: GetStatusPooling,
    private val linkMethods: MirPayMethods,
    private val scope: CoroutineScope
) {

    internal constructor(
        sdk: AcquiringSdk,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(
        GetStatusPooling(sdk),
        MirPayMethodsImpl(sdk),
        CoroutineScope(ioDispatcher)
    )

    val state = MutableStateFlow<MirPayPaymentState>(MirPayPaymentState.Created(null))
    private var looperJob: Job = Job()

    fun start(
        paymentOptions: PaymentOptions,
        paymentId: Long? = null,
    ) {
        scope.launch {
            runCatching { startFlow(paymentOptions, paymentId) }
                .onFailure { handlePaymentFlowFailure(it) }
        }
    }

    fun goingToBankApp() {
        when (val _state = state.value) {
            is MirPayPaymentState.Stopped,
            is MirPayPaymentState.NeedChooseOnUi -> {
                state.value = MirPayPaymentState.LeaveOnBankApp(_state.paymentId!!)
            }
            else -> Unit
        }
    }

    fun startCheckingStatus(retriesCount: Int? = null) {
        // выйдем из функции если стейт уже проверяется или вызов некорректен
        val _state = state.value
        if (_state is MirPayPaymentState.LeaveOnBankApp) {
            state.value = MirPayPaymentState.CheckingStatus(_state.paymentId, null)
            looperJob.cancel()
            looperJob = startLoping(retriesCount, paymentId = _state.paymentId)
        }
    }

    fun stop() {
        state.value = MirPayPaymentState.Stopped(state.value.paymentId)
        if (scope.isActive) {
            scope.coroutineContext.cancelChildren()
        }
    }

    private suspend fun startFlow(
        paymentOptions: PaymentOptions,
        paymentId: Long? = null
    ) {
        val _paymentId = paymentId ?: linkMethods.init(paymentOptions)
        state.value = MirPayPaymentState.Started(_paymentId)
        val link = linkMethods.getLink(_paymentId)
        state.value = MirPayPaymentState.NeedChooseOnUi(_paymentId, link)
    }

    private fun handlePaymentFlowFailure(ex: Throwable) {
        if (ex is CancellationException)
            return
        ex as Exception
        state.value = MirPayPaymentState.PaymentFailed(state.value.paymentId, ex, ex.getErrorCodeIfApiError())
    }

    private fun startLoping(retriesCount: Int?, paymentId: Long): Job {
        return scope.launch {
            getStatusPooling.start(
                retriesCount = retriesCount ?: POLLING_RETRIES_COUNT,
                paymentId = paymentId,
                delayMs = POLLING_DELAY_MS
            )
                .map { mapResponseStatusToState(status = it, paymentId = paymentId) }
                .catch {
                    emit(MirPayPaymentState.PaymentFailed(throwable = it, paymentId = paymentId))
                }
                .collectLatest { state.value = it }
        }
    }

    private fun mapResponseStatusToState(status: ResponseStatus, paymentId: Long) = when (status) {
        ResponseStatus.AUTHORIZED,
        ResponseStatus.CONFIRMED -> { MirPayPaymentState.Success(paymentId) }
        ResponseStatus.REJECTED -> {
            MirPayPaymentState.PaymentFailed(
                paymentId,
                AcquiringSdkException(IllegalStateException("PaymentState = $status"))
            )
        }
        ResponseStatus.DEADLINE_EXPIRED -> {
            MirPayPaymentState.PaymentFailed(
                paymentId,
                AcquiringSdkTimeoutException(IllegalStateException("PaymentState = $status"))
            )
        }
        else -> MirPayPaymentState.CheckingStatus(paymentId, status)
    }

    companion object {
        private const val POLLING_DELAY_MS = 5000L
        private const val POLLING_RETRIES_COUNT = 60
        private var instance: MirPayProcess? = null

        @Synchronized
        @JvmStatic
        internal fun init(sdk: AcquiringSdk) {
            instance?.scope?.cancel()
            instance = MirPayProcess(sdk)
        }

        @JvmStatic
        internal fun getRequired() = instance!!

        @JvmStatic
        fun get() = instance
    }
}

sealed interface MirPayPaymentState {
    val paymentId: Long?

    class Created(override val paymentId: Long? = null) : MirPayPaymentState
    class Started(override val paymentId: Long) : MirPayPaymentState
    class NeedChooseOnUi(override val paymentId: Long, val deeplink: String) : MirPayPaymentState
    class Success(override val paymentId: Long) : MirPayPaymentState
    class LeaveOnBankApp(override val paymentId: Long) : MirPayPaymentState
    class CheckingStatus(override val paymentId: Long, val status: ResponseStatus?) : MirPayPaymentState
    class PaymentFailed(
        override val paymentId: Long?,
        val throwable: Throwable,
        val errorCode: String? = null
    ) : MirPayPaymentState

    class Stopped(override val paymentId: Long?) : MirPayPaymentState
}
