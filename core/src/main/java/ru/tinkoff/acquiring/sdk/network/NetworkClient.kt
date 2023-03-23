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

package ru.tinkoff.acquiring.sdk.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.NetworkException
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.models.enums.Tax
import ru.tinkoff.acquiring.sdk.models.enums.Taxation
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.JSON
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.TIMEOUT
import ru.tinkoff.acquiring.sdk.requests.AcquiringRequest
import ru.tinkoff.acquiring.sdk.requests.FinishAuthorizeRequest
import ru.tinkoff.acquiring.sdk.responses.AcquiringResponse
import ru.tinkoff.acquiring.sdk.responses.GetCardListResponse
import ru.tinkoff.acquiring.sdk.utils.serialization.*
import java.io.*
import java.lang.reflect.Modifier
import java.net.HttpURLConnection.HTTP_OK
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * @author Mariya Chernyadieva, Taras Nagorny
 */
internal class NetworkClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
        .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
        .build()

    private val gson: Gson = createGson()

    internal fun <R : AcquiringResponse> call(
        request: AcquiringRequest<R>,
        responseClass: Class<R>,
        onSuccess: (R) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        var response: String? = null

        try {
            val httpRequest = request.httpRequest()
            val call = okHttpClient.newCall(httpRequest)
            val httpResponse = call.execute()

            AcquiringSdk.log("=== Sending ${httpRequest.method} request to ${httpRequest.url}")

            val responseCode = httpResponse.code
            response = httpResponse.body?.string()

            if (responseCode == HTTP_OK) {
                AcquiringSdk.log("=== Got server response: $response")
                val result = gson.fromJson(response, responseClass)

                checkResult(result) { isSuccess ->
                    if (!request.isDisposed()) {
                        if (isSuccess) {
                            AcquiringSdk.log("=== Request done with success, sent for processing")
                            onSuccess(result)
                        } else {
                            AcquiringSdk.log("=== Request done with fail")
                            onFailure(
                                AcquiringApiException(
                                    result,
                                    makeNetworkErrorMessage(result.message, result.details)
                                )
                            )
                        }
                    }
                }

            } else {
                if (!response.isNullOrEmpty()) {
                    AcquiringSdk.log("=== Got server error response: $response")
                } else {
                    AcquiringSdk.log("=== Got server error response code: $responseCode")
                }
                if (!request.isDisposed()) {
                    onFailure(NetworkException("Unable to performRequest request ${request.apiMethod}"))
                }
            }

        } catch (e: IOException) {
            if (!request.isDisposed()) {
                onFailure(
                    NetworkException(
                        "Unable to performRequest request ${request.apiMethod}",
                        e
                    )
                )
            }
        } catch (e: JsonParseException) {
            if (!request.isDisposed()) {
                onFailure(AcquiringApiException("Invalid response. $response", e))
            }
        }
    }

    internal fun <R : AcquiringResponse> callRaw(
        request: AcquiringRequest<R>
    ): Response {
        return try {
            val httpRequest = request.httpRequest()
            val call = okHttpClient.newCall(httpRequest)
            AcquiringSdk.log("=== Sending ${httpRequest.method} request to ${httpRequest.url}")
            call.execute()
        } catch (e: IOException) {
            AcquiringSdk.log("=== Receive error ${e.message} on method ${request.httpRequestMethod} ${request.apiMethod}")
            throw NetworkException("Unable to performRequest request ${request.apiMethod}", e)
        }
    }

    private fun AcquiringRequest<*>.httpRequest() = Request.Builder().also { builder ->
        builder.url(prepareURL(apiMethod))
        when (httpRequestMethod) {
            AcquiringApi.API_REQUEST_METHOD_GET -> builder.get()
            AcquiringApi.API_REQUEST_METHOD_POST -> {
                val body = getRequestBody()
                AcquiringSdk.log("=== Parameters: $body")
                builder.post(body.toRequestBody(contentType.toMediaType()))
            }
        }

        if (this is FinishAuthorizeRequest && is3DsVersionV2()) {
            builder.header("User-Agent", System.getProperty("http.agent"))
            builder.header("Accept", JSON)
        }

        getHeaders().forEach { (key, value) ->
            builder.header(key, value)
        }

    }.build()

    private fun <R : AcquiringResponse> checkResult(
        result: R,
        onChecked: (isSuccess: Boolean) -> Unit
    ) {
        if (result.errorCode == AcquiringApi.API_ERROR_CODE_NO_ERROR && result.isSuccess!!) {
            onChecked(true)
        } else {
            onChecked(false)
        }
    }

    @Throws(MalformedURLException::class)
    private fun prepareURL(apiMethod: String?): URL {
        if (apiMethod.isNullOrEmpty()) {
            throw IllegalArgumentException(
                "Cannot prepare URL for request api method is empty or null!"
            )
        }

        val builder = StringBuilder(AcquiringApi.getUrl(apiMethod))
        builder.append("/")
        builder.append(apiMethod)

        return URL(builder.toString())
    }

    private fun makeNetworkErrorMessage(message : String?, details: String?): String {
        return setOf(message.orEmpty(), details.orEmpty()).joinToString()
    }

    companion object {

        fun createGson(): Gson {
            return GsonBuilder()
                .disableHtmlEscaping()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
                .setExclusionStrategies(SerializableExclusionStrategy())
                .registerTypeAdapter(CardStatus::class.java, CardStatusSerializer())
                .registerTypeAdapter(ResponseStatus::class.java, PaymentStatusSerializer())
                .registerTypeAdapter(GetCardListResponse::class.java, CardsListDeserializer())
                .registerTypeAdapter(Tax::class.java, TaxSerializer())
                .registerTypeAdapter(Taxation::class.java, TaxationSerializer())
                .create()
        }
    }
}