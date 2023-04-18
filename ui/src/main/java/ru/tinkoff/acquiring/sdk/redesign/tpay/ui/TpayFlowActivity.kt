package ru.tinkoff.acquiring.sdk.redesign.tpay.ui

import android.app.Application
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.databinding.AcqTpayActivityBinding
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkTimeoutException
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.TpayPaymentState
import ru.tinkoff.acquiring.sdk.payment.TpayProcess
import ru.tinkoff.acquiring.sdk.redesign.dialog.PaymentStatusSheetState
import ru.tinkoff.acquiring.sdk.redesign.dialog.component.PaymentStatusComponent
import ru.tinkoff.acquiring.sdk.redesign.mainform.ui.BottomSheetComponent
import ru.tinkoff.acquiring.sdk.redesign.tpay.Tpay
import ru.tinkoff.acquiring.sdk.redesign.tpay.Tpay.Contract.EXTRA_START_DATA
import ru.tinkoff.acquiring.sdk.redesign.tpay.Tpay.setResult
import ru.tinkoff.acquiring.sdk.redesign.tpay.util.TpayHelper
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe

/**
 * Created by i.golovachev
 */
internal class TpayFlowActivity : AppCompatActivity() {

    private lateinit var binding: AcqTpayActivityBinding

    private val startData by lazyUnsafe {
        checkNotNull(intent.getParcelableExtra<Tpay.StartData>(EXTRA_START_DATA))
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

    override fun onStart() {
        super.onStart()
        viewModel.startCheckingStatus()
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
                        viewModel.goingToBankApp()
                        TpayHelper.openTpayDeeplink(
                            it.deeplink,
                            this@TpayFlowActivity
                        )
                    }
                    is TpayNavigation.Event.Close -> setResult(it.result)
                }
            }
        }
    }

}


internal class TpayViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val tpayProcess: TpayProcess,
    private val tpayProcessMapper: TpayProcessMapper,
    private val tpayNavigation: TpayNavigation
) : ViewModel() {

    private val startData: Tpay.StartData = checkNotNull(savedStateHandle[EXTRA_START_DATA])
    val state = tpayProcess.state.mapNotNull { tpayProcessMapper.mapState(it) }
    val navEvent = tpayNavigation.flow

    init {
        tpayProcess.state.mapNotNull { tpayProcessMapper.mapEvent(it) }
            .onEach { tpayNavigation.send(it) }
            .launchIn(viewModelScope)
    }

    fun pay() {
        tpayProcess.start(
            paymentOptions = startData.paymentOptions,
            tpayVersion = startData.version,
            paymentId = startData.paymentId
        )
    }

    fun goingToBankApp() {
        tpayProcess.goingToBankApp()
    }

    fun startCheckingStatus() {
        tpayProcess.startCheckingStatus(2)
    }

    fun onClose() {
        viewModelScope.launch {
            tpayProcessMapper.mapResult(tpayProcess.state.value)?.let {
                tpayNavigation.send(TpayNavigation.Event.Close(it))
            }
        }
    }

    companion object {
        fun factory(application: Application, paymentOptions: PaymentOptions) = viewModelFactory {
            val acq = TinkoffAcquiring(
                application, paymentOptions.terminalKey, paymentOptions.publicKey
            )
            initializer {
                TpayViewModel(
                    createSavedStateHandle(),
                    TpayProcess(acq.sdk),
                    TpayProcessMapper(),
                    TpayNavigation()
                )
            }
        }
    }
}

internal class TpayProcessMapper() {

    fun mapState(it: TpayPaymentState): PaymentStatusSheetState? {
        return when (it) {
            TpayPaymentState.Created -> PaymentStatusSheetState.Progress(
                title = R.string.acq_commonsheet_processing_title,
                subtitle = R.string.acq_commonsheet_processing_description
            )
            is TpayPaymentState.PaymentFailed -> if (it.throwable is AcquiringSdkTimeoutException) {
                PaymentStatusSheetState.Error(
                    title = R.string.acq_commonsheet_timeout_failed_title,
                    subtitle = R.string.acq_commonsheet_timeout_failed_description,
                    throwable = it.throwable,
                    secondButton = R.string.acq_commonsheet_timeout_failed_flat_button
                )
            } else {
                PaymentStatusSheetState.Error(
                    title = R.string.acq_commonsheet_failed_title,
                    subtitle = R.string.acq_commonsheet_failed_description,
                    throwable = it.throwable,
                    mainButton = R.string.acq_commonsheet_failed_primary_button
                )
            }
            is TpayPaymentState.Started -> PaymentStatusSheetState.Progress(
                title = R.string.acq_commonsheet_processing_title,
                subtitle = R.string.acq_commonsheet_processing_description
            )
            is TpayPaymentState.Success -> PaymentStatusSheetState.Success(
                title = R.string.acq_commonsheet_paid_title,
                mainButton = R.string.acq_commonsheet_clear_primarybutton,
                paymentId = it.paymentId,
                cardId = it.cardId,
                rebillId = it.rebillId
            )
            is TpayPaymentState.NeedChooseOnUi -> {
                null
            }
            is TpayPaymentState.CheckingStatus -> {
                val status = it.status
                if (status == ResponseStatus.FORM_SHOWED) {
                    PaymentStatusSheetState.Progress(
                        title = R.string.acq_commonsheet_payment_waiting_title,
                        secondButton = R.string.acq_commonsheet_payment_waiting_flat_button
                    )
                } else {
                    PaymentStatusSheetState.Progress(
                        title = R.string.acq_commonsheet_processing_title,
                        subtitle = R.string.acq_commonsheet_processing_description
                    )
                }
            }
            is TpayPaymentState.LeaveOnBankApp -> PaymentStatusSheetState.Progress(
                title = R.string.acq_commonsheet_payment_waiting_title,
                secondButton = R.string.acq_commonsheet_payment_waiting_flat_button
            )
            is TpayPaymentState.Stopped -> null
        }
    }

    fun mapEvent(it: TpayPaymentState): TpayNavigation.Event? {
        return when (it) {
            is TpayPaymentState.NeedChooseOnUi -> TpayNavigation.Event.GoToTinkoff(it.deeplink)
            is TpayPaymentState.CheckingStatus,
            is TpayPaymentState.Created,
            is TpayPaymentState.LeaveOnBankApp,
            is TpayPaymentState.PaymentFailed,
            is TpayPaymentState.Started,
            is TpayPaymentState.Stopped,
            is TpayPaymentState.Success -> null
        }
    }

    fun mapResult(it: TpayPaymentState): Tpay.Result? {
        return when (it) {
            is TpayPaymentState.Started,
            is TpayPaymentState.LeaveOnBankApp,
            is TpayPaymentState.NeedChooseOnUi,
            is TpayPaymentState.CheckingStatus -> null
            is TpayPaymentState.Created,
            is TpayPaymentState.Stopped -> Tpay.Canceled
            is TpayPaymentState.PaymentFailed -> Tpay.Error(it.throwable, null)
            is TpayPaymentState.Success -> Tpay.Success(it.paymentId, null, null)
        }
    }
}

internal class TpayNavigation() {
    private val events = Channel<Event>()
    val flow = events.receiveAsFlow()

    suspend fun send(event: Event) {
        events.send(event)
    }

    sealed interface Event {

        class GoToTinkoff(val deeplink: String) : Event
        class Close(val result: Tpay.Result) : Event
    }
}