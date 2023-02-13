package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.NspkRequest
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.common.savedcard.SavedCardsRepository
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkBankAppsProvider
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkInstalledAppsChecker
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.SbpHelper
import ru.tinkoff.acquiring.sdk.utils.BankCaptionProvider
import ru.tinkoff.acquiring.sdk.utils.BankCaptionResourceProvider
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager
import ru.tinkoff.acquiring.sdk.utils.getExtra

/**
 * Created by i.golovachev
 */
internal class MainPaymentFormViewModel(
    private val primaryButtonFactory: MainPaymentFormFactory,
    private val coroutineManager: CoroutineManager
) : ViewModel() {

    val stateFlow = MutableStateFlow<State>(State.Loading)

    init {
        coroutineManager.launchOnBackground {
            runCatching { primaryButtonFactory.primary() }
                .onFailure { stateFlow.value = State.Error }
                .onSuccess { stateFlow.value = State.Content(it) }
        }
    }

    sealed interface State {

        object Loading : State

        object Error : State

        data class Content(val button: MainPaymentFormUi.Primary, val error: Throwable? = null) :
            State
    }

    companion object {

        fun factory(application: Application) = viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                val opt = handle.getExtra<PaymentOptions>()
                val sdk = TinkoffAcquiring(
                    application, opt.terminalKey, opt.publicKey
                ).sdk
                val savedCardRepo =  SavedCardsRepository.Impl(sdk)
                MainPaymentFormViewModel(
                    MainPaymentFormFactory(
                        sdk,
                        savedCardRepo,
                        { NspkRequest().execute().banks },
                        { nspkBanks, dl ->
                            SbpHelper.getBankApps(application.packageManager, dl, nspkBanks)
                        },
                        BankCaptionResourceProvider(application),
                        opt.customer.customerKey!!
                    ),
                    CoroutineManager()
                )
            }
        }
    }
}