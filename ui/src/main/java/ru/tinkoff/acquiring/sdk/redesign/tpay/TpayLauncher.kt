package ru.tinkoff.acquiring.sdk.redesign.tpay

import android.app.Activity.RESULT_CANCELED
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import kotlinx.android.parcel.Parcelize
import ru.tinkoff.acquiring.sdk.exceptions.asAcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.getErrorCodeIfApiError
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_CARD_ID
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_ERROR
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_PAYMENT_ID
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_REBILL_ID
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_START_DATA
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.redesign.common.result.AcqPaymentResult
import ru.tinkoff.acquiring.sdk.redesign.tpay.ui.TpayFlowActivity
import ru.tinkoff.acquiring.sdk.utils.getError
import ru.tinkoff.acquiring.sdk.utils.getExtra

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

    fun AppCompatActivity.setResult(result: Result) {
        val intent = Intent()
        when (result) {
            Canceled -> setResult(RESULT_CANCELED)
            is Error -> {
                intent.putExtra(EXTRA_ERROR, result.error)
                setResult(RESULT_ERROR, intent)
            }
            is Success -> {
                with(intent) {
                    putExtra(EXTRA_PAYMENT_ID, result.paymentId ?: -1)
                    putExtra(EXTRA_CARD_ID, result.cardId)
                    putExtra(EXTRA_REBILL_ID, result.rebillId)
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

    object Contract : ActivityResultContract<StartData, Result>() {
        override fun createIntent(context: Context, startData: StartData): Intent =
            Intent(context, TpayFlowActivity::class.java)
                .putExtra(EXTRA_START_DATA, startData)

        override fun parseResult(resultCode: Int, intent: Intent?): Result =
            when (resultCode) {
                RESULT_OK -> {
                   with(checkNotNull(intent)) {
                       Success(
                           getLongExtra(EXTRA_PAYMENT_ID, -1),
                           getStringExtra(EXTRA_CARD_ID),
                           getStringExtra(EXTRA_REBILL_ID),
                       )
                   }
                }
                RESULT_ERROR -> {
                    val throwable = intent.getError()
                    Error(
                        throwable,
                        throwable.asAcquiringApiException()?.getErrorCodeIfApiError()?.toIntOrNull()
                    )
                }
                else -> Canceled
            }
    }
}
