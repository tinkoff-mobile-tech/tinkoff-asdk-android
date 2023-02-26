package ru.tinkoff.acquiring.sdk.redesign.mainform

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ViewFlipper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.databinding.AcqCardPayComponentBinding
import ru.tinkoff.acquiring.sdk.databinding.AcqMainFormPrimaryButtonComponentBinding
import ru.tinkoff.acquiring.sdk.databinding.AcqMainFormSecondaryBlockBinding
import ru.tinkoff.acquiring.sdk.databinding.AcqPaymentStatusFormBinding
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.common.cardpay.CardPayComponent
import ru.tinkoff.acquiring.sdk.redesign.dialog.PaymentStatusSheetState
import ru.tinkoff.acquiring.sdk.redesign.dialog.component.PaymentStatusComponent
import ru.tinkoff.acquiring.sdk.redesign.mainform.navigation.MainFormNavController
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.vm.MainFormInputCardViewModel
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.vm.MainPaymentFormFactory
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.vm.MainPaymentFormViewModel
import ru.tinkoff.acquiring.sdk.redesign.mainform.ui.PrimaryButtonComponent
import ru.tinkoff.acquiring.sdk.redesign.mainform.ui.SecondaryBlockComponent
import ru.tinkoff.acquiring.sdk.redesign.payment.ui.PaymentByCard
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.ui.activities.TransparentActivity
import ru.tinkoff.acquiring.sdk.utils.*
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.utils.lazyView

/**
 * Created by i.golovachev
 */
// rework after desing main form
class MainPaymentFormStub : AppCompatActivity() {

    val options by lazyUnsafe { intent.getOptions<PaymentOptions>() }

    private val byNewCardPayment = registerForActivityResult(PaymentByCard.Contract) {}
    private val spbPayment = registerForActivityResult(TinkoffAcquiring.SbpScreen.Contract) {}
    private val savedCards = registerForActivityResult(TinkoffAcquiring.ChoseCard.Contract) {}

    private val factory by lazyUnsafe { MainPaymentFormFactory(application, options) }
    private val viewModel: MainPaymentFormViewModel by viewModels { factory }
    private val cardInputViewModel: MainFormInputCardViewModel by viewModels { factory }

    private val flipper: ViewFlipper by lazyView(R.id.acq_main_form_flipper)

    private val primaryButtonComponent by lazyUnsafe {
        PrimaryButtonComponent(
            viewBinding = AcqMainFormPrimaryButtonComponentBinding.bind(
                findViewById(R.id.acq_main_form_primary_button)
            ),
            onNewCardClick = viewModel::toNewCard,
            onSpbClick = viewModel::toSbp,
            onTpayClick = viewModel::toTpay,
            onPayClick = cardInputViewModel::pay
        )
    }

    private val cardPayComponent by lazyUnsafe {
        CardPayComponent(
            viewBinding = AcqCardPayComponentBinding.bind(
                findViewById(R.id.acq_main_form_primary_button)
            ),
            email = options.customer.email,
            onCvcCompleted = cardInputViewModel::setCvc,
            onEmailInput = cardInputViewModel::email,
            onEmailVisibleChange = cardInputViewModel::needEmail,
            onChooseCardClick = viewModel::toChooseCard,
        )
    }

    private val secondaryButtonComponent by lazyUnsafe {
        SecondaryBlockComponent(
            binding = AcqMainFormSecondaryBlockBinding.bind(
                findViewById(R.id.acq_main_form_secondary_button)
            ),
            onNewCardClick = viewModel::toNewCard,
            onSpbClick = viewModel::toSbp,
            onTpayClick = viewModel::toTpay,
        )
    }

    private val paymentStatusComponent by lazyUnsafe {
        PaymentStatusComponent(
            viewBinding = AcqPaymentStatusFormBinding.bind(findViewById(R.id.acq_payment_status))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_main_from_activity)

        lifecycleScope.launch { updateContent() }
        lifecycleScope.launch { updatePayEnable() }
        lifecycleScope.launch { updateButtonLoader() }
        lifecycleScope.launch { updatePrimary() }
        lifecycleScope.launch { updateSecondary() }
        lifecycleScope.launch { updateSavedCard() }
        lifecycleScope.launch { updateCardPayState() }

        lifecycleScope.launch { subscribeOnNav() }
    }

    private suspend fun updateContent() {
        combine(cardInputViewModel.paymentStatus, viewModel.formContent) { cardStatus, formContent ->
            if (cardStatus is PaymentStatusSheetState.NotYet &&
                cardStatus is PaymentStatusSheetState.Hide) {
                when (cardStatus) {
                    PaymentStatusSheetState.Hide -> Unit
                    PaymentStatusSheetState.NotYet -> Unit
                    is PaymentStatusSheetState.Error -> {
                        flipper.showById(R.id.acq_payment_status)
                        paymentStatusComponent.render(cardStatus)
                    }
                    is PaymentStatusSheetState.Progress -> Unit
                    is PaymentStatusSheetState.Success -> {
                        flipper.showById(R.id.acq_payment_status)
                        paymentStatusComponent.render(cardStatus)
                    }
                }
            } else {
                when (formContent) {
                    is MainPaymentFormViewModel.FormContent.Loading -> flipper.showById(R.id.acq_main_form_loader)
                    is MainPaymentFormViewModel.FormContent.Error -> flipper.showById(R.id.acq_main_form_loader)
                    is MainPaymentFormViewModel.FormContent.Content -> if (formContent.isSavedCard)
                        flipper.showById(R.id.acq_main_card_pay)
                    else
                        flipper.showById(R.id.acq_main_form_primary_button)
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
    }

    private suspend fun updateCardPayState() = with(cardInputViewModel) {
        combine(savedCardFlow, emailFlow) { card, email -> card to email }.take(1)
            .collectLatest { (card, email) -> cardPayComponent.render(card, email) }
    }

    private suspend fun subscribeOnNav() {
        viewModel.mainFormNav.collect {
            when (it) {
                is MainFormNavController.Navigation.ToChooseCard -> savedCards.launch(it.savedCardsOptions)
                is MainFormNavController.Navigation.ToPayByCard -> byNewCardPayment.launch(it.startData)
                is MainFormNavController.Navigation.ToSbp -> spbPayment.launch(it.startData)
                is MainFormNavController.Navigation.ToTpay -> {
                    // todo tinkoff
                }
                is MainFormNavController.Navigation.To3ds -> ThreeDsHelper.Launch.launchBrowserBased(
                    this,
                    TransparentActivity.THREE_DS_REQUEST_CODE,
                    it.paymentOptions,
                    it.threeDsState.data,
                )
            }
        }
    }

    companion object {
        fun intent(options: PaymentOptions, context: Context): Intent {
            val intent = Intent(context, MainPaymentFormStub::class.java)
            intent.putOptions(options)
            return intent
        }
    }
}