package ru.tinkoff.acquiring.sdk.redesign.tpay.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenResumed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.databinding.AcqTpayActivityBinding
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_START_DATA
import ru.tinkoff.acquiring.sdk.redesign.dialog.component.PaymentStatusComponent
import ru.tinkoff.acquiring.sdk.redesign.mainform.ui.BottomSheetComponent
import ru.tinkoff.acquiring.sdk.redesign.tpay.TpayLauncher.StartData
import ru.tinkoff.acquiring.sdk.redesign.tpay.TpayLauncher.setResult
import ru.tinkoff.acquiring.sdk.redesign.tpay.nav.TpayNavigation
import ru.tinkoff.acquiring.sdk.redesign.tpay.presentation.TpayViewModel
import ru.tinkoff.acquiring.sdk.redesign.tpay.util.TpayHelper
import ru.tinkoff.acquiring.sdk.utils.getParcelable
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe

/**
 * Created by i.golovachev
 */
internal class TpayFlowActivity : AppCompatActivity() {

    private lateinit var binding: AcqTpayActivityBinding

    private val startData by lazyUnsafe {
        intent.getParcelable(EXTRA_START_DATA, StartData::class)
    }

    private val viewModel: TpayViewModel by viewModels {
        TpayViewModel.factory(application, startData.paymentOptions)
    }

    private val paymentStatusComponent by lazyUnsafe {
        PaymentStatusComponent(
            viewBinding = binding.acqPaymentStatus,
            onMainButtonClick = { viewModel.onClose() },
            onSecondButtonClick = { viewModel.onClose() },
        )
    }

    private val bottomSheetComponent by lazyUnsafe {
        BottomSheetComponent(binding.root, binding.acqTpayFormSheet) {
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
        binding = AcqTpayActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun updateSheetState() = lifecycleScope.launch {
        viewModel.state.collectLatest {
            paymentStatusComponent.render(it)
            bottomSheetComponent.trimSheetToContent(binding.acqTpayFormSheet)
        }
    }

    private fun subscribeOnEvents() = lifecycleScope.launch {
        whenResumed {
            viewModel.navEvent.collectLatest {
                when (it) {
                    is TpayNavigation.Event.GoToTinkoff -> {
                        TpayHelper.openTpayDeeplink(
                            it.deeplink,
                            this@TpayFlowActivity
                        )
                        viewModel.goingToBankApp()
                    }
                    is TpayNavigation.Event.Close -> setResult(it.result)
                }
            }
        }
    }
}
