/*
 * Copyright © 2020 Tinkoff Bank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.tinkoff.cardio

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import io.card.payment.CardIOActivity
import io.card.payment.CreditCard
import ru.tinkoff.acquiring.sdk.cardscanners.delegate.CardScannerContract
import ru.tinkoff.acquiring.sdk.cardscanners.delegate.ScannedCardResult
import ru.tinkoff.acquiring.sdk.cardscanners.models.AsdkScannedCardData
import ru.tinkoff.acquiring.sdk.cardscanners.models.ScannedCardData
import java.util.*

/**
 * @author Mariya Chernyadieva
 */
object CameraCardIOScannerContract : CardScannerContract() {

    override fun createIntent(context: Context, input: Unit): Intent {
        return createIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ScannedCardResult {
        return when (resultCode) {
            RESULT_OK -> ScannedCardResult.Success(parseIntentData(intent!!))
            RESULT_CANCELED -> ScannedCardResult.Cancel
            else -> ScannedCardResult.Failure(null)
        }
    }

    private fun parseIntentData(data: Intent): ScannedCardData {
        val cardNumber: String
        var expireDate = ""
        val cardholderName = ""

        val scanResult = data.getParcelableExtra<CreditCard>(CardIOActivity.EXTRA_SCAN_RESULT)
        cardNumber = scanResult!!.formattedCardNumber
        if (scanResult.expiryMonth != 0 && scanResult.expiryYear != 0) {
            val locale = Locale.getDefault()
            val expiryYear = scanResult.expiryYear % 100
            expireDate = String.format(locale, "%02d%02d", scanResult.expiryMonth, expiryYear)
        }

        return AsdkScannedCardData(cardNumber, expireDate, cardholderName)
    }

    private fun createIntent(context: Context): Intent {
        val scanIntent = Intent(context, CardIOActivity::class.java)
        return scanIntent.apply {
            putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, true)
            putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, false)
            putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false)
            putExtra(CardIOActivity.EXTRA_SUPPRESS_CONFIRMATION, true)
        }
    }
}