package ru.tinkoff.acquiring.sdk.redesign.payment.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardProcess

internal class PaymentByCardViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val paymentByCardProcess: PaymentByCardProcess
) : ViewModel() {

    private val startData =
        savedStateHandle.get<PaymentByCard.StartData>(PaymentByCard.Contract.EXTRA_SAVED_CARDS)!!

    val paymentProcessState = paymentByCardProcess.state

    val state: MutableStateFlow<State> =
        MutableStateFlow(
            State(
                isValidEmail = startData.paymentOptions.customer.email.isNullOrBlank().not(),
                sendReceipt = startData.paymentOptions.customer.email.isNullOrBlank().not(),
                email = startData.paymentOptions.customer.email,
                paymentOptions = startData.paymentOptions,
                hasSavedCard = startData.list.isNotEmpty()
            )
        )

    fun setCardDate(
        cardNumber: String? = null,
        cvc: String? = null,
        dateExpired: String? = null,
        isValidCardData: Boolean = false
    ) = state.update {
        it.copy(
            cardNumber = cardNumber,
            cvc = cvc,
            dateExpired = dateExpired,
            isValidCardData = isValidCardData
        )
    }

    fun setCvc(cvc: String) = state.update { it.copy(cvc = cvc) }

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

        val hasSavedCard: Boolean = false,
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
        fun factory() = viewModelFactory {
            initializer {
                PaymentByCardViewModel(createSavedStateHandle(), PaymentByCardProcess.get())
            }
        }
    }
}