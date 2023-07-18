package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.vm

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.*
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.analytics.ChosenMethod
import ru.tinkoff.acquiring.sdk.models.paysources.AttachedCard
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardProcess
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardState
import ru.tinkoff.acquiring.sdk.redesign.common.emailinput.EmailValidator
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.analytics.MainFormAnalyticsDelegate
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFromUtils.CHOSEN_CARD
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFromUtils.CVC_KEY
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFromUtils.EMAIL_KEY
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFromUtils.NEED_EMAIL_KEY
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.process.MainFormPaymentProcessMapper
import ru.tinkoff.acquiring.sdk.redesign.payment.model.CardChosenModel
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.validators.CardValidator
import ru.tinkoff.acquiring.sdk.utils.BankCaptionProvider
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager
import ru.tinkoff.acquiring.sdk.utils.getExtra

/**
 * Created by i.golovachev
 */
internal class MainFormInputCardViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val byCardProcess: PaymentByCardProcess,
    private val mapper: MainFormPaymentProcessMapper,
    private val bankCaptionProvider: BankCaptionProvider,
    private val mainFormAnalyticsDelegate: MainFormAnalyticsDelegate,
    private val coroutineManager: CoroutineManager,
) : ViewModel() {

    private val paymentOptions: PaymentOptions = savedStateHandle.getExtra()

    val savedCardFlow =
        savedStateHandle.getStateFlow<CardChosenModel?>(CHOSEN_CARD, null).filterNotNull()
    val cvcFlow = savedStateHandle.getStateFlow(CVC_KEY, "")
    val emailFlow = savedStateHandle.getStateFlow(EMAIL_KEY, paymentOptions.customer.email)
    val needEmail = savedStateHandle.getStateFlow(
        NEED_EMAIL_KEY,
        paymentOptions.customer.email.isNullOrBlank().not()
    )
    val payEnable = combine(cvcFlow, needEmail, emailFlow) { cvc, needEmailValidate, email ->
        validateState(cvc, needEmailValidate, email)
    }
    val isLoading = byCardProcess.state.map { it is PaymentByCardState.Started }
    val paymentStatus = byCardProcess.state.map { mapper(it) }

    fun choseCard(cardChosenModel: CardChosenModel) {
        savedStateHandle[CHOSEN_CARD] = cardChosenModel
    }

    fun choseCard(card: Card) {
        savedStateHandle[CHOSEN_CARD] = CardChosenModel(card, bankCaptionProvider(card.pan!!))
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

    fun set3dsResult(error: Throwable?) {
        byCardProcess.set3dsResult(error)
    }

    fun set3dsResult(paymentResult: PaymentResult) {
        byCardProcess.set3dsResult(paymentResult)
    }

    fun pay() = coroutineManager.launchOnBackground {
        val card = savedStateHandle.get<CardChosenModel>(CHOSEN_CARD)!!
        val cvc = savedStateHandle.get<String>(CVC_KEY)
        byCardProcess.start(
            paymentOptions = mainFormAnalyticsDelegate.prepareOptions(paymentOptions, ChosenMethod.Card),
            cardData = AttachedCard(cardId = card.id, cvv = cvc)
        )
    }

    @VisibleForTesting
    fun validateState(
        cvc: String,
        needEmailValidate: Boolean,
        email: String?
    ): Boolean {
        val validateEmailIfNeed =
            if (needEmailValidate) EmailValidator.validate(email) else true
        return CardValidator.validateSecurityCode(cvc) && validateEmailIfNeed
    }
}