package ru.tinkoff.acquiring.sdk.redesign.mirpay.presentation

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
import kotlinx.coroutines.processNextEventInCurrentThread
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.MirPayProcess
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_START_DATA
import ru.tinkoff.acquiring.sdk.redesign.mirpay.MirPayLauncher
import ru.tinkoff.acquiring.sdk.redesign.mirpay.nav.MirPayNavigation

/**
 * @author k.shpakovskiy
 */
internal class MirPayViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val mirPayProcess: MirPayProcess,
    private val mirPayMapper: MirPayProcessMapper,
    private val mirPayNavigation: MirPayNavigation
) : ViewModel() {

    private val startData: MirPayLauncher.StartData by lazy {
        checkNotNull(savedStateHandle[EXTRA_START_DATA])
    }
    val state = mirPayProcess.state.mapNotNull(mirPayMapper::mapState)
    val navEvent = mirPayNavigation.flow

    init {
        mirPayProcess.state
            .mapNotNull(mirPayMapper::mapEvent)
            .onEach(mirPayNavigation::send)
            .launchIn(viewModelScope)
    }

    fun pay() {
        mirPayProcess.start(paymentOptions = startData.paymentOptions)
    }

    fun goingToBankApp() {
        mirPayProcess.goingToBankApp()
    }

    fun startCheckingStatus() {
        mirPayProcess.startCheckingStatus()
    }

    fun onClose() {
        viewModelScope.launch {
            mirPayMapper.mapResult(mirPayProcess.state.value)?.let {
                mirPayNavigation.send(MirPayNavigation.Event.Close(it))
            }
        }
    }

    override fun onCleared() {
        mirPayProcess.stop()
        super.onCleared()
    }

    companion object {
        fun factory(application: Application, paymentOptions: PaymentOptions) = viewModelFactory {
            val acq = TinkoffAcquiring(
                application,
                paymentOptions.terminalKey,
                paymentOptions.publicKey
            )
            initializer {
                MirPayViewModel(
                    createSavedStateHandle(),
                    MirPayProcess(acq.sdk),
                    MirPayProcessMapper(),
                    MirPayNavigation()
                )
            }
        }
    }
}
