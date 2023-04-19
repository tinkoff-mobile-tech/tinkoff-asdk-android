package main

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.asCoroutineDispatcher
import org.mockito.kotlin.mock
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.mainform.navigation.MainFormNavController
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormFactory
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.analytics.MainFormAnalyticsDelegate
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.vm.MainPaymentFormViewModel
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager
import java.util.concurrent.Executors

internal class MainPaymentFormViewModelEnv {
    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val paymentOptions = PaymentOptions().apply {
        orderOptions { }
    }
    val primaryButtonFactory: MainPaymentFormFactory = mock()
    val mainFormNavController: MainFormNavController = MainFormNavController()
    val savedStateHandle = SavedStateHandle(mapOf("options" to paymentOptions))
    val viewModel
        get() = MainPaymentFormViewModel(
            primaryButtonFactory = primaryButtonFactory,
            mainFormNavController = mainFormNavController,
            savedStateHandle = savedStateHandle,
            paymentByCardProcess = mock(),
            mainFormAnalyticsDelegate = MainFormAnalyticsDelegate(),
            coroutineManager = CoroutineManager(dispatcher, dispatcher)
        )
}