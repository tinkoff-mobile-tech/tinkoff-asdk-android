package ru.tinkoff.acquiring.sdk.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.LoadedState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.SingleEvent
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.GooglePay
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.payment.PaymentListenerAdapter
import ru.tinkoff.acquiring.sdk.payment.PaymentProcess

/**
 * @author Mariya Chernyadieva
 */
internal class NotificationPaymentViewModel(
    application: Application,
    handleErrorsInSdk: Boolean,
    sdk: AcquiringSdk
) : BaseAcquiringViewModel(application, handleErrorsInSdk, sdk) {

    private var paymentProcess: PaymentProcess? = null

    private val paymentResult: MutableLiveData<PaymentResult> = MutableLiveData()
    val paymentResultLiveData: LiveData<PaymentResult> = paymentResult

    private val errorData: MutableLiveData<Throwable> = MutableLiveData()
    val errorLiveData: LiveData<Throwable> = errorData

    private val uiEvent: MutableLiveData<SingleEvent<AsdkState>> = MutableLiveData()
    val uiEventLiveData: LiveData<SingleEvent<AsdkState>> = uiEvent

    override fun onCleared() {
        super.onCleared()
        paymentProcess?.stop()
    }

    fun initPayment(token: String, paymentOptions: PaymentOptions) {
        changeScreenState(LoadingState)

        paymentProcess = PaymentProcess(sdk, context).createPaymentProcess(GooglePay(token), paymentOptions)
                .subscribe(object : PaymentListenerAdapter() {
                    override fun onSuccess(paymentId: Long, cardId: String?, rebillId: String?) {
                        changeScreenState(LoadedState)
                        paymentResult.value = PaymentResult(paymentId, cardId, rebillId)
                    }

                    override fun onUiNeeded(state: AsdkState) {
                        changeScreenState(LoadedState)
                        uiEvent.value = SingleEvent(state)
                    }

                    override fun onError(throwable: Throwable, paymentId: Long?) {
                        changeScreenState(LoadedState)
                        errorData.value = throwable
                    }
                }).start()
    }
}