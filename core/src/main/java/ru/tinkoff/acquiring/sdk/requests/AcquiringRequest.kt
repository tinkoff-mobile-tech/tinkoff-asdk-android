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

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import okhttp3.Response
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.NetworkException
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.JSON
import ru.tinkoff.acquiring.sdk.network.NetworkClient
import ru.tinkoff.acquiring.sdk.responses.AcquiringResponse
import ru.tinkoff.acquiring.sdk.utils.Request
import ru.tinkoff.acquiring.sdk.utils.RequestResult
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.security.PublicKey

/**
 * Базовый класс создания запроса Acquiring API
 *
 * @author Mariya Chernyadieva, Taras Nagorny
 */
abstract class AcquiringRequest<R : AcquiringResponse>(internal val apiMethod: String) :
    Request<R> {

    protected val gson: Gson = NetworkClient.createGson()

    open val httpRequestMethod: String = AcquiringApi.API_REQUEST_METHOD_POST
    open val contentType: String = AcquiringApi.JSON

    internal lateinit var terminalKey: String
    internal lateinit var publicKey: PublicKey

    @Volatile
    private var disposed = false
    private val ignoredFieldsSet: HashSet<String> = hashSetOf(DATA, RECEIPT, RECEIPTS, SHOPS)
    private val headersMap: HashMap<String, String> = hashMapOf()

    internal open val tokenIgnoreFields: HashSet<String>
        get() = ignoredFieldsSet

    protected abstract fun validate()

    override fun isDisposed(): Boolean {
        return disposed
    }

    override fun dispose() {
        disposed = true
    }

    open fun asMap(): MutableMap<String, Any> {
        val map = HashMap<String, Any>()

        map.putIfNotNull(TERMINAL_KEY, terminalKey)

        return map
    }

    protected fun <R : AcquiringResponse> performRequest(
        request: AcquiringRequest<R>,
        responseClass: Class<R>,
        onSuccess: (R) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        request.validate()
        val client = NetworkClient()
        client.call(request, responseClass, onSuccess, onFailure)
    }

    open fun performRequestAsync(responseClass: Class<R>): Deferred<Result<R>> {
        this.validate()
        val client = NetworkClient()
        val deferred: CompletableDeferred<Result<R>> = CompletableDeferred()

        client.call(this, responseClass,
            onSuccess = {
                deferred.complete(Result.success(it))
            },
            onFailure = {
                deferred.complete(Result.failure(it))
            })
        return deferred
    }

    suspend fun performSuspendRequest(responseClass: Class<R>): Result<R> {
        return performRequestAsync(responseClass).run {
            start()
            await()
        }
    }

    protected fun <R : AcquiringResponse> performRequestFlow(request: AcquiringRequest<R>,
                                                             responseClass: Class<R>) : Flow<RequestResult<out R>> {
        request.validate()
        val client = NetworkClient()
        val flow = MutableStateFlow<RequestResult<out R>>(RequestResult.NotYet)
        client.call(request, responseClass,
            onSuccess = { flow.tryEmit(RequestResult.Success(it)) },
            onFailure = { flow.tryEmit(RequestResult.Failure(it)) }
        )
        return flow
    }

    @kotlin.jvm.Throws(NetworkException::class)
    protected fun <R : AcquiringResponse> performRequestRaw(request: AcquiringRequest<R>): Response {
        request.validate()
        val client = NetworkClient()
        return client.callRaw(request)
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

    open fun getRequestBody(): String {
        val params = asMap()
        if (params.isEmpty()) return ""

        getToken()?.let { params[TOKEN] = it }

        return when (contentType) {
            AcquiringApi.FORM_URL_ENCODED -> encodeRequestBody(params)
            else -> gson.toJson(params)
        }
    }

    fun addUserAgentHeader(userAgent: String = System.getProperty("http.agent")) {
        headersMap.put("User-Agent", userAgent)
    }

    fun addContentHeader(content: String = JSON) {
        headersMap.put("Accept", content)
    }

    protected open fun getToken(): String? =
        AcquiringSdk.tokenGenerator?.generateToken(this, paramsForToken())

    internal fun getHeaders() = headersMap

    private fun paramsForToken(): MutableMap<String, Any> {
        val tokenParams = asMap()
        tokenIgnoreFields.forEach {
            tokenParams.remove(it)
        }
        tokenParams.remove(TOKEN)
        return tokenParams
    }

    private fun encodeRequestBody(params: Map<String, Any>): String {
        val builder = StringBuilder()
        for ((key, value1) in params) {
            try {
                val value = URLEncoder.encode(value1.toString(), "UTF-8")
                builder.append(key)
                builder.append('=')
                builder.append(value)
                builder.append('&')
            } catch (e: UnsupportedEncodingException) {
                AcquiringSdk.log(e)
            }
        }

        builder.setLength(builder.length - 1)

        return builder.toString()
    }

    internal companion object {

        const val TERMINAL_KEY = "TerminalKey"
        const val PASSWORD = "Password"
        const val TOKEN = "Token"
        const val PAYMENT_ID = "PaymentId"
        const val SEND_EMAIL = "SendEmail"
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
        const val ENCRYPTED_PAYMENT_DATA = "EncryptedPaymentData"
        const val DATA_TYPE = "DataType"
        const val REDIRECT_DUE_DATE = "RedirectDueDate"
        const val NOTIFICATION_URL = "NotificationURL"
        const val SUCCESS_URL = "SuccessURL"
        const val FAIL_URL = "FailURL"
        const val IP = "IP"
        const val CONNECTION_TYPE = "connection_type"
        const val SDK_VERSION = "sdk_version"
        const val SOFTWARE_VERSION = "software_version"
        const val DEVICE_MODEL = "device_model"
        const val THREE_DS_SERVER_TRANS_ID = "threeDSServerTransID"
        const val TRANS_STATUS = "transStatus"
        const val CRES = "cres"
        const val PAYSOURCE = "Paysource"
    }
}

suspend inline fun <reified R : AcquiringResponse> AcquiringRequest<R>.performSuspendRequest(): Result<R> {
    return performSuspendRequest(R::class.java)
}