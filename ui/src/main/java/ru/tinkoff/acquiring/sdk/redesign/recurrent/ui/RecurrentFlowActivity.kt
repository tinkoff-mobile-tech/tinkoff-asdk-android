package ru.tinkoff.acquiring.sdk.redesign.recurrent.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.databinding.AcqPaymentStatusFormBinding
import ru.tinkoff.acquiring.sdk.databinding.AcqRecurrentFromActivityBinding
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.redesign.common.cardpay.CardPayComponent
import ru.tinkoff.acquiring.sdk.redesign.dialog.component.PaymentStatusComponent
import ru.tinkoff.acquiring.sdk.redesign.mainform.navigation.MainFormContract
import ru.tinkoff.acquiring.sdk.redesign.mainform.ui.BottomSheetComponent
import ru.tinkoff.acquiring.sdk.redesign.payment.ui.PaymentByCard
import ru.tinkoff.acquiring.sdk.utils.getOptions
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.utils.putOptions

/**
 * Created by i.golovachev
 */
// TODO Вынести навигацию в отдельный класс.
internal class RecurrentPaymentActivity : AppCompatActivity() {

    private val paymentOptions by lazyUnsafe { intent.getOptions<PaymentOptions>() }
    private val savedCardOptions: SavedCardsOptions by lazyUnsafe {
        SavedCardsOptions().apply {
            setTerminalParams(
                paymentOptions.terminalKey,
                paymentOptions.publicKey
            )
            paymentOptions.featuresOptions {
                showOnlyRecurrentCards = true
            }
            customer = paymentOptions.customer
            features.showOnlyRecurrentCards = true
        }
    }
    private val recurrentPaymentViewModel: RecurrentPaymentViewModel by viewModels {
        RecurrentPaymentViewModel.factory(application, paymentOptions)
    }
    private val savedCards = registerForActivityResult(TinkoffAcquiring.ChoseCard.Contract) {
        when (it) {
            is TinkoffAcquiring.ChoseCard.Canceled -> {
                setResult(RESULT_CANCELED)
                finish()
            }
            is TinkoffAcquiring.ChoseCard.Error -> {
                setResult(
                    TinkoffAcquiring.RESULT_ERROR,
                    RecurrentPayment.Contract.createFailedIntent(it.error, null)
                )
                finish()
            }
            is TinkoffAcquiring.ChoseCard.NeedInputNewCard -> {
                launchByNewCard()
            }
            is TinkoffAcquiring.ChoseCard.Success -> {
                recurrentPaymentViewModel.pay(it.card)
            }
        }
    }
    private val byNewCardPayment = registerForActivityResult(PaymentByCard.Contract) {
        when (it) {
            is PaymentByCard.Canceled -> {
                setResult(RESULT_CANCELED)
                finish()
            }
            is PaymentByCard.Error -> {
                setResult(
                    TinkoffAcquiring.RESULT_ERROR,
                    MainFormContract.Contract.createFailedIntent(it)
                )
                finish()
            }
            is PaymentByCard.Success -> {
                setResult(
                    RESULT_OK,
                    MainFormContract.Contract.createSuccessIntent(it)
                )
                finish()
            }
        }
    }
    private lateinit var binding : AcqRecurrentFromActivityBinding
    private val paymentStatusComponent by lazyUnsafe {
        PaymentStatusComponent(
            viewBinding = AcqPaymentStatusFormBinding.bind(findViewById(R.id.acq_payment_status)),
            onMainButtonClick = {  },
            onSecondButtonClick = { },
        )
    }
    private val bottomSheetComponent by lazyUnsafe {
        BottomSheetComponent(binding.root, binding.acqRecurrentFormSheet) {

        }
    }
    private val cardPayComponent by lazyUnsafe {
        CardPayComponent(
            viewBinding = binding.acqRecurrentFormPay,
            email = null,
            onCvcCompleted = {},
            onPayClick = {  }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindView()
        launchSavedCards()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        updatePaymentState()
    }

    private fun bindView() {
        binding = AcqRecurrentFromActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun launchSavedCards() {
        savedCards.launch(savedCardOptions)
    }

    private fun launchByNewCard() {
        byNewCardPayment.launch(PaymentByCard.StartData(paymentOptions, arrayListOf()))
    }

    private fun updatePaymentState() = lifecycleScope.launch {
        recurrentPaymentViewModel.state
            .filterNotNull()
            .collectLatest(paymentStatusComponent::render)
    }

    companion object {
        fun intent(context: Context, paymentOptions: PaymentOptions): Intent {
            return Intent(context, RecurrentPaymentActivity::class.java).apply {
                putOptions(paymentOptions)
            }
        }
    }
}