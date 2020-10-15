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

package ru.tinkoff.acquiring.sdk.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import org.json.JSONArray
import org.json.JSONObject
import ru.tinkoff.acquiring.sdk.models.GooglePayParams
import java.math.BigDecimal

/**
 * Осуществляет настройку и вызов Google Pay
 *
 * @author Mariya Chernyadieva
 */
class GooglePayHelper(private val params: GooglePayParams) {

    private lateinit var paymentsClient: PaymentsClient

    /**
     * Инициирует Google Pay API, определяет доступность совершения платежей на устройстве
     *
     * @param context          контекст для инициализации
     * @param onGooglePayReady коллбек, оповещающий о доступности Google Pay на устройстве
     */
    fun initGooglePay(context: Context, onGooglePayReady: (Boolean) -> Unit) {
        val isReadyToPayRequest = getBaseRequest()
                .put("allowedPaymentMethods", JSONArray()
                        .put(getBaseCardPaymentMethod()))

        val request = IsReadyToPayRequest.fromJson(isReadyToPayRequest.toString())
        val options = Wallet.WalletOptions.Builder()
                .setEnvironment(params.environment)
                .build()

        paymentsClient = Wallet.getPaymentsClient(context, options)
        paymentsClient.isReadyToPay(request).addOnCompleteListener { task ->
            try {
                val result = task.getResult(ApiException::class.java)
                if (result != null) {
                    onGooglePayReady(result)
                } else {
                    onGooglePayReady(false)
                }
            } catch (e: ApiException) {
                onGooglePayReady(false)
            }
        }
    }

    /**
     * Запускает экран Google Pay
     *
     * @param activity    контекст для запуска экрана
     * @param price       сумма для оплаты
     * @param requestCode код для получения результата, по завершению работы Google Pay
     */
    fun openGooglePay(activity: Activity, price: Money, requestCode: Int) {
        check(::paymentsClient.isInitialized) { "Method initGooglePay() was not called" }

        val request = PaymentDataRequest.fromJson(createPaymentDataRequest(price).toString())
        AutoResolveHelper.resolveTask(paymentsClient.loadPaymentData(request), activity, requestCode)
    }

    private fun createPaymentDataRequest(price: Money): JSONObject {
        return getBaseRequest()
                .put("allowedPaymentMethods", JSONArray()
                        .put(getCardPaymentMethod()))
                .put("transactionInfo", getTransactionInfo(price))
                .put("shippingAddressRequired", params.isAddressRequired)
                .put("shippingAddressParameters", JSONObject()
                        .put("phoneNumberRequired", params.isPhoneRequired))
    }

    private fun getTransactionInfo(price: Money): JSONObject {
        val formattedPrice = BigDecimal(price.coins).setScale(2, BigDecimal.ROUND_HALF_EVEN).toString()
        return JSONObject()
                .put("totalPrice", formattedPrice)
                .put("totalPriceStatus", PRICE_STATUS)
                .put("currencyCode", GooglePayParams.CURRENCY_CODE)
    }

    private fun getCardPaymentMethod(): JSONObject {
        return getBaseCardPaymentMethod()
                .put("tokenizationSpecification", getTokenSpecification())
    }

    private fun getBaseCardPaymentMethod(): JSONObject {
        return JSONObject()
                .put("type", "CARD")
                .put("parameters", JSONObject()
                        .put("allowedAuthMethods", getAllowedCardAuthMethods())
                        .put("allowedCardNetworks", getAllowedCardNetworks()))
    }

    private fun getAllowedCardAuthMethods(): JSONArray {
        return JSONArray()
                .put("PAN_ONLY")
                .put("CRYPTOGRAM_3DS")
    }

    private fun getAllowedCardNetworks(): JSONArray {
        return JSONArray()
                .put("VISA")
                .put("MASTERCARD")
    }

    private fun getTokenSpecification(): JSONObject {
        return JSONObject()
                .put("type", GATEWAY_TYPE)
                .put("parameters", JSONObject()
                        .put("gateway", GATEWAY_NAME)
                        .put("gatewayMerchantId", params.terminalKey))

    }

    private fun getBaseRequest(): JSONObject {
        return JSONObject()
                .put("apiVersion", API_VERSION)
                .put("apiVersionMinor", API_VERSION_MINOR)
    }

    companion object {
        private const val API_VERSION = 2
        private const val API_VERSION_MINOR = 0

        private const val GATEWAY_NAME = "tinkoff"
        private const val GATEWAY_TYPE = "PAYMENT_GATEWAY"
        private const val PRICE_STATUS = "FINAL"

        @JvmStatic
        fun getGooglePayToken(data: Intent): String? {
            val json = PaymentData.getFromIntent(data)?.toJson()

            return if (json == null) {
                null
            } else {
                val paymentMethodData = JSONObject(json).getJSONObject("paymentMethodData")
                val token = paymentMethodData.getJSONObject("tokenizationData").getString("token")
                Base64.encodeToString(token.toByteArray(), Base64.DEFAULT).trim { it <= ' ' }
            }
        }
    }
}