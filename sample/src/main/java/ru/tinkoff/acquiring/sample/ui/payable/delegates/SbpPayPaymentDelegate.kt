package ru.tinkoff.acquiring.sample.ui.payable.delegates

import androidx.activity.result.ActivityResultLauncher
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.ui.payable.PayableActivity
import ru.tinkoff.acquiring.sample.utils.toast
import ru.tinkoff.acquiring.sdk.redesign.sbp.SbpPayLauncher

/**
 * @author k.shpakovskiy
 */
interface SbpPayPaymentDelegate {
    fun initSbpPayPayment(activity: PayableActivity)
    fun startSbpPayment()
}

class SbpPayPayment : SbpPayPaymentDelegate,
    InitRequestDelegate by InitRequestDelegateImpl() {
    private lateinit var sbpPayment: ActivityResultLauncher<SbpPayLauncher.StartData>
    private lateinit var activity: PayableActivity

    override fun initSbpPayPayment(activity: PayableActivity) {
        this.activity = activity
        with(activity) {
            sbpPayment = activity.registerForActivityResult(SbpPayLauncher.Contract) { result ->
                when (result) {
                    is SbpPayLauncher.Success -> {
                        toast("SBP Success")
                    }
                    is SbpPayLauncher.Error -> toast(result.error.message ?: getString(R.string.error_title))
                    is SbpPayLauncher.Canceled -> toast("SBP canceled")
                    is SbpPayLauncher.NoBanks -> Unit
                }
            }
        }
    }

    override fun startSbpPayment() {
        with(activity) {
            val options = createPaymentOptions()
            tinkoffAcquiring.initSbpPaymentSession()
            if (settings.isEnableCombiInit) {
                startInitRequest(activity, options) { optionsWithPaymentId ->
                    sbpPayment.launch(SbpPayLauncher.StartData(optionsWithPaymentId))
                }
            } else {
                sbpPayment.launch(SbpPayLauncher.StartData(options))
            }
        }
    }
}
