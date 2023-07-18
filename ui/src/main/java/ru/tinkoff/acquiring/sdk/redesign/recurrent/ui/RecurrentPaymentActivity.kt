package ru.tinkoff.acquiring.sdk.redesign.recurrent.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.databinding.AcqPaymentStatusFormBinding
import ru.tinkoff.acquiring.sdk.databinding.AcqRecurrentFromActivityBinding
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.redesign.common.cardpay.CardPayComponent
import ru.tinkoff.acquiring.sdk.redesign.dialog.PaymentStatusSheetState
import ru.tinkoff.acquiring.sdk.redesign.dialog.component.PaymentStatusComponent
import ru.tinkoff.acquiring.sdk.redesign.mainform.ui.BottomSheetComponent
import ru.tinkoff.acquiring.sdk.redesign.recurrent.RecurrentPayLauncher
import ru.tinkoff.acquiring.sdk.redesign.recurrent.presentation.RecurrentPaymentViewModel
import ru.tinkoff.acquiring.sdk.redesign.recurrent.presentation.RecurrentViewModelsFactory
import ru.tinkoff.acquiring.sdk.redesign.recurrent.presentation.RejectedViewModel
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.ui.activities.TransparentActivity
import ru.tinkoff.acquiring.sdk.utils.getOptions
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.utils.putOptions

/**
 * Created by i.golovachev
 */
// TODO Вынести навигацию в отдельный класс.
internal class RecurrentPaymentActivity : AppCompatActivity() {

    private val paymentOptions by lazyUnsafe { intent.getOptions<PaymentOptions>() }
    private val factory by lazyUnsafe { RecurrentViewModelsFactory(application, paymentOptions) }
    private val recurrentPaymentViewModel: RecurrentPaymentViewModel by viewModels { factory }
    private val rejectedViewModel: RejectedViewModel by viewModels { factory }
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

    private val cardPayComponent by lazyUnsafe {
        CardPayComponent(
            viewBinding = binding.acqRecurrentFormPay,
            email = null,
            onCvcCompleted = rejectedViewModel::inputCvc,
            onPayClick = rejectedViewModel::payRejected
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindView()

        updatePaymentState()
        updateCvcValidState()
        updateLoadingState()
        updateForceHideKeyboard()
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun updatePaymentState() = lifecycleScope.launch {
        combine(
            recurrentPaymentViewModel.state.filterNotNull(), rejectedViewModel.needInputCvcState
        ) { state, needCvcForCard -> state to needCvcForCard }
            .collectLatest {  (state, needCvcForCard) ->
                paymentStatusComponent.isVisible = needCvcForCard == null
                paymentStatusComponent.render(state)
                cardPayComponent.isVisible(needCvcForCard != null)
                if (needCvcForCard != null) {
                    cardPayComponent.renderInputCvc(needCvcForCard, paymentOptions)
                    bottomSheetComponent.trimSheetToContent(binding.acqRecurrentFormPayContainer)
                } else {
                    bottomSheetComponent.trimSheetToContent(paymentStatusComponent.viewBinding.root)
                }

            }
    }

    private fun updateCvcValidState() = lifecycleScope.launch {
        rejectedViewModel.cvcValid.collectLatest {
            cardPayComponent.renderEnable(it)
        }
    }

    private fun updateLoadingState() = lifecycleScope.launch {
        rejectedViewModel.loaderButtonState.collectLatest {
            cardPayComponent.renderLoader(it)
        }
    }

    private fun updateForceHideKeyboard() = lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            rejectedViewModel.needHideKeyboard.filter { it }.collectLatest {
                cardPayComponent.isKeyboardVisible(false)
            }
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
                            RecurrentPayLauncher.Contract.createFailedIntent(it.throwable)
                        )
                        finish()
                    }
                    is RecurrentPaymentEvent.CloseWithSuccess -> {
                        setResult(
                            RESULT_OK,
                            RecurrentPayLauncher.Contract.createSuccessIntent(it.paymentId, it.rebillId)
                        )
                        finish()
                    }
                    is RecurrentPaymentEvent.To3ds ->  {
                        tryLaunch3ds(it)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == TransparentActivity.THREE_DS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val result =
                    data.getSerializableExtra(ThreeDsHelper.Launch.RESULT_DATA) as PaymentResult
                recurrentPaymentViewModel.set3dsResult(result)
            } else if (resultCode == ThreeDsHelper.Launch.RESULT_ERROR) {
                recurrentPaymentViewModel.set3dsResult(
                    data?.getSerializableExtra(ThreeDsHelper.Launch.ERROR_DATA) as Throwable
                )
            } else {
               recurrentPaymentViewModel.onClose()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun tryLaunch3ds(it: RecurrentPaymentEvent.To3ds) {
        try {
            ThreeDsHelper.Launch.launchBrowserBased(
                this@RecurrentPaymentActivity,
                TransparentActivity.THREE_DS_REQUEST_CODE,
                it.paymentOptions,
                it.threeDsState.data,
            )
        } catch (e: Throwable) {
            paymentStatusComponent.render(
                PaymentStatusSheetState.Error(
                    title = R.string.acq_commonsheet_failed_title,
                    mainButton = R.string.acq_commonsheet_failed_primary_button,
                    throwable = e
                )
            )
        } finally {
            recurrentPaymentViewModel.goTo3ds()
        }
    }

    companion object {

        internal const val EXTRA_CARD = "extra_card"

        fun intent(context: Context, paymentOptions: PaymentOptions, card: Card): Intent {
            return Intent(context, RecurrentPaymentActivity::class.java).apply {
                putOptions(paymentOptions)
                putExtra(EXTRA_CARD, card)
            }
        }
    }
}
