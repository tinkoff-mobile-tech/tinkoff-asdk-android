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

package ru.tinkoff.acquiring.sample.utils

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.SampleApplication
import ru.tinkoff.acquiring.sample.service.PaymentNotificationIntentService
import ru.tinkoff.acquiring.sample.ui.MainActivity.Companion.NOTIFICATION_PAYMENT_REQUEST_CODE
import ru.tinkoff.acquiring.sdk.localization.AsdkSource
import ru.tinkoff.acquiring.sdk.localization.Language
import ru.tinkoff.acquiring.sdk.models.GooglePayParams
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.utils.Money
import java.util.*
import kotlin.math.abs

/**
 * @author Mariya Chernyadieva
 */
object PaymentNotificationManager {

    const val ACTION_SELECT_PRICE = "SELECT_PRICE"

    const val PRICE_BUTTON_ID_1 = "button1"
    const val PRICE_BUTTON_ID_2 = "button2"
    const val PRICE_BUTTON_ID_3 = "button3"

    private const val NOTIFICATION_ID = 1
    private const val NOTIFICATION_CHANNEL_ID = "payments_channel"

    private val priceMap: HashMap<String, Money> = hashMapOf(
            PRICE_BUTTON_ID_1 to Money.ofRubles(100),
            PRICE_BUTTON_ID_2 to Money.ofRubles(200),
            PRICE_BUTTON_ID_3 to Money.ofRubles(300))

    fun triggerNotification(activity: Activity, selectedButtonId: String) {
        val res = activity.resources
        val packageName = activity.packageName

        val notificationLayout = RemoteViews(packageName, R.layout.payment_notification)

        priceMap.keys.forEach { buttonPriceId ->
            val buttonId = res.getIdentifier(buttonPriceId, "id", packageName)
            val selectedBg = R.drawable.bg_notification_price_button_selected
            val unselectedBg = R.drawable.bg_notification_price_button_unselected

            notificationLayout.setTextViewText(buttonId,
                    priceMap[buttonPriceId]?.toHumanReadableString())

            if (buttonPriceId == selectedButtonId) {
                notificationLayout.setInt(buttonId, "setBackgroundResource", selectedBg)
            } else {
                notificationLayout.setInt(buttonId, "setBackgroundResource", unselectedBg)
            }
            val selectPriceIntent = Intent(activity,
                    PaymentNotificationIntentService::class.java).apply {
                action = ACTION_SELECT_PRICE + buttonPriceId
            }
            notificationLayout.setOnClickPendingIntent(buttonId, PendingIntent.getService(
                    activity,
                    0,
                    selectPriceIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT))
        }

        val googlePayIntent = getGooglePayIntent(activity,
                requireNotNull(priceMap[selectedButtonId]))
        notificationLayout.setOnClickPendingIntent(R.id.googlePayButton, googlePayIntent)

        val tinkoffPayIntent = getTinkoffPayIntent(activity,
                requireNotNull(priceMap[selectedButtonId]))
        notificationLayout.setOnClickPendingIntent(R.id.buttonPayOther, tinkoffPayIntent)

        val notification = createNotification(activity, googlePayIntent, notificationLayout)
        NotificationManagerCompat.from(activity).notify(NOTIFICATION_ID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(context: Context) {
        val channelName = context.getString(R.string.notification_channel_name)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableLights(true)
            enableVibration(true)
            lightColor = Color.BLUE
        }

        notificationManager.createNotificationChannel(channel)
    }

    private fun getGooglePayIntent(activity: Activity, price: Money): PendingIntent {
        val settings = SettingsSdkManager(activity)
        val options = createNotificationPaymentOptions(activity, price)
        val googleParams = GooglePayParams(settings.terminalKey,
                environment = SessionParams.GPAY_TEST_ENVIRONMENT)

        return SampleApplication.tinkoffAcquiring.createGooglePayPendingIntentForResult(activity,
                googleParams,
                options, NOTIFICATION_PAYMENT_REQUEST_CODE, NOTIFICATION_ID)
    }

    private fun getTinkoffPayIntent(activity: Activity, price: Money): PendingIntent {
        val options = createNotificationPaymentOptions(activity, price)
        return SampleApplication.tinkoffAcquiring.createTinkoffPaymentPendingIntentForResult(activity,
                options, NOTIFICATION_PAYMENT_REQUEST_CODE, NOTIFICATION_ID)
    }

    private fun createNotificationPaymentOptions(activity: Activity, price: Money): PaymentOptions {
        val settings = SettingsSdkManager(activity)
        val sessionParams = SessionParams[settings.terminalKey]

        return PaymentOptions()
                .setOptions {
                    orderOptions {
                        orderId = abs(Random().nextInt()).toString()
                        amount = price
                        title = activity.getString(R.string.notification_order_title_subscription_renewal)
                    }
                    customerOptions {
                        customerKey = sessionParams.customerKey
                        checkType = settings.checkType
                    }
                    featuresOptions {
                        localizationSource = AsdkSource(Language.RU)
                        useSecureKeyboard = settings.isCustomKeyboardEnabled
                        cameraCardScanner = settings.cameraScanner
                        fpsEnabled = settings.isFpsEnabled
                        darkThemeMode = settings.resolveDarkThemeMode()
                        theme = settings.resolvePaymentStyle()
                    }
                }
    }

    private fun createNotification(context: Context,
                                   intent: PendingIntent,
                                   layout: RemoteViews): Notification {
        return NotificationCompat.Builder(context,
                NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.cart)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_text))
                .setContentIntent(intent)
                .setCustomBigContentView(layout)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .build()
    }
}