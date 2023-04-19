package ru.tinkoff.acquiring.sdk.redesign.recurrent.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring.Companion.EXTRA_REBILL_ID
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.AttachedCard
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardState
import ru.tinkoff.acquiring.sdk.payment.RecurrentPaymentProcess
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsDataCollector
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.utils.getExtra

internal class RecurrentPaymentViewModel(
    private val recurrentPaymentProcess: RecurrentPaymentProcess,
    private val recurrentProcessMapper: RecurrentProcessMapper,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val paymentOptions: PaymentOptions = savedStateHandle.getExtra()
    private val rebillId: String = checkNotNull(savedStateHandle.get<String>(EXTRA_REBILL_ID))
    private val eventChannel = Channel<RecurrentPaymentEvent>()
    val state = recurrentPaymentProcess.state.map {
        recurrentProcessMapper(it)
    }
    val events = eventChannel.receiveAsFlow()

    fun pay() {
        viewModelScope.launch {
            recurrentPaymentProcess.start(
                AttachedCard(rebillId),
                paymentOptions,
                paymentOptions.customer.email
            )
        }
    }

    fun onClose() {
        viewModelScope.launch {
            when (val paymentState = recurrentPaymentProcess.state.value) {
                PaymentByCardState.Created -> Unit
                PaymentByCardState.CvcUiInProcess -> RecurrentPaymentEvent.CloseWithCancel()
                is PaymentByCardState.CvcUiNeeded -> RecurrentPaymentEvent.CloseWithCancel()
                is PaymentByCardState.Error -> eventChannel.send(
                    RecurrentPaymentEvent.CloseWithError(
                        paymentState
                    )
                )
                is PaymentByCardState.Started -> Unit
                is PaymentByCardState.Success -> eventChannel.send(
                    RecurrentPaymentEvent.CloseWithSuccess(paymentState)
                )
                PaymentByCardState.ThreeDsInProcess -> eventChannel.send(
                    RecurrentPaymentEvent.CloseWithCancel()
                )
                is PaymentByCardState.ThreeDsUiNeeded -> eventChannel.send(
                    RecurrentPaymentEvent.CloseWithCancel()
                )
            }
        }
    }

    companion object {
        fun factory(
            application: Application,
            paymentOptions: PaymentOptions,
            threeDsDataCollector: ThreeDsDataCollector = ThreeDsHelper.CollectData
        ) = viewModelFactory {
            RecurrentPaymentProcess.init(
                TinkoffAcquiring(
                    application,
                    paymentOptions.terminalKey,
                    paymentOptions.publicKey
                ).sdk,
                application,
                threeDsDataCollector
            )
            initializer {
                RecurrentPaymentViewModel(
                    RecurrentPaymentProcess.get(),
                    RecurrentProcessMapper(),
                    createSavedStateHandle()
                )
            }
        }
    }
}