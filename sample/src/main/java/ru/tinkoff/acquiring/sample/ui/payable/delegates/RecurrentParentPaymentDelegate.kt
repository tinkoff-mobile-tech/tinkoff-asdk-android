package ru.tinkoff.acquiring.sample.ui.payable.delegates

import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.isVisible
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.SampleApplication
import ru.tinkoff.acquiring.sample.ui.payable.PayableActivity
import ru.tinkoff.acquiring.sample.utils.toast
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.PaymentByCardProcess
import ru.tinkoff.acquiring.sdk.redesign.payment.PaymentByCardLauncher
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper

/**
 * @author k.shpakovskiy
 */
interface RecurrentParentPaymentDelegate {
    fun initRecurrentParentPaymentDelegate(activity: PayableActivity)
    fun setupRecurrentParentPayment()
}

class RecurrentParentPayment : RecurrentParentPaymentDelegate,
    InitRequestDelegate by InitRequestDelegateImpl() {
    private lateinit var activity: PayableActivity
    private lateinit var recurrentParentPaymentLauncher: ActivityResultLauncher<PaymentByCardLauncher.StartData>

    override fun initRecurrentParentPaymentDelegate(activity: PayableActivity) {
        this.activity = activity
        recurrentParentPaymentLauncher = with(activity) {
            registerForActivityResult(PaymentByCardLauncher.Contract) { result ->
                when (result) {
                    is PaymentByCardLauncher.Success -> {
                        toast("byCardPayment Success : ${result.paymentId}")
                    }
                    is PaymentByCardLauncher.Error -> toast(result.error.message ?: getString(R.string.error_title))
                    is PaymentByCardLauncher.Canceled -> toast("byCardPayment canceled")
                }
            }
        }
    }

    override fun setupRecurrentParentPayment() {
        with(activity) {
            val recurrentButton : TextView? = findViewById(R.id.recurrent_pay)
            recurrentButton?.isVisible = settings.isRecurrentPayment
            recurrentButton?.setOnClickListener {
                PaymentByCardProcess.init(
                    sdk = SampleApplication.tinkoffAcquiring.sdk,
                    application = application,
                    threeDsDataCollector = ThreeDsHelper.CollectData
                )

                val options = createPaymentOptions()
                if (settings.isEnableCombiInit) {
                    startInitRequest(activity, options) { optionsWithPaymentId ->
                        launchPaymentByCard(optionsWithPaymentId)
                    }
                } else {
                    launchPaymentByCard(options)
                }
            }
        }
    }

    private fun launchPaymentByCard(options: PaymentOptions) {
        recurrentParentPaymentLauncher.launch(
            PaymentByCardLauncher.StartData(
                paymentOptions = options,
                cards = ArrayList()
            )
        )
    }
}
