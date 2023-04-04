package ru.tinkoff.acquiring.sdk.redesign.recurrent.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.AttachedCard
import ru.tinkoff.acquiring.sdk.payment.RecurrentPaymentProcess
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsDataCollector
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.utils.getExtra

internal class RecurrentPaymentViewModel(
    private val recurrentPaymentProcess: RecurrentPaymentProcess,
    private val recurrentProcessMapper: RecurrentProcessMapper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val paymentOptions: PaymentOptions = savedStateHandle.getExtra()
    val state = recurrentPaymentProcess.state.map {
        recurrentProcessMapper(it)
    }

    fun pay(card: Card) {
        viewModelScope.launch {
            recurrentPaymentProcess.start(
                AttachedCard(card.rebillId), paymentOptions, paymentOptions.customer.email
            )
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