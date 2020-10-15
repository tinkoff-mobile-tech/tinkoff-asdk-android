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

package ru.tinkoff.acquiring.sdk.requests

import ru.tinkoff.acquiring.sdk.network.NetworkClient
import ru.tinkoff.acquiring.sdk.responses.AcquiringResponse
import ru.tinkoff.acquiring.sdk.utils.CryptoUtils
import ru.tinkoff.acquiring.sdk.utils.Disposable
import java.security.PublicKey
import java.util.*

/**
 * Базовый класс создания запроса Acquiring API
 *
 * @author Mariya Chernyadieva
 */
abstract class AcquiringRequest<R : AcquiringResponse>(internal val apiMethod: String) : Disposable {

    internal lateinit var terminalKey: String
    internal lateinit var password: String
    internal lateinit var publicKey: PublicKey
    private var token: String? = null
    @Volatile
    private var disposed = false
    private val ignoredFieldsSet: HashSet<String> = hashSetOf(DATA, RECEIPT, RECEIPTS, SHOPS)

    internal open val tokenIgnoreFields: HashSet<String>
        get() = ignoredFieldsSet


    protected abstract fun validate()

    abstract fun execute(onSuccess: (R) -> Unit, onFailure: (Exception) -> Unit)

    override fun isDisposed(): Boolean {
        return disposed
    }

    override fun dispose() {
        disposed = true
    }

    open fun asMap(): MutableMap<String, Any> {
        val map = HashMap<String, Any>()

        map.putIfNotNull(TERMINAL_KEY, terminalKey)
        map.putIfNotNull(TOKEN, token)

        return map
    }

    protected fun <R : AcquiringResponse> performRequest(request: AcquiringRequest<R>,
                                                         responseClass: Class<R>,
                                                         onSuccess: (R) -> Unit,
                                                         onFailure: (Exception) -> Unit) {
        request.validate()
        request.apply { token = makeToken(request) }
        val client = NetworkClient()
        client.call(request, responseClass, onSuccess, onFailure)
    }

    protected fun MutableMap<String, Any>.putIfNotNull(key: String, value: Any?) {
        if (value == null) {
            return
        }

        this[key] = value
    }

    protected fun Any?.validate(fieldName: String) {
        checkNotNull(this) { "Unable to build request: field '$fieldName' is null" }

        when (this) {
            is String -> check(this.isNotEmpty()) { "Unable to build request: field '$fieldName' is empty" }
            is Long -> check(this >= 0) { "Unable to build request: field '$fieldName' is negative" }
        }
    }

    private fun makeToken(request: AcquiringRequest<R>): String {
        val parameters = request.asMap()

        parameters.remove(TOKEN)
        parameters[PASSWORD_KEY] = password

        val sortedKeys = ArrayList(parameters.keys)
        sortedKeys.sort()

        val builder = StringBuilder()
        val ignoredKes = request.tokenIgnoreFields
        for (key in sortedKeys) {
            if (!ignoredKes.contains(key)) {
                builder.append(parameters[key])
            }
        }

        return CryptoUtils.sha256(builder.toString())
    }

    internal companion object {

        const val TERMINAL_KEY = "TerminalKey"
        const val PASSWORD_KEY = "Password"
        const val PAYMENT_ID = "PaymentId"
        const val SEND_EMAIL = "SendEmail"
        const val TOKEN = "Token"
        const val EMAIL = "InfoEmail"
        const val CARD_DATA = "CardData"
        const val LANGUAGE = "Language"
        const val AMOUNT = "Amount"
        const val ORDER_ID = "OrderId"
        const val DESCRIPTION = "Description"
        const val PAY_FORM = "PayForm"
        const val CUSTOMER_KEY = "CustomerKey"
        const val RECURRENT = "Recurrent"
        const val REBILL_ID = "RebillId"
        const val CARD_ID = "CardId"
        const val CVV = "CVV"
        const val PAY_TYPE = "PayType"
        const val RECEIPT = "Receipt"
        const val RECEIPTS = "Receipts"
        const val SHOPS = "Shops"
        const val DATA = "DATA"
        const val CHARGE_FLAG = "chargeFlag"
        const val DATA_KEY_EMAIL = "Email"
        const val CHECK_TYPE = "CheckType"
        const val REQUEST_KEY = "RequestKey"
        const val SOURCE = "Source"
        const val PAYMENT_SOURCE = "PaymentSource"
        const val ANDROID_PAY_TOKEN = "EncryptedPaymentData"
        const val DATA_TYPE = "DataType"
        const val REDIRECT_DUE_DATE = "RedirectDueDate"
        const val IP = "IP"
    }
}
