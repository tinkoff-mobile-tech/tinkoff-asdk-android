package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.NspkRequest
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.common.emailinput.EmailValidator
import ru.tinkoff.acquiring.sdk.redesign.common.savedcard.SavedCardsRepository
import ru.tinkoff.acquiring.sdk.redesign.mainform.navigation.MainFormNavController
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.primary.PrimaryButtonConfigurator
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.secondary.SecondButtonConfigurator
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkBankAppsProvider
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkInstalledAppsChecker
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.SbpHelper
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.validators.CardValidator
import ru.tinkoff.acquiring.sdk.utils.BankCaptionResourceProvider
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager
import ru.tinkoff.acquiring.sdk.utils.getExtra

/**
 * Created by i.golovachev
 */
internal class MainPaymentFormViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val primaryButtonFactory: MainPaymentFormFactory,
    private val mainFormNavController: MainFormNavController,
    private val coroutineManager: CoroutineManager,
) : ViewModel() {

    private val paymentOptions: PaymentOptions = savedStateHandle.getExtra()

    val stateFlow = MutableStateFlow<State>(State.Loading)
    val cvcFlow = savedStateHandle.getStateFlow(CVC_KEY, "")
    val emailFlow = savedStateHandle.getStateFlow(EMAIL_KEY, paymentOptions.customer.email)
    val needEmail = savedStateHandle.getStateFlow(
        NEED_EMAIL_KEY,
        paymentOptions.customer.email.isNullOrBlank().not()
    )

    val payEnable =
        combine(stateFlow, cvcFlow, needEmail, emailFlow) { state, cvc, needEmailValidate, email ->
            validateState(state, cvc, needEmailValidate, email)
        }

    val mainFormNav = mainFormNavController.navFlow

    init {
        coroutineManager.launchOnBackground {
            runCatching { primaryButtonFactory.getUi() }
                .onFailure { stateFlow.value = State.Error }
                .onSuccess { stateFlow.value = State.Content(it) }
        }
    }

    fun setCvc(cvc: String) {
        savedStateHandle[CVC_KEY] = cvc
    }

    fun needEmail(isNeed: Boolean) {
        savedStateHandle[NEED_EMAIL_KEY] = isNeed
    }

    fun email(email: String) {
        savedStateHandle[EMAIL_KEY] = email
    }

    fun toSbp() = viewModelScope.launch { mainFormNavController.toSbp(paymentOptions) }

    fun toNewCard() = viewModelScope.launch { mainFormNavController.toPayNewCard(paymentOptions) }

    fun toChooseCard() =
        viewModelScope.launch { mainFormNavController.toChooseCard(paymentOptions) }

    fun toTpay() {
        // todo
    }

    @VisibleForTesting
    fun validateState(
        state: State,
        cvc: String,
        needEmailValidate: Boolean,
        email: String?
    ): Boolean {
        val checkCard = state is State.Content
                && state.ui.primary is MainPaymentFormUi.Primary.Card
                && state.ui.primary.selectedCard != null

        return if (checkCard) {
            val validateEmailIfNeed =
                if (needEmailValidate) EmailValidator.validate(email) else true
            CardValidator.validateSecurityCode(cvc) && validateEmailIfNeed
        } else {
            state is State.Content
        }
    }

    sealed interface State {

        object Loading : State

        object Error : State

        data class Content(val ui: MainPaymentFormUi.Ui, val error: Throwable? = null) :
            State
    }

    companion object {

        private const val EMAIL_KEY = "EMAIL_KEY"
        private const val NEED_EMAIL_KEY = "NEED_EMAIL_KEY"
        private const val CVC_KEY = "CVC_KEY"

        fun factory(application: Application) = viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                val opt = handle.getExtra<PaymentOptions>()
                val sdk = TinkoffAcquiring(
                    application, opt.terminalKey, opt.publicKey
                ).sdk
                val savedCardRepo = SavedCardsRepository.Impl(sdk)
                val nspkProvider = NspkBankAppsProvider { NspkRequest().execute().banks }
                val nspkChecker = NspkInstalledAppsChecker { nspkBanks, dl ->
                    SbpHelper.getBankApps(application.packageManager, dl, nspkBanks)
                }
                val bankCaptionProvider = BankCaptionResourceProvider(application)
                MainPaymentFormViewModel(
                    handle,
                    MainPaymentFormFactory(
                        sdk,
                        savedCardRepo,
                        PrimaryButtonConfigurator.Impl(
                            nspkProvider,
                            nspkChecker,
                            bankCaptionProvider
                        ),
                        SecondButtonConfigurator.Impl(nspkProvider, nspkChecker),
                        MergeMethodsStrategy.ImplV1,
                        opt.customer.customerKey!!
                    ),
                    MainFormNavController(),
                    CoroutineManager(),
                )
            }
        }
    }
}