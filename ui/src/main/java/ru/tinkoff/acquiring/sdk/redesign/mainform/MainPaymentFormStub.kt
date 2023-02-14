package ru.tinkoff.acquiring.sdk.redesign.mainform

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.payment.ui.PaymentByCard
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.utils.getOptions
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.utils.putOptions

/**
 * Created by i.golovachev
 */
// rework after desing main form
class MainPaymentFormStub : AppCompatActivity() {

    val options by lazyUnsafe { intent.getOptions<PaymentOptions>() }
    private val byCardPayment = registerForActivityResult(PaymentByCard.Contract) { result ->
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            val cardList = TinkoffAcquiring(
                applicationContext,
                options.terminalKey,
                options.publicKey
            )
                .sdk.getCardList { this.customerKey = options.customer.customerKey }
                .performSuspendRequest().getOrNull()?.cards ?: emptyArray()


            byCardPayment.launch(
                PaymentByCard.StartData(
                    options,
                    ArrayList(cardList.toMutableList())
                )
            )
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