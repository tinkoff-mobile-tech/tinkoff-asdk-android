package ru.tinkoff.acquiring.sdk.redesign.payment.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardState
import ru.tinkoff.acquiring.sdk.redesign.common.carddatainput.CardDataInputFragment
import ru.tinkoff.acquiring.sdk.redesign.common.emailinput.EmailInputFragment
import ru.tinkoff.acquiring.sdk.redesign.dialog.*
import ru.tinkoff.acquiring.sdk.redesign.payment.ui.PaymentByCard.Contract.EXTRA_SAVED_CARDS
import ru.tinkoff.acquiring.sdk.redesign.payment.ui.PaymentByCard.Contract.createSuccessIntent
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.ui.activities.TransparentActivity
import ru.tinkoff.acquiring.sdk.ui.component.bindKtx
import ru.tinkoff.acquiring.sdk.ui.customview.LoaderButton
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.utils.getOptions
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.utils.lazyView

/**
 * Created by i.golovachev
 */
internal class PaymentByCardActivity : AppCompatActivity(),
    CardDataInputFragment.OnCardDataChanged,
    EmailInputFragment.OnEmailDataChanged,
    OnPaymentSheetCloseListener {

    private val startData: PaymentByCard.StartData by lazyUnsafe {
        intent.getParcelableExtra(EXTRA_SAVED_CARDS)!!
    }
    private val savedCardOptions: SavedCardsOptions by lazyUnsafe {
        SavedCardsOptions().apply {
            setTerminalParams(
                startData.paymentOptions.terminalKey,
                startData.paymentOptions.publicKey
            )
            customer = startData.paymentOptions.customer
            features = startData.paymentOptions.features
        }
    }
    private val cardDataInputContainer: FragmentContainerView by lazyView(R.id.fragment_card_data_input)
    private val cardDataInput
        get() = supportFragmentManager.findFragmentById(R.id.fragment_card_data_input) as CardDataInputFragment
    private val chosenCardContainer: CardView by lazyView(R.id.acq_chosen_card)
    private val chosenCardComponent: ChosenCardComponent by lazyUnsafe {
        ChosenCardComponent(
            chosenCardContainer,
            onChangeCard = { onChangeCard() },
            onCvcCompleted = { cvc, isValid -> viewModel.setCvc(cvc, isValid) }
        )
    }
    private val emailInput: EmailInputFragment by lazyUnsafe {
        EmailInputFragment.getInstance(startData.paymentOptions.customer.email)
    }
    private val emailInputContainer: FragmentContainerView by lazyView(R.id.fragment_email_input)
    private val sendReceiptSwitch: SwitchCompat by lazyView(R.id.acq_send_receipt_switch)
    private val payButton: LoaderButton by lazyView(R.id.acq_pay_btn)
    private val viewModel: PaymentByCardViewModel by viewModels {
        PaymentByCardViewModel.factory(application)
    }
    private val statusSheetStatus = createPaymentSheetWrapper()
    private val savedCards =
        registerForActivityResult(TinkoffAcquiring.ChoseCard.Contract) { result ->
            chosenCardComponent.clearCvc()
            when (result) {
                is TinkoffAcquiring.ChoseCard.Success -> viewModel.setSavedCard(result.card)
                is TinkoffAcquiring.ChoseCard.NeedInputNewCard ->  {
                    cardDataInput.clearInput()
                    viewModel.setInputNewCard()
                }
                else -> Unit
            }
        }

    //todo сделать через вьюмодельй
    private var onBackEnabled: Boolean = true

    //region Activity LC
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_payment_by_card_new_activity)
        initToolbar()
        initViews()

        lifecycleScope.launchWhenResumed { processState() }
        lifecycleScope.launchWhenCreated { uiState() }
        lifecycleScope.launch { selectedCardState() }

        cardDataInput.setupCameraCardScanner(startData.paymentOptions.features.cameraCardScannerContract)
        cardDataInput.validateNotExpired = startData.paymentOptions.features.validateExpiryDate
        chosenCardComponent.bindKtx(lifecycleScope, viewModel.state.mapNotNull { it.chosenCard })
    }

    override fun onStop() {
        super.onStop()
        statusSheetStatus.takeIf { it.isAdded }?.dismissAllowingStateLoss()
        payButton.setOnClickListener {
            viewModel.pay()
        }
    }

    override fun onResume() {
        super.onResume()
        if (statusSheetStatus.state != null) {
            statusSheetStatus.showIfNeed(supportFragmentManager)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == TransparentActivity.THREE_DS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val result =
                    data.getSerializableExtra(ThreeDsHelper.Launch.RESULT_DATA) as PaymentResult
                statusSheetStatus.state = PaymentStatusSheetState.Success(
                    title = R.string.acq_commonsheet_paid_title,
                    mainButton = R.string.acq_commonsheet_clear_primarybutton,
                    paymentId = result.paymentId!!,
                    cardId = result.cardId,
                    rebillId = result.rebillId
                )
            } else if (resultCode == ThreeDsHelper.Launch.RESULT_ERROR) {
                statusSheetStatus.state = PaymentStatusSheetState.Error(
                    title = R.string.acq_commonsheet_failed_title,
                    mainButton = R.string.acq_commonsheet_failed_primary_button,
                    throwable = data?.getSerializableExtra(ThreeDsHelper.Launch.ERROR_DATA) as Throwable
                )
            } else {
                setResult(Activity.RESULT_CANCELED)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
    //endregion

    //region Data change callbacks
    override fun onCardDataChanged(isValid: Boolean) {
        viewModel.setCardDate(
            cardNumber = cardDataInput.cardNumber,
            cvc = cardDataInput.cvc,
            dateExpired = cardDataInput.expiryDate,
            isValidCardData = isValid
        )
    }

    override fun onEmailDataChanged(isValid: Boolean) {
        viewModel.setEmail(email = emailInput.emailValue, isValidEmail = isValid)
    }
    //endregion

    //region Navigation
    private fun onChangeCard() {
        viewModel.rechoseCard()
        savedCards.launch(savedCardOptions)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        if(onBackEnabled.not()) return

        viewModel.cancelPayment()
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onClose(state: PaymentStatusSheetState) {
        when (state) {
            is PaymentStatusSheetState.Error -> statusSheetStatus.dismissAllowingStateLoss()
            is PaymentStatusSheetState.Success -> finishWithSuccess(state.getPaymentResult())
            else -> {
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }
    //endregion

    //region init views
    private fun initToolbar() {
        setSupportActionBar(findViewById(R.id.acq_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.acq_cardpay_title)
    }

    private fun initViews() {

        supportFragmentManager.commit {
            replace(emailInputContainer.id, emailInput)
        }

        sendReceiptSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.sendReceiptChange(isChecked)
            // TODO Избавиться от логики на ui в рамках задачи EACQAPW-5066
            when {
                emailInput.isAdded.not() || emailInput.isVisible.not() -> return@setOnCheckedChangeListener
                isChecked -> emailInput.emailInput.requestViewFocus()
                else -> {
                    emailInput.emailInput.clearViewFocus()
                    emailInput.emailInput.hideKeyboard()
                    if (cardDataInput.isAdded && cardDataInput.isVisible) {
                        cardDataInput.clearFocus()
                    }
                }
            }
        }

        payButton.setOnClickListener {
            viewModel.pay()
        }
    }
    //endregion

    //region subscribe States
    private suspend fun uiState() {
        viewModel.state.collect {
            chosenCardContainer.isVisible = it.chosenCard != null
            cardDataInputContainer.isVisible = it.chosenCard == null
            emailInputContainer.isVisible = it.sendReceipt
            sendReceiptSwitch.isChecked = it.sendReceipt
            payButton.text = getString(R.string.acq_cardpay_pay, it.amount)

            // TODO Удалить if в рамках задачи EACQAPW-5066
            if (payButton.isEnabled != it.buttonEnabled) {
                payButton.isEnabled = it.buttonEnabled
            }
        }
    }

    private suspend fun processState() {
        viewModel.paymentProcessState.collect {
            handleLoadingInProcess(it is PaymentByCardState.Started)
            when (it) {
                is PaymentByCardState.Created -> statusSheetStatus.state = null
                is PaymentByCardState.Error -> {
                    statusSheetStatus.showIfNeed(supportFragmentManager).state =
                        PaymentStatusSheetState.Error(
                            title = R.string.acq_commonsheet_failed_title,
                            mainButton = R.string.acq_commonsheet_failed_primary_button,
                            throwable = it.throwable
                        )
                }
                is PaymentByCardState.Started -> Unit
                is PaymentByCardState.Success -> {
                    statusSheetStatus.showIfNeed(supportFragmentManager).state =
                        PaymentStatusSheetState.Success(
                            title = R.string.acq_commonsheet_paid_title,
                            mainButton = R.string.acq_commonsheet_clear_primarybutton,
                            paymentId = it.paymentId,
                            cardId = it.cardId,
                            rebillId = it.rebillId
                        )
                }
                is PaymentByCardState.ThreeDsUiNeeded -> {
                    try {
                        ThreeDsHelper.Launch.launchBrowserBased(
                            this,
                            TransparentActivity.THREE_DS_REQUEST_CODE,
                            it.paymentOptions,
                            it.threeDsState.data,
                        )
                    } catch (e: Throwable) {
                        statusSheetStatus.showIfNeed(supportFragmentManager).state =
                            PaymentStatusSheetState.Error(
                                title = R.string.acq_commonsheet_failed_title,
                                mainButton = R.string.acq_commonsheet_failed_primary_button,
                                throwable = e
                            )
                    } finally {
                        viewModel.goTo3ds()
                    }
                }
                is PaymentByCardState.ThreeDsInProcess -> statusSheetStatus.state = null
            }
        }
    }

    private suspend fun selectedCardState() {
        viewModel.state.collectLatest {
            savedCardOptions.featuresOptions { selectedCardId = it.chosenCard?.id }
        }
    }
    //endregion

    private fun handleLoadingInProcess(inProcess: Boolean) {
        payButton.isLoading = inProcess
        emailInput.enableEmail(inProcess.not())
        chosenCardComponent.enableCvc(inProcess.not())
        onBackEnabled = inProcess.not()

        if (inProcess) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }

    private fun finishWithSuccess(result: PaymentResult) {
        setResult(RESULT_OK, createSuccessIntent(result))
        finish()
    }
}
