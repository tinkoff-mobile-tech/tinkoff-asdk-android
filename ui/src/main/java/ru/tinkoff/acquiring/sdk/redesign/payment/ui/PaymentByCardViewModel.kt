package ru.tinkoff.acquiring.sdk.redesign.payment.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.*
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.AttachedCard
import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.models.paysources.CardSource
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardProcess
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_SAVED_CARDS
import ru.tinkoff.acquiring.sdk.redesign.payment.PaymentByCardLauncher
import ru.tinkoff.acquiring.sdk.redesign.payment.model.CardChosenModel
import ru.tinkoff.acquiring.sdk.utils.BankCaptionProvider
import ru.tinkoff.acquiring.sdk.utils.BankCaptionResourceProvider

// todo - раздельная vm  для сохраненной карты и новой
internal class PaymentByCardViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val paymentByCardProcess: PaymentByCardProcess,
    private val bankCaptionProvider: BankCaptionProvider,
) : ViewModel() {

    private val startData =
        savedStateHandle.get<PaymentByCardLauncher.StartData>(EXTRA_SAVED_CARDS)!!
    private val chosenCard = startData.list.firstOrNull { it.status == CardStatus.ACTIVE }?.let {
        CardChosenModel(it, bankCaptionProvider(it.pan!!))
    }

    val paymentProcessState = paymentByCardProcess.state

    val state: MutableStateFlow<State> =
        MutableStateFlow(
            State(
                cardId = chosenCard?.id,
                isValidEmail = startData.paymentOptions.customer.email.isNullOrBlank().not(),
                sendReceipt = startData.paymentOptions.customer.email.isNullOrBlank().not(),
                email = startData.paymentOptions.customer.email,
                paymentOptions = startData.paymentOptions,
                chosenCard = chosenCard
            )
        )

    // ручной ввод карты
    fun setCardDate(
        cardNumber: String? = null,
        cvc: String? = null,
        dateExpired: String? = null,
        isValidCardData: Boolean = false,
    ) {
        if (state.value.chosenCard != null) return

        state.update {
            it.copy(
                cardNumber = cardNumber,
                cvc = cvc,
                dateExpired = dateExpired,
                isValidCardData = isValidCardData,
                cardId = null,
            )
        }
    }

    // ввод сохраненной карты
    fun setSavedCard(card: Card) = state.update {
        it.copy(
            cardId = card.cardId,
            cardNumber = card.pan,
            cvc = null,
            dateExpired = card.expDate,
            isValidCardData = false,
            chosenCard = CardChosenModel(card, bankCaptionProvider(card.pan!!))
        )
    }

    // ввод кода сохраненной карты
    fun setCvc(cvc: String, isValid: Boolean) =
        state.update { it.copy(cvc = cvc, isValidCardData = isValid) }

    fun setInputNewCard() = state.update {
            it.copy(
                cardId = null,
                cardNumber = null,
                cvc = null,
                dateExpired = null,
                isValidCardData = false,
                isValidEmail = it.isValidEmail,
                chosenCard = null
            )
        }

    fun sendReceiptChange(isSelect: Boolean) = state.update {
        it.copy(sendReceipt = isSelect)
    }

    fun setEmail(email: String?, isValidEmail: Boolean) = state.update {
        it.copy(email = email, isValidEmail = isValidEmail)
    }

    fun pay() {
        val _state = state.value
        val emailForPayment = if (_state.sendReceipt) _state.email else null

        paymentByCardProcess.start(
            cardData = _state.cardSource,
            paymentOptions = _state.paymentOptions,
            email = emailForPayment
        )
    }

    fun rechoseCard() {
        paymentByCardProcess.recreate()
    }
    fun goTo3ds() {
        paymentByCardProcess.goTo3ds()
    }

    fun cancelPayment() {
        paymentByCardProcess.stop()
    }

    data class State(
        private val cardId: String? = null,
        private val cardNumber: String? = null,
        private val cvc: String? = null,
        private val dateExpired: String? = null,
        private val isValidCardData: Boolean = false,
        val isValidEmail: Boolean = false,
        val chosenCard: CardChosenModel? = null,
        val sendReceipt: Boolean = false,
        val email: String? = null,
        val paymentOptions: PaymentOptions,
    ) {

        val buttonEnabled: Boolean = if (sendReceipt) {
            isValidCardData && isValidEmail
        } else {
            isValidCardData
        }

        val amount = paymentOptions.order.amount.toHumanReadableString()

        val cardSource: CardSource
            get() {
                return if (cardId != null)
                    AttachedCard(cardId, cvc)
                else
                    CardData(cardNumber!!, dateExpired!!, cvc!!)
            }
    }

    companion object {
        fun factory(application: Application) = viewModelFactory {
            initializer {
                PaymentByCardViewModel(
                    createSavedStateHandle(),
                    PaymentByCardProcess.get(),
                    BankCaptionResourceProvider(application)
                )
            }
        }
    }
}
