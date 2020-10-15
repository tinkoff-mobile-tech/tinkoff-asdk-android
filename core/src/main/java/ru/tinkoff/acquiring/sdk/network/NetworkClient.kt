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
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.NetworkException
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.models.enums.Tax
import ru.tinkoff.acquiring.sdk.models.enums.Taxation
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.API_REQUEST_METHOD_POST
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.FORM_URL_ENCODED
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.JSON
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.STREAM_BUFFER_SIZE
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.TIMEOUT
import ru.tinkoff.acquiring.sdk.requests.AcquiringRequest
import ru.tinkoff.acquiring.sdk.requests.FinishAuthorizeRequest
import ru.tinkoff.acquiring.sdk.responses.AcquiringResponse
import ru.tinkoff.acquiring.sdk.responses.GetCardListResponse
import ru.tinkoff.acquiring.sdk.utils.serialization.CardStatusSerializer
import ru.tinkoff.acquiring.sdk.utils.serialization.CardsListDeserializer
import ru.tinkoff.acquiring.sdk.utils.serialization.PaymentStatusSerializer
import ru.tinkoff.acquiring.sdk.utils.serialization.SerializableExclusionStrategy
import ru.tinkoff.acquiring.sdk.utils.serialization.TaxSerializer
import ru.tinkoff.acquiring.sdk.utils.serialization.TaxationSerializer
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.lang.reflect.Modifier
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_OK
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder

/**
 * @author Mariya Chernyadieva
 */
internal class NetworkClient {

    private val gson: Gson = createGson()

    internal fun <R : AcquiringResponse> call(request: AcquiringRequest<R>,
                                              responseClass: Class<R>,
                                              onSuccess: (R) -> Unit,
                                              onFailure: (Exception) -> Unit) {

        val result: R
        var response: String? = null
        var responseReader: InputStreamReader? = null
        var requestContentStream: OutputStream? = null

        try {
            lateinit var connection: HttpURLConnection

            prepareBody(request) { body ->
                prepareConnection(request) {
                    connection = it
                    connection.setRequestProperty("Content-length", body.size.toString())
                    requestContentStream = connection.outputStream
                    requestContentStream?.write(body)

                    AcquiringSdk.log("=== Sending $API_REQUEST_METHOD_POST request to ${connection.url}")
                }
            }

            val responseCode = connection.responseCode

            if (responseCode == HTTP_OK) {
                responseReader = InputStreamReader(connection.inputStream)
                response = read(responseReader)
                AcquiringSdk.log("=== Got server response: $response")
                result = gson.fromJson(response, responseClass)

                checkResult(result) { isSuccess ->
                    if (!request.isDisposed()) {
                        if (isSuccess) {
                            AcquiringSdk.log("=== Request done with success, sent for processing")
                            onSuccess(result)
                        } else {
                            AcquiringSdk.log("=== Request done with fail")
                            onFailure(AcquiringApiException(result, "${result.message ?: ""} ${result.details ?: ""}"))
                        }
                    }
                }

            } else {
                responseReader = InputStreamReader(connection.errorStream)
                response = read(responseReader)
                if (response.isNotEmpty()) {
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
                onFailure(NetworkException("Unable to performRequest request ${request.apiMethod}", e))
            }
        } catch (e: JsonParseException) {
            if (!request.isDisposed()) {
                onFailure(AcquiringApiException("Invalid response. $response", e))
            }
        } finally {
            closeQuietly(responseReader)
            closeQuietly(requestContentStream)
        }
    }

    private fun <R : AcquiringResponse> prepareConnection(request: AcquiringRequest<R>, onReady: (HttpURLConnection) -> Unit) {
        val targetUrl = prepareURL(request.apiMethod)
        val connection = targetUrl.openConnection() as HttpURLConnection

        with(connection) {
            requestMethod = API_REQUEST_METHOD_POST
            connectTimeout = TIMEOUT
            readTimeout = TIMEOUT
            doOutput = true
            setRequestProperty("Content-type", if (AcquiringApi.useV1Api(request.apiMethod)) FORM_URL_ENCODED else JSON)

            if (request is FinishAuthorizeRequest && request.is3DsVersionV2()) {
                setRequestProperty("User-Agent", System.getProperty("http.agent"))
                setRequestProperty("Accept", JSON)
            }
        }

        onReady(connection)
    }

    private fun <R : AcquiringResponse> prepareBody(request: AcquiringRequest<R>, onReady: (ByteArray) -> Unit) {
        val requestBody = formatRequestBody(request.asMap(), request.apiMethod)
        AcquiringSdk.log("=== Parameters: $requestBody")

        onReady(requestBody.toByteArray())
    }

    private fun <R : AcquiringResponse> checkResult(result: R, onChecked: (isSuccess: Boolean) -> Unit) {
        if (result.errorCode == AcquiringApi.API_ERROR_CODE_NO_ERROR && result.isSuccess!!) {
            onChecked(true)
        } else {
            onChecked(false)
        }
    }

    @Throws(MalformedURLException::class)
    private fun prepareURL(apiMethod: String?): URL {
        if (apiMethod == null || apiMethod.isEmpty()) {
            throw IllegalArgumentException(
                    "Cannot prepare URL for request api method is empty or null!"
            )
        }

        val builder = StringBuilder(AcquiringApi.getUrl(apiMethod))
        builder.append("/")
        builder.append(apiMethod)

        return URL(builder.toString())
    }

    private fun formatRequestBody(params: Map<String, Any>?, apiMethod: String): String {
        if (params == null || params.isEmpty()) {
            return ""
        }
        return if (AcquiringApi.useV1Api(apiMethod)) {
            encodeRequestBody(params)
        } else {
            jsonRequestBody(params)
        }
    }

    private fun jsonRequestBody(params: Map<String, Any>): String {
        return gson.toJson(params)
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

    @Throws(IOException::class)
    private fun read(reader: InputStreamReader): String {
        val buffer = CharArray(STREAM_BUFFER_SIZE)
        var read: Int = -1
        val result = StringBuilder()

        while ({ read = reader.read(buffer, 0, STREAM_BUFFER_SIZE); read }() != -1) {
            result.append(buffer, 0, read)
        }

        return result.toString()
    }

    private fun closeQuietly(closeable: Closeable?) {
        if (closeable == null) {
            return
        }

        try {
            closeable.close()
        } catch (e: IOException) {
            AcquiringSdk.log(e)
        }
    }

    private fun createGson(): Gson {
        return GsonBuilder()
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