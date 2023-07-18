package ru.tinkoff.acquiring.sdk.redesign.mainform.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.*
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.databinding.*
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ChooseCardLauncher
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.redesign.common.cardpay.CardPayComponent
import ru.tinkoff.acquiring.sdk.redesign.common.result.AcqPaymentResult
import ru.tinkoff.acquiring.sdk.redesign.common.util.AcqShimmerAnimator
import ru.tinkoff.acquiring.sdk.redesign.dialog.PaymentStatusSheetState
import ru.tinkoff.acquiring.sdk.redesign.dialog.component.PaymentStatusComponent
import ru.tinkoff.acquiring.sdk.redesign.mainform.MainFormLauncher
import ru.tinkoff.acquiring.sdk.redesign.mainform.navigation.MainFormNavController
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.vm.MainFormInputCardViewModel
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.vm.MainPaymentFormFactory
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.vm.MainPaymentFormViewModel
import ru.tinkoff.acquiring.sdk.redesign.mirpay.MirPayLauncher
import ru.tinkoff.acquiring.sdk.redesign.payment.PaymentByCardLauncher
import ru.tinkoff.acquiring.sdk.redesign.sbp.SbpPayLauncher
import ru.tinkoff.acquiring.sdk.redesign.tpay.TpayLauncher
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.ui.activities.TransparentActivity
import ru.tinkoff.acquiring.sdk.utils.*


/**
 * Created by i.golovachev
 */
internal class MainPaymentFormActivity : AppCompatActivity() {

    val options by lazyUnsafe { intent.getOptions<PaymentOptions>() }

    private val byNewCardPayment = registerForActivityResult(PaymentByCardLauncher.Contract) {
        when (it) {
            is PaymentByCardLauncher.Canceled -> Unit
            is PaymentByCardLauncher.Error -> {
                setResult(RESULT_ERROR, MainFormLauncher.Contract.createFailedIntent(it))
                finish()
            }
            is PaymentByCardLauncher.Success -> {
                setResult(
                    RESULT_OK,
                    MainFormLauncher.Contract.createSuccessIntent(it)
                )
                finish()
            }
        }
    }

    private val spbPayment = registerForActivityResult(SbpPayLauncher.Contract) {
        when (it) {
            is SbpPayLauncher.Canceled -> Unit
            is SbpPayLauncher.NoBanks -> Unit
            is SbpPayLauncher.Error -> {
                setResult(RESULT_ERROR, MainFormLauncher.Contract.createFailedIntent(it.error))
                finish()
            }
            is SbpPayLauncher.Success -> {
                setResult(RESULT_OK, MainFormLauncher.Contract.createSuccessIntent(it.payment))
                finish()
            }
        }
    }

    private val tpayPayment = registerForActivityResult(TpayLauncher.Contract) {
        when (it) {
            is TpayLauncher.Canceled -> {
                setResult(RESULT_CANCELED)
                finish()
            }
            is TpayLauncher.Error -> {
                viewModel.returnOnForm()
            }
            is TpayLauncher.Success -> {
                setResult(RESULT_OK, MainFormLauncher.Contract.createSuccessIntent(it))
                finish()
            }
        }
    }

    private val mirPayPayment = registerForActivityResult(MirPayLauncher.Contract) {
        when (it) {
            is MirPayLauncher.Canceled -> {
                setResult(RESULT_CANCELED)
                finish()
            }
            is MirPayLauncher.Error -> {
                viewModel.returnOnForm()
            }
            is MirPayLauncher.Success -> {
                setResult(RESULT_OK, MainFormLauncher.Contract.createSuccessIntent(it))
                finish()
            }
        }
    }

    private val savedCards = registerForActivityResult(ChooseCardLauncher.Contract) {
        when (it) {
            is ChooseCardLauncher.Canceled -> viewModel.loadState()
            is ChooseCardLauncher.Error -> Unit
            is ChooseCardLauncher.NeedInputNewCard -> {
                viewModel.toNewCard()
            }
            is ChooseCardLauncher.Success -> {
                viewModel.choseCard(it.card)
                cardInputViewModel.choseCard(it.card)
            }
        }
    }

    private val factory by lazyUnsafe { MainPaymentFormFactory(application, options) }
    private val viewModel: MainPaymentFormViewModel by viewModels { factory }
    private val cardInputViewModel: MainFormInputCardViewModel by viewModels { factory }

    private val root: CoordinatorLayout by lazyView(R.id.acq_main_form_root)
    private val shimmer: ViewGroup by lazyView(R.id.acq_main_form_loader)
    private val content: LinearLayout by lazyView(R.id.acq_main_form_content)
    private val amount: TextView by lazyView(R.id.acq_main_form_amount)
    private val sheet: NestedScrollView by lazyView(R.id.acq_main_form_sheet)

    private val bottomSheetComponent by lazyUnsafe {
        BottomSheetComponent(root, sheet) {
            viewModel.onBackPressed()
        }
    }

    private val primaryButtonComponent by lazyUnsafe {
        PrimaryButtonComponent(
            viewBinding = AcqMainFormPrimaryButtonComponentBinding.bind(
                findViewById(R.id.acq_main_form_primary_button)
            ),
            onMirPayClick = viewModel::toMirPay,
            onNewCardClick = viewModel::toNewCard,
            onSpbClick = viewModel::toSbp,
            onTpayClick = viewModel::toTpay,
            onPayClick = cardInputViewModel::pay
        )
    }

    private val cardPayComponent by lazyUnsafe {
        CardPayComponent(
            viewBinding = AcqCardPayComponentBinding.bind(
                findViewById(R.id.acq_main_card_pay)
            ),
            email = options.customer.email,
            onCvcCompleted = cardInputViewModel::setCvc,
            onEmailInput = cardInputViewModel::email,
            onEmailVisibleChange = cardInputViewModel::needEmail,
            onChooseCardClick = viewModel::toChooseCard,
            onPayClick = { cardInputViewModel.pay() }
        )
    }

    private val secondaryButtonComponent by lazyUnsafe {
        SecondaryBlockComponent(
            binding = AcqMainFormSecondaryBlockBinding.bind(
                findViewById(R.id.acq_main_form_secondary_button)
            ),
            onNewCardClick = viewModel::toPayCardOrNewCard,
            onSpbClick = viewModel::toSbp,
            onTpayClick = { viewModel.toTpay(false) },
            onMirPayClick = viewModel::toMirPay,
        )
    }

    private val paymentStatusComponent by lazyUnsafe {
        PaymentStatusComponent(
            viewBinding = AcqPaymentStatusFormBinding.bind(findViewById(R.id.acq_payment_status)),
            onMainButtonClick = { viewModel.onBackPressed() },
            onSecondButtonClick = { viewModel.onBackPressed() },
        )
    }

    private val errorStubComponent by lazyUnsafe {
        ErrorStubComponent(
            viewBinding = AcqMainFromErrorStubBinding.bind(findViewById(R.id.acq_main_from_error_stub)),
            onRetry = viewModel::onRetry
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_main_from_activity)
        createTitleView()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch { updateContent() }
        lifecycleScope.launch { updatePayEnable() }
        lifecycleScope.launch { updateButtonLoader() }
        lifecycleScope.launch { updatePrimary() }
        lifecycleScope.launch { updateSecondary() }
        lifecycleScope.launch { updateSavedCard() }
        lifecycleScope.launch { updateCardPayState() }
        lifecycleScope.launch {
            cardInputViewModel.savedCardFlow.collectLatest {
                cardPayComponent.renderNewCard(it)
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                subscribeOnNav()
            }
        }
    }

    override fun onAttachedToWindow() {
        bottomSheetComponent.onAttachedToWindow(this)
    }

    override fun onDetachedFromWindow() {
        bottomSheetComponent.onDetachWindow()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == TransparentActivity.THREE_DS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val result =
                    data.getSerializableExtra(ThreeDsHelper.Launch.RESULT_DATA) as PaymentResult
                cardInputViewModel.set3dsResult(result)
            } else if (resultCode == ThreeDsHelper.Launch.RESULT_ERROR) {
                cardInputViewModel.set3dsResult(
                    data?.getSerializableExtra(ThreeDsHelper.Launch.ERROR_DATA) as Throwable
                )
            } else {
                setResult(Activity.RESULT_CANCELED)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private suspend fun updateContent() {
        combine(
            cardInputViewModel.paymentStatus, viewModel.formContent
        ) { cardStatus, formContent ->
            if (cardStatus is PaymentStatusSheetState.Error ||
                cardStatus is PaymentStatusSheetState.Success
            ) {
                shimmer.isVisible = false
                content.isVisible = false
                errorStubComponent.isVisible(false)
                paymentStatusComponent.isVisible = true
                paymentStatusComponent.render(cardStatus)
                bottomSheetComponent.trimSheetToContent(paymentStatusComponent.viewBinding.root)
                bottomSheetComponent.collapse()

            } else {
                paymentStatusComponent.isVisible = false
                when (formContent) {
                    is MainPaymentFormViewModel.FormContent.Loading -> {
                        errorStubComponent.isVisible(false)
                        shimmer.isVisible = true
                        content.isVisible = false
                        bottomSheetComponent.trimSheetToContent(shimmer)
                        AcqShimmerAnimator.animateSequentially(shimmer.children.toList())
                    }
                    is MainPaymentFormViewModel.FormContent.Error -> {
                        shimmer.isVisible = false
                        content.isVisible = false
                        errorStubComponent.isVisible(true)
                        errorStubComponent.render(ErrorStubComponent.State.Error)
                        bottomSheetComponent.trimSheetToContent(errorStubComponent.root)
                        bottomSheetComponent.collapse()
                    }
                    is MainPaymentFormViewModel.FormContent.Content -> {
                        shimmer.isVisible = false
                        content.isVisible = true
                        errorStubComponent.isVisible(false)
                        cardPayComponent.isVisible(formContent.isSavedCard)
                        primaryButtonComponent.isVisible(formContent.isSavedCard.not())
                        bottomSheetComponent.trimSheetToContent(content)
                        bottomSheetComponent.collapse()
                    }
                    is MainPaymentFormViewModel.FormContent.NoNetwork -> {
                        shimmer.isVisible = false
                        content.isVisible = false
                        errorStubComponent.isVisible(true)
                        errorStubComponent.render(ErrorStubComponent.State.NoNetwork)
                        bottomSheetComponent.trimSheetToContent(errorStubComponent.root)
                        bottomSheetComponent.collapse()
                    }
                    is MainPaymentFormViewModel.FormContent.Hide -> {
                        shimmer.isVisible = false
                        content.isVisible = false
                        errorStubComponent.isVisible(false)
                        bottomSheetComponent.trimSheetToContent(paymentStatusComponent.viewBinding.root)
                        bottomSheetComponent.collapse()
                    }
                }
            }
        }.collect()
    }

    private suspend fun updatePrimary() = viewModel.primary.collect {
        primaryButtonComponent.render(it)
    }

    private suspend fun updateSecondary() = viewModel.secondary.collect {
        secondaryButtonComponent.render(it)
    }

    private suspend fun updateSavedCard() = viewModel.chosenCard.collect {
        cardInputViewModel.choseCard(it)
    }

    private suspend fun updatePayEnable() = cardInputViewModel.payEnable.collectLatest {
        cardPayComponent.renderEnable(it)
    }

    private suspend fun updateButtonLoader() = cardInputViewModel.isLoading.collectLatest {
        cardPayComponent.renderLoader(it)
        if (it) {
            cardPayComponent.isKeyboardVisible(false)
        }
        handleLoadingInProcess(it)
    }

    private suspend fun updateCardPayState() = with(cardInputViewModel) {
        combine(savedCardFlow, emailFlow) { card, email -> card to email }
            .take(1)
            .collectLatest { (card, email) ->
                cardPayComponent.render(card, email, options)
            }
    }

    private suspend fun subscribeOnNav() {
        viewModel.mainFormNav.collect {
            when (it) {
                is MainFormNavController.Navigation.ToChooseCard -> {
                    cardPayComponent.isKeyboardVisible(false)
                    savedCards.launch(it.savedCardsOptions)
                }
                is MainFormNavController.Navigation.ToPayByCard -> {
                    byNewCardPayment.launch(it.startData)
                }
                is MainFormNavController.Navigation.ToSbp -> {
                    spbPayment.launch(it.startData)
                }
                is MainFormNavController.Navigation.ToTpay -> {
                    tpayPayment.launch(it.startData)
                }
                is MainFormNavController.Navigation.ToMirPay -> {
                    mirPayPayment.launch(it.startData)
                }
                is MainFormNavController.Navigation.To3ds -> ThreeDsHelper.Launch.launchBrowserBased(
                    this,
                    TransparentActivity.THREE_DS_REQUEST_CODE,
                    it.paymentOptions,
                    it.threeDsState.data,
                )
                is MainFormNavController.Navigation.Return -> {
                    when (it.result) {
                        is AcqPaymentResult.Canceled -> setResult(RESULT_CANCELED)
                        is AcqPaymentResult.Error -> setResult(
                            RESULT_ERROR,
                            MainFormLauncher.Contract.createFailedIntent(it.result.error)
                        )
                        is AcqPaymentResult.Success -> setResult(
                            RESULT_OK,
                            MainFormLauncher.Contract.createSuccessIntent(it.result)
                        )
                    }
                    finish()
                }
                is MainFormNavController.Navigation.ToWebView -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.url)))
                }
                null -> Unit
            }
        }
    }

    private fun createTitleView() {
        amount.text = options.order.amount.toHumanReadableString()
    }

    private fun handleLoadingInProcess(inProcess: Boolean) {
        cardPayComponent.isEnable(inProcess.not())

        if (inProcess) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }

    internal companion object {
        fun intent(options: PaymentOptions, context: Context): Intent {
            val intent = Intent(context, MainPaymentFormActivity::class.java)
            intent.putOptions(options)
            return intent
        }
    }
}
