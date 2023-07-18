package ru.tinkoff.acquiring.sdk.cardscanners.delegate.nfc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import ru.tinkoff.acquiring.sdk.cardscanners.delegate.*
import ru.tinkoff.acquiring.sdk.cardscanners.models.ScannedCardData
import ru.tinkoff.acquiring.sdk.cardscanners.ui.AsdkNfcScanActivity
import ru.tinkoff.acquiring.sdk.cardscanners.ui.AsdkNfcScanActivity.Companion.EXTRA_CARD

/**
 *  Дефолтная реализация  сканированния карты по НФС
 */
internal class NfcCardScannerDelegate(
    activity: ComponentActivity,
    callback: AsdkCardScanResultCallback
) : AsdkCardScannerDelegate(
    activity = activity,
    contract = NfcCardScannerContract,
    callback,
    CardScannerTypes.NFC,
    isEnabledChecker = { activity.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) }
)

object NfcCardScannerContract : CardScannerContract() {
    override fun createIntent(context: Context, input: Unit) =
        Intent(context, AsdkNfcScanActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): ScannedCardResult {
        return when (resultCode) {
            Activity.RESULT_OK -> ScannedCardResult.Success(intent!!.getSerializableExtra(EXTRA_CARD) as ScannedCardData)
            Activity.RESULT_CANCELED -> ScannedCardResult.Cancel
            AsdkNfcScanActivity.RESULT_ERROR -> ScannedCardResult.Failure(null)
            else -> throw java.lang.IllegalStateException("unknown code: $resultCode")
        }
    }
}