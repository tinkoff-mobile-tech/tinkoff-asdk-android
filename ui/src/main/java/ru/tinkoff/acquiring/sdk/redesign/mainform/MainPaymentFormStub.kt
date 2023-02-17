package ru.tinkoff.acquiring.sdk.redesign.mainform

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ViewFlipper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.databinding.AcqMainFormPrimaryButtonComponentBinding
import ru.tinkoff.acquiring.sdk.databinding.AcqMainFormSecondaryBlockBinding
import ru.tinkoff.acquiring.sdk.databinding.AcqMainFormSecondaryButtonBinding
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.mainform.navigation.MainFormNavController
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormViewModel
import ru.tinkoff.acquiring.sdk.redesign.mainform.ui.PrimaryButtonComponent
import ru.tinkoff.acquiring.sdk.redesign.mainform.ui.SecondaryBlockComponent
import ru.tinkoff.acquiring.sdk.redesign.mainform.ui.SecondaryButtonComponent
import ru.tinkoff.acquiring.sdk.redesign.payment.ui.PaymentByCard
import ru.tinkoff.acquiring.sdk.ui.component.bindKtx
import ru.tinkoff.acquiring.sdk.utils.*
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.utils.lazyView

/**
 * Created by i.golovachev
 */
// rework after desing main form
class MainPaymentFormStub : AppCompatActivity() {

    val options by lazyUnsafe { intent.getOptions<PaymentOptions>() }

    private val byNewCardPayment =
        registerForActivityResult(PaymentByCard.Contract) {
        }
    private val spbPayment =
        registerForActivityResult(TinkoffAcquiring.SbpScreen.Contract) {
        }
    private val savedCards =
        registerForActivityResult(TinkoffAcquiring.ChoseCard.Contract) {
        }

    private val viewModel: MainPaymentFormViewModel by viewModels {
        MainPaymentFormViewModel.factory(application)
    }

    private val flipper: ViewFlipper by lazyView(R.id.acq_main_form_flipper)

    private val primaryButtonComponent by lazyUnsafe {
        PrimaryButtonComponent(
            viewBinding = AcqMainFormPrimaryButtonComponentBinding.bind(
                findViewById(R.id.acq_main_form_primary_button)
            ),
            email = options.customer.email,
            onCvcCompleted = viewModel::setCvc,
            onEmailInput = viewModel::email,
            onEmailVisibleChange = viewModel::needEmail,
            onNewCardClick = viewModel::toNewCard,
            onSpbClick = viewModel::toSbp,
            onTpayClick = viewModel::toTpay,
            onChooseCardClick = viewModel::toChooseCard
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_main_from_activity)

        lifecycleScope.launch { updateContent() }
        lifecycleScope.launch { updatePayEnable() }
        lifecycleScope.launch { subscribeOnNav() }
    }


    private suspend fun updateContent() {
        viewModel.stateFlow.collect { state ->
            when (state) {
                is MainPaymentFormViewModel.State.Loading -> flipper.showById(R.id.acq_main_form_loader)
                is MainPaymentFormViewModel.State.Error -> flipper.showById(R.id.acq_main_form_loader)
                is MainPaymentFormViewModel.State.Content -> {
                    flipper.showById(R.id.acq_main_form_primary_button)
                    primaryButtonComponent.render(state = state.ui.primary)
                    secondaryButtonComponent.render(state = state.ui.secondaries)
                }
            }
        }
    }

    private suspend fun updatePayEnable() {
        viewModel.payEnable.collectLatest(primaryButtonComponent::renderEnable)
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