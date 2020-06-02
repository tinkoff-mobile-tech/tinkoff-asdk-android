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
import ru.tinkoff.acquiring.sample.camera.DemoCameraScanActivity.Companion.EXTRA_CARD_NUMBER
import ru.tinkoff.acquiring.sample.camera.DemoCameraScanActivity.Companion.EXTRA_EXPIRE_DATE
import ru.tinkoff.acquiring.sdk.cardscanners.CameraCardScanner
import ru.tinkoff.acquiring.sdk.cardscanners.models.AsdkScannedCardData
import ru.tinkoff.acquiring.sdk.cardscanners.models.ScannedCardData

/**
 * @author Mariya Chernyadieva
 */
class DemoCameraScanner : CameraCardScanner {

    override fun startActivityForScanning(context: Context, requestCode: Int) {
        val scanIntent = createIntent(context)
        (context as Activity).startActivityForResult(scanIntent, requestCode)
    }

    override fun hasResult(data: Intent): Boolean {
        return data.hasExtra(EXTRA_CARD_NUMBER) || data.hasExtra(EXTRA_EXPIRE_DATE)
    }

    override fun parseIntentData(data: Intent): ScannedCardData {
        val cardNumber = data.getStringExtra(EXTRA_CARD_NUMBER)
        val expireDate = data.getStringExtra(EXTRA_EXPIRE_DATE)
        return AsdkScannedCardData(cardNumber!!, expireDate!!, "")
    }

    private fun createIntent(context: Context): Intent {
        return Intent(context, DemoCameraScanActivity::class.java)
    }
}