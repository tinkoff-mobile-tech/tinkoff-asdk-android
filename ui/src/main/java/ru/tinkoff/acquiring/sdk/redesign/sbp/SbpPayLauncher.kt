package ru.tinkoff.acquiring.sdk.redesign.sbp

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import kotlinx.android.parcel.Parcelize
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_PAYMENT_ID
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.redesign.sbp.ui.SbpPaymentActivity
import ru.tinkoff.acquiring.sdk.utils.getError

/**
 * @author k.shpakovskiy
 */
object SbpPayLauncher {

    sealed class Result
    class Success(val payment: Long) : Result()
    class Canceled : Result()
    class Error(val error: Throwable) : Result()
    class NoBanks : Result()

    @Parcelize
    class StartData(val paymentOptions: PaymentOptions) : Parcelable

    object Contract: ActivityResultContract<StartData, Result>() {
        internal const val SBP_BANK_RESULT_CODE_NO_BANKS = 501
        internal const val EXTRA_PAYMENT_DATA = "extra_payment_data"

        override fun createIntent(context: Context, data: StartData): Intent =
            Intent(context, SbpPaymentActivity::class.java).
                putExtra(EXTRA_PAYMENT_DATA, data)

        override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
            RESULT_OK ->
                Success(
                    checkNotNull(
                        intent?.getLongExtra(EXTRA_PAYMENT_ID, 0),
                    )
                )
            RESULT_ERROR -> Error(intent.getError())
            SBP_BANK_RESULT_CODE_NO_BANKS -> NoBanks()
            else -> Canceled()
        }
    }
}
