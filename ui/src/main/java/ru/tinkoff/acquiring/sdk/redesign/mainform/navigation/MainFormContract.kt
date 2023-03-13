package ru.tinkoff.acquiring.sdk.redesign.mainform.navigation

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.parcel.Parcelize
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.NetworkException
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.redesign.common.result.AcqPaymentResult
import ru.tinkoff.acquiring.sdk.redesign.mainform.MainPaymentFormActivity

object MainFormContract {
    sealed class Result
    class Success(
        override val paymentId: Long? = null,
        override val cardId: String? = null,
        override val rebillId: String? = null
    ) : Result(), AcqPaymentResult.Success

    object Canceled : Result(), AcqPaymentResult.Canceled
    class Error(
        override val error: Throwable,
        override val errorCode: Int?
    ) : Result(), AcqPaymentResult.Error {

        constructor(error: AcquiringApiException) : this(error, error.response?.errorCode?.toInt())
    }

    @Parcelize
    class StartData(
        val paymentOptions: PaymentOptions,
        val paymentId: Int? = null
    ) : Parcelable

    object Contract : ActivityResultContract<StartData, Result>() {
        override fun createIntent(context: Context, input: StartData) =
            MainPaymentFormActivity.intent(input.paymentOptions, context)

        override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
            AppCompatActivity.RESULT_OK -> {
                val _intent = intent!!
                Success(
                    _intent.getLongExtra(TinkoffAcquiring.EXTRA_PAYMENT_ID, -1),
                    _intent.getStringExtra(TinkoffAcquiring.EXTRA_CARD_ID),
                    _intent.getStringExtra(TinkoffAcquiring.EXTRA_REBILL_ID),
                )
            }
            TinkoffAcquiring.RESULT_ERROR -> {
                val th = intent!!.getSerializableExtra(TinkoffAcquiring.EXTRA_ERROR)!! as Throwable
                Error(th, (th as? AcquiringApiException)?.response?.errorCode?.toInt())
            }
            else -> Canceled
        }

        internal fun createSuccessIntent(
            paymentId: Long? = null,
            cardId: String? = null,
            rebillId: String? = null
        ): Intent {
            val intent = Intent()
            intent.putExtra(TinkoffAcquiring.EXTRA_PAYMENT_ID, paymentId)
            intent.putExtra(TinkoffAcquiring.EXTRA_CARD_ID, cardId)
            intent.putExtra(TinkoffAcquiring.EXTRA_REBILL_ID, rebillId)
            return intent
        }

        internal fun createSuccessIntent(
            success: AcqPaymentResult.Success
        ) = createSuccessIntent(success.paymentId, success.cardId, success.cardId)

        internal fun createFailedIntent(throwable: Throwable): Intent {
            val intent = Intent()
            intent.putExtra(TinkoffAcquiring.EXTRA_ERROR, throwable)
            return intent
        }

        internal fun createFailedIntent(error: AcqPaymentResult.Error) =
            Contract.createFailedIntent(error.error)
    }
}