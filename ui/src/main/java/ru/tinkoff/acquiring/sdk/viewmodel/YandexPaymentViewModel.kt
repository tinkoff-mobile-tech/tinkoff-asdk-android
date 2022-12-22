package ru.tinkoff.acquiring.sdk.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.*
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.PaymentScreenState
import ru.tinkoff.acquiring.sdk.models.ThreeDsScreenState
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.payment.*

/**
 * Created by i.golovachev
 */
internal class YandexPaymentViewModel(
    application: Application,
    handleErrorsInSdk: Boolean,
    sdk: AcquiringSdk,
    private val paymentProcess: YandexPaymentProcess
) : BaseAcquiringViewModel(application, handleErrorsInSdk, sdk) {

    private val paymentResult: MutableLiveData<PaymentResult> = MutableLiveData()
    val paymentResultLiveData: LiveData<PaymentResult> = paymentResult

    init {
        paymentProcess.state.launchAndCollect()
    }

    fun startYandexPayPayment(paymentOptions: PaymentOptions, yandexPayToken: String) {
        changeScreenState(LoadingState)

        with(paymentProcess) {
            create(paymentOptions, yandexPayToken)
            start()
        }
    }

    fun onThreeDsReceiveResult(threeDsPaymentResult: PaymentResult) {
        paymentResult.value = threeDsPaymentResult
    }

    private fun StateFlow<YandexPaymentState?>.launchAndCollect() {
        viewModelScope.launch {
            buffer(0, BufferOverflow.DROP_OLDEST)
                .collectLatest {
                    when (it) {
                        is YandexPaymentState.ThreeDsUiNeeded -> {
                            changeScreenState(
                                ThreeDsScreenState(
                                    it.asdkState.data,
                                    it.asdkState.transaction
                                )
                            )
                            coroutine.runWithDelay(500) {
                                changeScreenState(LoadedState)
                            }
                        }
                        is YandexPaymentState.Success -> {
                            changeScreenState(LoadedState)
                            paymentResult.value =
                                PaymentResult(it.paymentId, it.cardId, it.rebillId)
                        }
                        is YandexPaymentState.Error -> {
                            changeScreenState(LoadedState)
                            handleException(it.throwable)
                        }
                        else -> Unit
                    }
                }
        }
    }
}