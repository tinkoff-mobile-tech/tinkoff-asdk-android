package ru.tinkoff.acquiring.sample.ui.payable.delegates

import androidx.activity.result.ActivityResultLauncher
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.ui.payable.PayableActivity
import ru.tinkoff.acquiring.sample.utils.toast
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.redesign.recurrent.RecurrentPayLauncher

/**
 * @author k.shpakovskiy
 */
interface RecurrentPaymentDelegate {
    fun initRecurrentPaymentDelegate(activity: PayableActivity)
    fun launchRecurrent(card: Card)
}

class RecurrentPayment : RecurrentPaymentDelegate {
    private lateinit var activity: PayableActivity
    private lateinit var recurrentPayment: ActivityResultLauncher<RecurrentPayLauncher.StartData>

    override fun initRecurrentPaymentDelegate(activity: PayableActivity) {
        this.activity = activity
        with(activity) {
            recurrentPayment = registerForActivityResult(RecurrentPayLauncher.Contract) { result ->
                when (result) {
                    is RecurrentPayLauncher.Canceled -> toast("payment canceled")
                    is RecurrentPayLauncher.Error -> toast(result.error.message ?: getString(R.string.error_title))
                    is RecurrentPayLauncher.Success -> toast("payment Success-  paymentId:${result.paymentId}")
                }
            }
        }
    }

    override fun launchRecurrent(card: Card) {
        with(activity) {
            val options = createPaymentOptions()
            recurrentPayment.launch(
                RecurrentPayLauncher.StartData(card, options)
            )
        }
    }
}
