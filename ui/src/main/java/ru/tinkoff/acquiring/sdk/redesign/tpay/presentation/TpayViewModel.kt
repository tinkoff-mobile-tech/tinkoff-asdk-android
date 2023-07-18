package ru.tinkoff.acquiring.sdk.redesign.tpay.presentation

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.TpayProcess
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_START_DATA
import ru.tinkoff.acquiring.sdk.redesign.tpay.TpayLauncher
import ru.tinkoff.acquiring.sdk.redesign.tpay.nav.TpayNavigation

internal class TpayViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val tpayProcess: TpayProcess,
    private val tpayProcessMapper: TpayProcessMapper,
    private val tpayNavigation: TpayNavigation
) : ViewModel() {

    private val startData: TpayLauncher.StartData = checkNotNull(savedStateHandle[EXTRA_START_DATA])
    val state = tpayProcess.state.mapNotNull(tpayProcessMapper::mapState)
    val navEvent = tpayNavigation.flow

    init {
        tpayProcess.state
            .mapNotNull(tpayProcessMapper::mapEvent)
            .onEach(tpayNavigation::send)
            .launchIn(viewModelScope)
    }

    fun pay() {
        tpayProcess.start(
            paymentOptions = startData.paymentOptions,
            tpayVersion = startData.version,
            paymentId = startData.paymentId
        )
    }

    fun goingToBankApp() {
        tpayProcess.goingToBankApp()
    }

    fun startCheckingStatus() {
        tpayProcess.startCheckingStatus()
    }

    fun onClose() {
        viewModelScope.launch {
            tpayProcessMapper.mapResult(tpayProcess.state.value)?.let {
                tpayNavigation.send(TpayNavigation.Event.Close(it))
            }
        }
    }

    companion object {
        fun factory(application: Application, paymentOptions: PaymentOptions) = viewModelFactory {
            val acq = TinkoffAcquiring(
                application,
                paymentOptions.terminalKey,
                paymentOptions.publicKey
            )
            initializer {
                TpayViewModel(
                    createSavedStateHandle(),
                    TpayProcess(acq.sdk),
                    TpayProcessMapper(),
                    TpayNavigation()
                )
            }
        }
    }
}
