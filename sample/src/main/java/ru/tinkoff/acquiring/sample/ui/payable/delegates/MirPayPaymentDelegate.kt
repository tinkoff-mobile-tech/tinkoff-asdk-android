package ru.tinkoff.acquiring.sample.ui.payable.delegates

import android.view.View
import androidx.activity.result.ActivityResultLauncher
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.ui.payable.PayableActivity
import ru.tinkoff.acquiring.sample.utils.SettingsSdkManager
import ru.tinkoff.acquiring.sample.utils.toast
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.mirpay.MirPayLauncher
import ru.tinkoff.acquiring.sdk.redesign.tpay.models.enableMirPay

/**
 * @author k.shpakovskiy
 */
interface MirPayPaymentDelegate {
    fun initMirPayPaymentDelegate(activity: PayableActivity)
    fun setupMirPay()
}

class MirPayPayment : MirPayPaymentDelegate,
    InitRequestDelegate by InitRequestDelegateImpl()  {
    private lateinit var activity: PayableActivity
    private lateinit var mirPayment: ActivityResultLauncher<MirPayLauncher.StartData>

    override fun initMirPayPaymentDelegate(activity: PayableActivity) {
        this.activity = activity
        mirPayment = with(activity) {
            registerForActivityResult(MirPayLauncher.Contract) { result ->
                when (result) {
                    is MirPayLauncher.Canceled -> toast("MirPay canceled")
                    is MirPayLauncher.Error -> toast(result.error.message ?: getString(R.string.error_title))
                    is MirPayLauncher.Success -> toast("payment Success - paymentId:${result.paymentId}")
                }
            }
        }
    }

    override fun setupMirPay() {
        with(activity) {
            val mirPayButton = findViewById<View>(R.id.mir_pay_button)

            tinkoffAcquiring.checkTerminalInfo({ status ->
                if (status.enableMirPay().not()) return@checkTerminalInfo

                mirPayButton.visibility = View.VISIBLE

                tinkoffAcquiring.initMirPayPaymentSession()
                val options = createPaymentOptions()
                mirPayButton.setOnClickListener { launchMirPayPayment(settings, options) }
            })
        }
    }

    private fun launchMirPayPayment(settings: SettingsSdkManager, options: PaymentOptions) {
        if (settings.isEnableCombiInit) {
            startInitRequest(activity, options) { optionsWithPaymentId ->
                mirPayment.launch(MirPayLauncher.StartData(optionsWithPaymentId))
            }
        } else {
            mirPayment.launch(MirPayLauncher.StartData(options))
        }
    }
}
