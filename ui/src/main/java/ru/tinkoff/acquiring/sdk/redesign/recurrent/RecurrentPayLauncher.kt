package ru.tinkoff.acquiring.sdk.redesign.recurrent

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.parcel.Parcelize
import ru.tinkoff.acquiring.sdk.exceptions.asAcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.getErrorCodeIfApiError
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_ERROR
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_PAYMENT_ID
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_REBILL_ID
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.redesign.common.result.AcqPaymentResult
import ru.tinkoff.acquiring.sdk.redesign.recurrent.ui.RecurrentPaymentActivity
import ru.tinkoff.acquiring.sdk.utils.getError

object RecurrentPayLauncher {

    sealed class Result

    class Success(
        override val paymentId: Long? = null,
        override val cardId: String? = null,
        override val rebillId: String
    ) : Result(), AcqPaymentResult.Success

    object Canceled : Result(), AcqPaymentResult.Canceled

    class Error(
        val paymentId: Long?,
        override val error: Throwable,
        override val errorCode: Int?
    ) : Result(), AcqPaymentResult.Error

    @Parcelize
    class StartData(
        val card: Card,
        val paymentOptions: PaymentOptions,
    ) : Parcelable

    object Contract : ActivityResultContract<StartData, Result>() {

        internal fun createSuccessIntent(
            paymentId: Long,
            rebillId: String,
        ): Intent {
            val intent = Intent()
            intent.putExtra(EXTRA_PAYMENT_ID, paymentId)
            intent.putExtra(EXTRA_REBILL_ID, rebillId)
            return intent
        }

        internal fun createFailedIntent(throwable: Throwable): Intent {
            val intent = Intent()
            intent.putExtra(EXTRA_ERROR, throwable)
            return intent
        }

        override fun createIntent(context: Context, input: StartData): Intent {
            return RecurrentPaymentActivity.intent(context, input.paymentOptions, input.card)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
            AppCompatActivity.RESULT_OK -> {
                with(checkNotNull(intent)) {
                    Success(
                        getLongExtra(EXTRA_PAYMENT_ID, -1),
                        null,
                        checkNotNull(getStringExtra(EXTRA_REBILL_ID)),
                    )
                }
            }
            RESULT_ERROR -> {
                with(checkNotNull(intent)) {
                    val throwable = intent.getError()
                    Error(
                        getLongExtra(EXTRA_PAYMENT_ID, -1),
                        throwable,
                        throwable.asAcquiringApiException()?.getErrorCodeIfApiError()?.toIntOrNull()
                    )
                }

            }
            else -> Canceled
        }
    }
}
