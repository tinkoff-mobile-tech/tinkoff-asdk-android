package ru.tinkoff.acquiring.sdk.redesign.payment.ui

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.parcel.Parcelize
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult

object PaymentByCard {

    sealed class Result
    class Success(
        val paymentId: Long? = null,
        val cardId: String? = null,
        val rebillId: String? = null
    ) : Result()

    object Canceled : Result()
    class Error(val error: Throwable) : Result()

    @Parcelize
    class StartData(
        val paymentOptions: PaymentOptions,
        val list: ArrayList<Card>
    ): Parcelable

    object Contract : ActivityResultContract<StartData, Result>() {

        internal const val EXTRA_SAVED_CARDS = "extra_saved_cards"

        internal fun createSuccessIntent(paymentResult: PaymentResult): Intent {
            val intent = Intent()
            intent.putExtra(TinkoffAcquiring.EXTRA_PAYMENT_ID, paymentResult.paymentId)
            intent.putExtra(TinkoffAcquiring.EXTRA_CARD_ID, paymentResult.cardId)
            intent.putExtra(TinkoffAcquiring.EXTRA_REBILL_ID, paymentResult.rebillId)
            return intent
        }

        internal fun createFailedIntent(throwable: Throwable): Intent {
            val intent = Intent()
            intent.putExtra(TinkoffAcquiring.EXTRA_ERROR, throwable)
            return intent
        }

        override fun createIntent(context: Context, startData: StartData): Intent =
            Intent(context, PaymentByCardActivity::class.java).apply {
                putExtra(EXTRA_SAVED_CARDS, startData)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
            AppCompatActivity.RESULT_OK -> {
                val _intent = intent!!
                Success(
                    _intent.getLongExtra(TinkoffAcquiring.EXTRA_PAYMENT_ID, -1),
                    _intent.getStringExtra(TinkoffAcquiring.EXTRA_CARD_ID),
                    _intent.getStringExtra(TinkoffAcquiring.EXTRA_REBILL_ID),
                )
            }
            TinkoffAcquiring.RESULT_ERROR -> Error(intent!!.getSerializableExtra(TinkoffAcquiring.EXTRA_ERROR)!! as Throwable)
            else -> Canceled
        }
    }
}