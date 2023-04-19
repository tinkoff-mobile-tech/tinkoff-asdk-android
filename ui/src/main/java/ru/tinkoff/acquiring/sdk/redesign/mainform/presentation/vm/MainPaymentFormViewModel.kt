package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.analytics.ChosenMethod
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardProcess
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardState
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.analytics.MainFormAnalyticsDelegate
import ru.tinkoff.acquiring.sdk.redesign.mainform.navigation.MainFormNavController
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormFactory
import ru.tinkoff.acquiring.sdk.redesign.payment.ui.PaymentByCard
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager
import ru.tinkoff.acquiring.sdk.utils.getExtra

/**
 * Created by i.golovachev
 */
internal class MainPaymentFormViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val primaryButtonFactory: MainPaymentFormFactory,
    private val mainFormNavController: MainFormNavController,
    private val paymentByCardProcess: PaymentByCardProcess,
    private val mainFormAnalyticsDelegate: MainFormAnalyticsDelegate,
    private val coroutineManager: CoroutineManager,
) : ViewModel() {

    private val paymentOptions: PaymentOptions = savedStateHandle.getExtra()
    private val _formState = MutableStateFlow<MainPaymentForm.State?>(null)
    val primary = _formState.filterNotNull().map { it.ui.primary }
    val secondary = _formState.filterNotNull().map { it.ui.secondaries }
    val chosenCard =
        _formState.mapNotNull { (it?.ui?.primary as? MainPaymentForm.Primary.Card)?.selectedCard }
    val formContent = MutableStateFlow<FormContent>(FormContent.Loading)
    val mainFormNav = mainFormNavController.navFlow

    init {
        loadState()
    }

    fun onRetry() {
        loadState()
    }

    fun toSbp() = viewModelScope.launch(coroutineManager.main) {
        mainFormNavController.toSbp(
            mainFormAnalyticsDelegate.prepareOptions(
                paymentOptions,
                ChosenMethod.Sbp
            )
        )
    }

    fun toNewCard() = viewModelScope.launch(coroutineManager.main) {
        mainFormNavController.toPayNewCard(
            mainFormAnalyticsDelegate.prepareOptions(
                paymentOptions,
                ChosenMethod.NewCard
            )
        )
    }

    fun toChooseCard() = viewModelScope.launch(coroutineManager.main) {
        mainFormNavController.toChooseCard(paymentOptions, _formState.value?.data?.chosen)
    }

    fun toTpay() = viewModelScope.launch(coroutineManager.main) {
        mainFormNavController.toTpay(
            mainFormAnalyticsDelegate.prepareOptions(
                paymentOptions,
                ChosenMethod.TinkoffPay
            )
        )
    }

    fun choseCard(card: Card) {
        _formState.update {
            it?.let { primaryButtonFactory.changeCard(it, card) }
        }
    }

    fun onBackPressed() = viewModelScope.launch {
        val result = when (val it = paymentByCardProcess.state.value) {
            is PaymentByCardState.Created -> PaymentByCard.Canceled
            is PaymentByCardState.Error -> PaymentByCard.Error(it.throwable, null)
            is PaymentByCardState.Started -> PaymentByCard.Canceled
            is PaymentByCardState.Success -> PaymentByCard.Success(
                it.paymentId,
                it.cardId,
                it.rebillId
            )
            is PaymentByCardState.ThreeDsInProcess -> PaymentByCard.Canceled
            is PaymentByCardState.ThreeDsUiNeeded -> PaymentByCard.Canceled
            is PaymentByCardState.CvcUiNeeded -> PaymentByCard.Canceled
            is PaymentByCardState.CvcUiInProcess -> PaymentByCard.Canceled
        }
        mainFormNavController.close(result)
    }

    private fun loadState() {
        coroutineManager.launchOnBackground {
            formContent.value = FormContent.Loading
            runCatching { primaryButtonFactory.getState() }
                .onFailure {
                    formContent.value = FormContent.Error
                }
                .onSuccess {
                    mainFormAnalyticsDelegate.mapAnalytics(it.ui.primary)
                    _formState.value = it
                    formContent.value = if (it.noInternet) {
                        FormContent.NoNetwork
                    } else {
                        FormContent.Content(it.ui)
                    }
                }
        }
    }

    sealed interface FormContent {

        object Loading : FormContent

        object Error : FormContent

        object NoNetwork : FormContent

        class Content(val state: MainPaymentForm.Ui) : FormContent {
            val isSavedCard = (state.primary as? MainPaymentForm.Primary.Card)?.selectedCard != null
        }
    }
}