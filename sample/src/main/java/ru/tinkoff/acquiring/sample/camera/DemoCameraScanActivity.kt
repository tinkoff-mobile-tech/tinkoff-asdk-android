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

package ru.tinkoff.acquiring.sample.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sdk.cardscanners.delegate.CardScannerContract
import ru.tinkoff.acquiring.sdk.cardscanners.delegate.ScannedCardResult
import ru.tinkoff.acquiring.sdk.cardscanners.models.AsdkScannedCardData
import java.util.*

/**
 * @author Mariya Chernyadieva
 */
class DemoCameraScanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_demo)

        findViewById<Button>(R.id.bt_success).setOnClickListener {
            val data = Intent()
            data.putExtra(EXTRA_CARD_NUMBER, CARD_NUMBERS[random.nextInt(CARD_NUMBERS.size)])
            data.putExtra(EXTRA_EXPIRE_DATE, EXPIRE_DATE)
            setResult(Activity.RESULT_OK, data)
            finish()
        }

        findViewById<Button>(R.id.bt_cancel).setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    companion object {

        const val EXTRA_CARD_NUMBER = "card_number"
        const val EXTRA_EXPIRE_DATE = "expire_date"

        private const val EXPIRE_DATE = "11/28"
        private val CARD_NUMBERS = arrayOf(
            "5136 9149 2034 4072",
            "5136 9149 2034 7240",
            "5203 7375 0075 0535",
            "5203 7375 0075 3505"
        )
        private val random = Random()
    }

    object Contract : CardScannerContract() {
        override fun createIntent(context: Context, input: Unit): Intent {
            return Intent(context, DemoCameraScanActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): ScannedCardResult {
            return when (resultCode) {
                RESULT_CANCELED -> ScannedCardResult.Cancel
                RESULT_OK -> ScannedCardResult.Success(
                    AsdkScannedCardData(
                        cardNumber = intent?.getStringExtra(EXTRA_CARD_NUMBER).orEmpty(),
                        expireDate = intent?.getStringExtra(EXTRA_EXPIRE_DATE).orEmpty(),
                        ""
                    )
                )
                else -> ScannedCardResult.Failure(null)
            }
        }
    }
}