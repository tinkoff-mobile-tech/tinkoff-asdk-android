package ru.tinkoff.acquiring.sdk.redesign.recurrent.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring.Companion.EXTRA_REBILL_ID
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring.Companion.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.databinding.AcqPaymentStatusFormBinding
import ru.tinkoff.acquiring.sdk.databinding.AcqRecurrentFromActivityBinding
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.common.cardpay.CardPayComponent
import ru.tinkoff.acquiring.sdk.redesign.dialog.component.PaymentStatusComponent
import ru.tinkoff.acquiring.sdk.redesign.mainform.ui.BottomSheetComponent
import ru.tinkoff.acquiring.sdk.utils.getOptions
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.utils.putOptions

/**
 * Created by i.golovachev
 */
// TODO Вынести навигацию в отдельный класс.
internal class RecurrentPaymentActivity : AppCompatActivity() {

    private val paymentOptions by lazyUnsafe { intent.getOptions<PaymentOptions>() }
    private val recurrentPaymentViewModel: RecurrentPaymentViewModel by viewModels {
        RecurrentPaymentViewModel.factory(application, paymentOptions)
    }
    private lateinit var binding: AcqRecurrentFromActivityBinding
    private val paymentStatusComponent by lazyUnsafe {
        PaymentStatusComponent(
            viewBinding = AcqPaymentStatusFormBinding.bind(findViewById(R.id.acq_payment_status)),
            onMainButtonClick = { recurrentPaymentViewModel.onClose() },
            onSecondButtonClick = { recurrentPaymentViewModel.onClose() },
        )
    }

    private val bottomSheetComponent by lazyUnsafe {
        BottomSheetComponent(binding.root, binding.acqRecurrentFormSheet) {}
    }

    // TODO в другом реквесте со 104
    private val cardPayComponent by lazyUnsafe {
        CardPayComponent(
            viewBinding = binding.acqRecurrentFormPay,
            email = null,
            onCvcCompleted = { },
            onPayClick = { }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindView()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        updatePaymentState()
        subscribeOnEvents()

        if (savedInstanceState == null) {
            recurrentPaymentViewModel.pay()
        }
    }

    override fun onAttachedToWindow() {
        bottomSheetComponent.onAttachedToWindow(this)
    }

    override fun onDetachedFromWindow() {
        bottomSheetComponent.onDetachWindow()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        recurrentPaymentViewModel.onClose()
    }

    private fun bindView() {
        binding = AcqRecurrentFromActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun updatePaymentState() = lifecycleScope.launch {
        recurrentPaymentViewModel.state
            .filterNotNull()
            .collectLatest {
                paymentStatusComponent.isVisible = true
                paymentStatusComponent.render(it)
                bottomSheetComponent.trimSheetToContent(paymentStatusComponent.viewBinding.root)
            }
    }

    private fun subscribeOnEvents() = lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            recurrentPaymentViewModel.events.collectLatest {
                when (it) {
                    is RecurrentPaymentEvent.CloseWithCancel -> {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                    is RecurrentPaymentEvent.CloseWithError -> {
                        setResult(
                            RESULT_ERROR,
                            RecurrentPayment.Contract.createFailedIntent(it.throwable)
                        )
                        finish()
                    }
                    is RecurrentPaymentEvent.CloseWithSuccess -> {
                        setResult(
                            RESULT_OK,
                            RecurrentPayment.Contract.createSuccessIntent(it.paymentId, it.rebillId)
                        )
                        finish()
                    }
                }
            }
        }
    }

    companion object {
        fun intent(context: Context, paymentOptions: PaymentOptions, rebillId: String): Intent {
            return Intent(context, RecurrentPaymentActivity::class.java).apply {
                putOptions(paymentOptions)
                putExtra(EXTRA_REBILL_ID, rebillId)
            }
        }
    }
}