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

package ru.tinkoff.acquiring.sample.service

import android.app.IntentService
import android.content.Intent
import ru.tinkoff.acquiring.sample.utils.PaymentNotificationManager.ACTION_SELECT_PRICE

/**
 * @author Mariya Chernyadieva
 */
class PaymentNotificationIntentService : IntentService("PaymentNotificationIntentService") {

    override fun onHandleIntent(intent: Intent?) {
        intent?.let {
            val intentAction = it.action
            if (intentAction != null && intentAction.startsWith(ACTION_SELECT_PRICE)) {
                val option = intentAction.substringAfter(ACTION_SELECT_PRICE)
                val responseIntent = Intent().apply {
                    action = ACTION_PRICE_SELECT
                    addCategory(Intent.CATEGORY_DEFAULT)
                    putExtra(EXTRA_NOTIFICATION_PRICE_OPTION, option)
                }
                sendBroadcast(responseIntent)
            }
        }
    }

    companion object {
        const val ACTION_PRICE_SELECT = "ru.tinkoff.acquiring.sample.service.PRICE_SELECT"
        const val EXTRA_NOTIFICATION_PRICE_OPTION = "price_option"
    }

}
