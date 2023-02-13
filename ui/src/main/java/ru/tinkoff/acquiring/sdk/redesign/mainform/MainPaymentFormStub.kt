package ru.tinkoff.acquiring.sdk.redesign.mainform

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ViewFlipper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormViewModel
import ru.tinkoff.acquiring.sdk.redesign.mainform.ui.PrimaryButtonComponent
import ru.tinkoff.acquiring.sdk.redesign.payment.ui.PaymentByCard
import ru.tinkoff.acquiring.sdk.utils.*
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.utils.lazyView

/**
 * Created by i.golovachev
 */
// rework after desing main form
class MainPaymentFormStub : AppCompatActivity() {

    val options by lazyUnsafe { intent.getOptions<PaymentOptions>() }

    private val viewModel: MainPaymentFormViewModel by viewModels {
        MainPaymentFormViewModel.factory(application)
    }

    private val flipper: ViewFlipper by lazyView(R.id.acq_main_form_flipper)

    private val primaryButtonComponent by lazyUnsafe {
        PrimaryButtonComponent(findViewById(R.id.acq_main_form_primary_button))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_main_from_activity)

        lifecycleScope.launch {
            viewModel.stateFlow.collect { state ->
                when(state) {
                    MainPaymentFormViewModel.State.Loading -> flipper.showById(R.id.acq_main_form_loader)
                    is MainPaymentFormViewModel.State.Content -> flipper.showById(R.id.acq_main_form_primary_button).let {
                        primaryButtonComponent.render(state = state.button)
                    }
                    MainPaymentFormViewModel.State.Error -> flipper.showById(R.id.acq_main_form_loader)
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