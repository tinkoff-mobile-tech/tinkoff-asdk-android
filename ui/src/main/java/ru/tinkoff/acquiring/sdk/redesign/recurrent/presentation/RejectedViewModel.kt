package ru.tinkoff.acquiring.sdk.redesign.recurrent.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardState
import ru.tinkoff.acquiring.sdk.payment.RecurrentPaymentProcess
import ru.tinkoff.acquiring.sdk.redesign.payment.model.CardChosenModel
import ru.tinkoff.acquiring.sdk.redesign.recurrent.ui.RecurrentPaymentActivity
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.validators.CardValidator
import ru.tinkoff.acquiring.sdk.utils.BankCaptionProvider
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager
import ru.tinkoff.acquiring.sdk.utils.getExtra

internal class RejectedViewModel(
    private val recurrentPaymentProcess: RecurrentPaymentProcess,
    private val bankCaptionProvider: BankCaptionProvider,
    private val coroutineManager: CoroutineManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val paymentOptions: PaymentOptions = savedStateHandle.getExtra()
    private val card: Card = checkNotNull(savedStateHandle.get<Card>(RecurrentPaymentActivity.EXTRA_CARD))

    val needInputCvcState = recurrentPaymentProcess.state
        .map { mapCvcState(card, it) }
        .flowOn(coroutineManager.io)


    val loaderButtonState = recurrentPaymentProcess.state.map { it is PaymentByCardState.CvcUiInProcess }

    private val cvc: StateFlow<String?> = savedStateHandle.getStateFlow(DATA_CVC,null)
    val cvcValid = cvc.map { CardValidator.validateSecurityCodeOrFalse(it) }
    private val rejectedPaymentId: StateFlow<String?> = savedStateHandle.getStateFlow(
        DATA_REJECTED_PAYMENT_ID, null)
    val needHideKeyboard = savedStateHandle.getStateFlow(DATA_NEED_HIDE_KEYBOARD, false)

    fun inputCvc(cvc: String) {
        savedStateHandle[DATA_CVC] = cvc
    }

    fun payRejected() {
        savedStateHandle[DATA_NEED_HIDE_KEYBOARD] = true
        recurrentPaymentProcess.startWithCvc(
            cvc = checkNotNull(cvc.value),
            rebillId = checkNotNull(card.rebillId),
            rejectedId = checkNotNull(rejectedPaymentId.value),
            paymentOptions = paymentOptions,
            email = paymentOptions.customer.email
        )
    }

    private fun mapCvcState(card: Card, state: PaymentByCardState): CardChosenModel? {
        return when (state) {
            is PaymentByCardState.CvcUiNeeded ->  {
                savedStateHandle[DATA_REJECTED_PAYMENT_ID] = state.rejectedPaymentId
                CardChosenModel(card, bankCaptionProvider(checkNotNull(card.pan)))
            }
            else -> null
        }
    }

    companion object {
        private const val DATA_CVC = "data_cvc"
        private const val DATA_REJECTED_PAYMENT_ID = "rejected_payment_id"
        private const val DATA_NEED_HIDE_KEYBOARD = "need_hide_keyboard"
    }
}