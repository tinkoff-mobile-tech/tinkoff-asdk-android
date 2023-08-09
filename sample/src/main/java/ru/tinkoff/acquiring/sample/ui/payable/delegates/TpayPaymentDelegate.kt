package ru.tinkoff.acquiring.sample.ui.payable.delegates

import android.view.View
import androidx.activity.result.ActivityResultLauncher
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.ui.payable.PayableActivity
import ru.tinkoff.acquiring.sample.utils.toast
import ru.tinkoff.acquiring.sdk.redesign.tpay.TpayLauncher
import ru.tinkoff.acquiring.sdk.redesign.tpay.models.enableTinkoffPay
import ru.tinkoff.acquiring.sdk.redesign.tpay.models.getTinkoffPayVersion

/**
 * @author k.shpakovskiy
 */
interface TpayPaymentDelegate {
    fun initTpayPaymentDelegate(activity: PayableActivity)
    fun setupTinkoffPay()
}

class TpayPayment : TpayPaymentDelegate,
    InitRequestDelegate by InitRequestDelegateImpl() {
    private lateinit var activity: PayableActivity
    private lateinit var tpayPayment: ActivityResultLauncher<TpayLauncher.StartData>

    override fun initTpayPaymentDelegate(activity: PayableActivity) {
        this.activity = activity
        tpayPayment = with(activity) {
            registerForActivityResult(TpayLauncher.Contract) { result ->
                when (result) {
                    is TpayLauncher.Canceled -> toast("tpay canceled")
                    is TpayLauncher.Error -> toast(result.error.message ?: activity.getString(R.string.error_title))
                    is TpayLauncher.Success -> toast("payment Success-  paymentId:${result.paymentId}")
                }
            }
        }
    }

    override fun setupTinkoffPay() {
        with(activity) {
            if (!settings.isTinkoffPayEnabled) return

            val tinkoffPayButton = findViewById<View>(R.id.tinkoff_pay_button)

            tinkoffAcquiring.checkTerminalInfo({ status ->
                if (status.enableTinkoffPay().not()) return@checkTerminalInfo

                tinkoffPayButton.visibility = View.VISIBLE
                val version = checkNotNull(status?.getTinkoffPayVersion())

                tinkoffAcquiring.initTinkoffPayPaymentSession()
                tinkoffPayButton.setOnClickListener {
                    val options = createPaymentOptions()
                    if (settings.isEnableCombiInit) {
                        startInitRequest(activity, options) { optionsWithPaymentId ->
                            tpayPayment.launch(TpayLauncher.StartData(optionsWithPaymentId, version))
                        }
                    } else {
                        tpayPayment.launch(TpayLauncher.StartData(options, version))
                    }
                }
            })
        }
    }
}
