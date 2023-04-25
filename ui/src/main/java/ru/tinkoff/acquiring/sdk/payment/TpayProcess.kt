package ru.tinkoff.acquiring.sdk.payment

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkTimeoutException
import ru.tinkoff.acquiring.sdk.exceptions.getErrorCodeIfApiError
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.methods.GetTpayLinkMethodsSdkImpl
import ru.tinkoff.acquiring.sdk.payment.methods.TpayMethods
import ru.tinkoff.acquiring.sdk.payment.pooling.GetStatusPooling
import kotlin.coroutines.CoroutineContext

/**
 * Created by i.golovachev
 */
class TpayProcess internal constructor(
    private val getStatusPooling: GetStatusPooling,
    private val getTpayLinkMethods: TpayMethods,
    private val scope: CoroutineScope
) {

    internal constructor(
        sdk: AcquiringSdk,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(
        GetStatusPooling(sdk),
        GetTpayLinkMethodsSdkImpl(sdk),
        CoroutineScope(ioDispatcher)
    )

    val state = MutableStateFlow<TpayPaymentState>(TpayPaymentState.Created)
    private var looperJob: Job = Job()

    fun start(
        paymentOptions: PaymentOptions,
        tpayVersion: String,
        paymentId: Long? = null,
    ) {
        scope.launch {
            try {
                startFlow(paymentOptions, tpayVersion, paymentId)
            } catch (ignored: CancellationException) {
                throw ignored
            } catch (e: Exception) {
                state.value = TpayPaymentState.PaymentFailed(state.value.paymentId, e, e.getErrorCodeIfApiError())
            }
        }
    }

    fun goingToBankApp() {
        when (val _state = state.value) {
            is TpayPaymentState.NeedChooseOnUi,
            is TpayPaymentState.Stopped -> {
                state.value = TpayPaymentState.LeaveOnBankApp(_state.paymentId!!)
            }
            else -> Unit
        }
    }

    fun startCheckingStatus(retriesCount: Int? = null) {
        // выйдем из функции если стейт уже проверяется или вызов некорректен
        val _state = state.value
        if (_state is TpayPaymentState.LeaveOnBankApp) {
            state.value = TpayPaymentState.CheckingStatus(_state.paymentId, null)
            looperJob.cancel()
            looperJob = startLoping(retriesCount, paymentId = _state.paymentId)
        }
    }

    fun stop() {
        state.value = TpayPaymentState.Stopped(state.value.paymentId)
        if (scope.isActive) {
            scope.coroutineContext.cancelChildren()
        }
    }

    private suspend fun startFlow(
        paymentOptions: PaymentOptions,
        tpayVersion: String,
        paymentId: Long? = null,
    ) {
        val _paymentId = paymentId ?: init(paymentOptions)
        state.value = TpayPaymentState.Started(_paymentId)
        val link = getLink(_paymentId, tpayVersion)
        state.value = TpayPaymentState.NeedChooseOnUi(_paymentId, link)
    }

    private suspend fun init(paymentOptions: PaymentOptions): Long {
        return getTpayLinkMethods.init(paymentOptions = paymentOptions)
    }

    private suspend fun getLink(paymentId: Long, tpayVersion: String): String {
        return getTpayLinkMethods.tinkoffPayLink(paymentId, tpayVersion)
    }

    private fun startLoping(retriesCount: Int?, paymentId: Long): Job {
        return scope.launch {
            getStatusPooling.start(retriesCount = retriesCount, paymentId = paymentId)
                .map {
                    TpayPaymentState.mapResponseStatusToState(
                        status = it,
                        paymentId = paymentId
                    )
                }
                .catch {
                    emit(TpayPaymentState.PaymentFailed(throwable = it, paymentId = paymentId))
                }
                .collectLatest {
                    state.value = it
                }
        }
    }

    companion object {
        private var instance: TpayProcess? = null

        @Synchronized
        @JvmStatic
        internal fun init(sdk: AcquiringSdk) {
            instance?.scope?.cancel()
            instance = TpayProcess(sdk)
        }

        @JvmStatic
        internal fun getRequired() = instance!!

        @JvmStatic
        fun get() = instance
    }
}

sealed interface TpayPaymentState {
    val paymentId: Long?

    object Created : TpayPaymentState {
        override val paymentId: Long? = null
    }

    class Started(override val paymentId: Long) : TpayPaymentState

    class NeedChooseOnUi(
        override val paymentId: Long,
        val deeplink: String
    ) : TpayPaymentState

    class LeaveOnBankApp(override val paymentId: Long) : TpayPaymentState

    class CheckingStatus(
        override val paymentId: Long,
        val status: ResponseStatus?
    ) : TpayPaymentState

    class PaymentFailed(
        override val paymentId: Long?,
        val throwable: Throwable,
        val errorCode: String? = null
    ) : TpayPaymentState

    class Success(override val paymentId: Long, val cardId: String?, val rebillId: String?) :
        TpayPaymentState

    class Stopped(override val paymentId: Long?) : TpayPaymentState

    companion object {

        fun mapResponseStatusToState(status: ResponseStatus, paymentId: Long) = when (status) {
            ResponseStatus.AUTHORIZED, ResponseStatus.CONFIRMED -> {
                Success(
                    paymentId, null, null
                )
            }
            // по идее, в эти статусы проверка не зайдет - они обрабатываются в getStatusPooling
            ResponseStatus.REJECTED -> {
                PaymentFailed(
                    paymentId,
                    AcquiringSdkException(IllegalStateException("PaymentState = $status"))
                )
            }
            ResponseStatus.DEADLINE_EXPIRED -> {
                PaymentFailed(
                    paymentId,
                    AcquiringSdkTimeoutException(IllegalStateException("PaymentState = $status"))
                )
            }
            else -> CheckingStatus(paymentId, status)
        }
    }
}
