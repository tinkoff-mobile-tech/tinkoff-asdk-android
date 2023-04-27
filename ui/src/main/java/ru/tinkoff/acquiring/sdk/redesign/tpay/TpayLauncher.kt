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
import ru.tinkoff.acquiring.sdk.exceptions.asAcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.getErrorCodeIfApiError
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.common.result.AcqPaymentResult
import ru.tinkoff.acquiring.sdk.redesign.tpay.ui.TpayFlowActivity

object TpayLauncher {

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

    fun AppCompatActivity.setResult(result: TpayLauncher.Result) {
        val intent = Intent()
        when (result) {
            Canceled -> setResult(RESULT_CANCELED)
            is Error -> {
                intent.putExtra(TinkoffAcquiring.EXTRA_ERROR, result.error)
                setResult(TinkoffAcquiring.RESULT_ERROR, intent)
            }
            is Success -> {
                with(intent) {
                    putExtra(TinkoffAcquiring.EXTRA_PAYMENT_ID, result.paymentId ?: -1)
                    putExtra(TinkoffAcquiring.EXTRA_CARD_ID, result.cardId)
                    putExtra(TinkoffAcquiring.EXTRA_REBILL_ID, result.rebillId)
                }
                setResult(RESULT_OK, intent)
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

    object Contract : ActivityResultContract<StartData, TpayLauncher.Result>() {

        internal const val EXTRA_START_DATA  = "extra_start_data"

        override fun createIntent(context: Context, startData: StartData): Intent =
            Intent(context, TpayFlowActivity::class.java).apply {
                putExtra(EXTRA_START_DATA, startData)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): TpayLauncher.Result =
            when (resultCode) {
                AppCompatActivity.RESULT_OK -> {
                   with(checkNotNull(intent)) {
                       TpayLauncher.Success(
                           getLongExtra(TinkoffAcquiring.EXTRA_PAYMENT_ID, -1),
                           getStringExtra(TinkoffAcquiring.EXTRA_CARD_ID),
                           getStringExtra(TinkoffAcquiring.EXTRA_REBILL_ID),
                       )
                   }
                }
                TinkoffAcquiring.RESULT_ERROR -> {
                    val throwable = checkNotNull(
                        intent?.getSerializableExtra(TinkoffAcquiring.EXTRA_ERROR) as? Throwable
                    )
                    TpayLauncher.Error(
                        throwable,
                        throwable.asAcquiringApiException()?.getErrorCodeIfApiError()?.toIntOrNull()
                    )
                }
                else -> TpayLauncher.Canceled
            }
    }
}
