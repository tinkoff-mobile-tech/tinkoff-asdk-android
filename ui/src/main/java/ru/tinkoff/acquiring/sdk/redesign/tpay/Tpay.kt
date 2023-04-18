package ru.tinkoff.acquiring.sdk.redesign.tpay

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.parcel.Parcelize
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.common.result.AcqPaymentResult
import ru.tinkoff.acquiring.sdk.redesign.tpay.ui.TpayFlowActivity

object Tpay {

    sealed class Result

    class Success(
        override val paymentId: Long? = null,
        override val cardId: String? = null,
        override val rebillId: String? = null
    ) : Result(), AcqPaymentResult.Success, java.io.Serializable

    object Canceled : Result(), AcqPaymentResult.Canceled

    class Error(
        override val error: Throwable,
        override val errorCode: Int?
    ) : Result(), AcqPaymentResult.Error, java.io.Serializable

    fun AppCompatActivity.setResult(result: Tpay.Result) {
        val intent = Intent()
        when (result) {
            Canceled -> setResult(RESULT_CANCELED)
            is Error -> {
                intent.putExtra(TinkoffAcquiring.EXTRA_ERROR, result.error)
                setResult(TinkoffAcquiring.RESULT_ERROR, intent)
            }
            is Success -> {
                with(intent) {
                    getLongExtra(TinkoffAcquiring.EXTRA_PAYMENT_ID, -1)
                    getStringExtra(TinkoffAcquiring.EXTRA_CARD_ID)
                    getStringExtra(TinkoffAcquiring.EXTRA_REBILL_ID)
                }
                setResult(RESULT_OK)
            }
        }
        finish()
    }

    @Parcelize
    class StartData(
        val paymentOptions: PaymentOptions,
        val version: String,
        val paymentId: Long? = null
    ) : Parcelable

    object Contract : ActivityResultContract<StartData, Tpay.Result>() {

        internal const val EXTRA_START_DATA  = "extra_start_data"

        override fun createIntent(context: Context, startData: StartData): Intent =
            Intent(context, TpayFlowActivity::class.java).apply {
                putExtra(EXTRA_START_DATA, startData)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): Tpay.Result =
            when (resultCode) {
                AppCompatActivity.RESULT_OK -> {
                    val _intent = intent!!
                    Tpay.Success(
                        _intent.getLongExtra(TinkoffAcquiring.EXTRA_PAYMENT_ID, -1),
                        _intent.getStringExtra(TinkoffAcquiring.EXTRA_CARD_ID),
                        _intent.getStringExtra(TinkoffAcquiring.EXTRA_REBILL_ID),
                    )
                }
                TinkoffAcquiring.RESULT_ERROR -> Tpay.Error(
                    intent!!.getSerializableExtra(TinkoffAcquiring.EXTRA_ERROR)!! as Throwable,
                    null // TODO
                )
                else -> Tpay.Canceled
            }
    }
}
