package ru.tinkoff.acquiring.sdk.redesign.mirpay.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenResumed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.databinding.AcqMirPayActivityBinding
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_CARD_ID
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_ERROR
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_PAYMENT_ID
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_REBILL_ID
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_START_DATA
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.redesign.common.util.openDeepLink
import ru.tinkoff.acquiring.sdk.redesign.dialog.component.PaymentStatusComponent
import ru.tinkoff.acquiring.sdk.redesign.mainform.ui.BottomSheetComponent
import ru.tinkoff.acquiring.sdk.redesign.mirpay.MirPayLauncher
import ru.tinkoff.acquiring.sdk.redesign.mirpay.MirPayLauncher.StartData
import ru.tinkoff.acquiring.sdk.redesign.mirpay.nav.MirPayNavigation
import ru.tinkoff.acquiring.sdk.redesign.mirpay.presentation.MirPayViewModel
import ru.tinkoff.acquiring.sdk.utils.getParcelable
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe

/**
 * @author k.shpakovskiy
 */
internal class MirPayFlowActivity : AppCompatActivity() {

    private lateinit var binding: AcqMirPayActivityBinding

    private val startData by lazyUnsafe {
        intent.getParcelable(EXTRA_START_DATA, StartData::class)
    }

    private val viewModel: MirPayViewModel by viewModels {
        MirPayViewModel.factory(application, startData.paymentOptions)
    }

    private val paymentStatusComponent by lazyUnsafe {
        PaymentStatusComponent(
            viewBinding = binding.acqPaymentStatus,
            onMainButtonClick = { viewModel.onClose() },
            onSecondButtonClick = { viewModel.onClose() },
        )
    }

    private val bottomSheetComponent by lazyUnsafe {
        BottomSheetComponent(binding.root, binding.acqMirPayFormSheet) {
            viewModel.onClose()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindView()

        updateSheetState()
        subscribeOnEvents()

        if (savedInstanceState == null) {
            viewModel.pay()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startCheckingStatus()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        viewModel.onClose()
    }

    private fun bindView() {
        binding = AcqMirPayActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun updateSheetState() = lifecycleScope.launch {
        viewModel.state.collectLatest {
            paymentStatusComponent.render(it)
            bottomSheetComponent.trimSheetToContent(binding.acqMirPayFormSheet)
        }
    }

    private fun subscribeOnEvents() = lifecycleScope.launch {
        whenResumed {
            viewModel.navEvent.collectLatest {
                when (it) {
                    is MirPayNavigation.Event.GoToMirPay -> {
                        viewModel.goingToBankApp()
                        openDeepLink(MIR_PAY_REQUEST_CODE, it.deeplink)
                    }
                    is MirPayNavigation.Event.Close -> setResult(it.result)
                }
            }
        }
    }

    private fun setResult(result: MirPayLauncher.Result) {
        val intent = Intent()
        when (result) {
            MirPayLauncher.Canceled -> setResult(Activity.RESULT_CANCELED)
            is MirPayLauncher.Error -> {
                intent.putExtra(EXTRA_ERROR, result.error)
                setResult(RESULT_ERROR, intent)
            }
            is MirPayLauncher.Success -> {
                with(intent) {
                    putExtra(EXTRA_PAYMENT_ID, result.paymentId ?: -1)
                    putExtra(EXTRA_CARD_ID, result.cardId)
                    putExtra(EXTRA_REBILL_ID, result.rebillId)
                }
                setResult(RESULT_OK, intent)
            }
        }
        finish()
    }

    companion object {
        const val MIR_PAY_REQUEST_CODE = 105
    }
}
