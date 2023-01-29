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
import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardProcess
import ru.tinkoff.acquiring.sdk.redesign.payment.model.CardChosenModel
import ru.tinkoff.acquiring.sdk.utils.BankCaptionProvider
import ru.tinkoff.acquiring.sdk.utils.BankCaptionResourceProvider

internal class PaymentByCardViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val paymentByCardProcess: PaymentByCardProcess,
    private val bankCaptionProvider: BankCaptionProvider,
) : ViewModel() {

    private val startData =
        savedStateHandle.get<PaymentByCard.StartData>(PaymentByCard.Contract.EXTRA_SAVED_CARDS)!!
    private val chosenCard = startData.list.firstOrNull { it.status == CardStatus.ACTIVE }?.let {
        CardChosenModel(it, bankCaptionProvider(it.pan!!))
    }

    val paymentProcessState = paymentByCardProcess.state

    val state: MutableStateFlow<State> =
        MutableStateFlow(
            State(
                isValidEmail = startData.paymentOptions.customer.email.isNullOrBlank().not(),
                sendReceipt = startData.paymentOptions.customer.email.isNullOrBlank().not(),
                email = startData.paymentOptions.customer.email,
                paymentOptions = startData.paymentOptions,
                chosenCard = chosenCard
            )
        )

    fun setCardDate(
        cardNumber: String? = null,
        cvc: String? = null,
        dateExpired: String? = null,
        isValidCardData: Boolean = false,
    ) = state.update {
        it.copy(
            cardNumber = cardNumber,
            cvc = cvc,
            dateExpired = dateExpired,
            isValidCardData = isValidCardData,
        )
    }

    fun setSavedCard(card: Card) = state.update {
        it.copy(
            cardNumber = card.pan,
            cvc = null,
            dateExpired = card.expDate,
            isValidCardData = false,
            chosenCard = CardChosenModel(card, bankCaptionProvider(card.pan!!))
        )
    }

    fun setCvc(cvc: String, isValid: Boolean) =
        state.update { it.copy(cvc = cvc, isValidCardData = isValid) }

    fun sendReceiptChange(isSelect: Boolean) = state.update {
        it.copy(sendReceipt = isSelect)
    }

    fun setEmail(email: String?, isValidEmail: Boolean) = state.update {
        it.copy(email = email, isValidEmail = isValidEmail)
    }

    fun pay() {
        val _state = state.value
        val emailForPayment = if (_state.sendReceipt) _state.email else null
        paymentByCardProcess.start(_state.cardData, _state.paymentOptions, emailForPayment)
    }

    fun cancelPayment() {
        paymentByCardProcess.stop()
    }

    data class State(
        private val cardNumber: String? = null,
        private val cvc: String? = null,
        private val dateExpired: String? = null,
        private val isValidCardData: Boolean = false,
        private val isValidEmail: Boolean = false,
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

        val cardData: CardData
            get() {
                return CardData(
                    pan = cardNumber!!,
                    expiryDate = dateExpired!!,
                    securityCode = cvc!!
                ).apply {
                    validate()
                }
            }
    }

    companion object {
        fun factory(application: Application) = viewModelFactory {
            initializer {
                PaymentByCardViewModel(
                    createSavedStateHandle(), PaymentByCardProcess.get(),
                    BankCaptionResourceProvider(application)
                )
            }
        }
    }
}