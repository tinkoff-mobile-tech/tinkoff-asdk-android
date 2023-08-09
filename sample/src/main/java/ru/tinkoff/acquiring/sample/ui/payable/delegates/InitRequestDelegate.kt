package ru.tinkoff.acquiring.sample.ui.payable.delegates

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sample.ui.payable.PayableActivity
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.utils.checkNotNull

/**
 * @author k.shpakovskiy
 */
interface InitRequestDelegate {
    fun startInitRequest(
        activity: PayableActivity,
        options: PaymentOptions,
        result: (PaymentOptions) -> Unit
    )
}

class InitRequestDelegateImpl : InitRequestDelegate {

    override fun startInitRequest(
        activity: PayableActivity,
        options: PaymentOptions,
        result: (PaymentOptions) -> Unit
    ) {
        with(activity) {
            lifecycleScope.launch {
                showProgressDialog()
                runCatching {
                    combInitDelegate
                        .sendInit(options)
                        .paymentId
                        .checkNotNull { "payment id mustn't null" }
                }
                    .onFailure {
                        hideProgressDialog()
                        showErrorDialog()
                    }
                    .onSuccess { paymentId ->
                        hideProgressDialog()
                        options.paymentId = paymentId
                        result(options)
                    }
            }
        }
    }
}
