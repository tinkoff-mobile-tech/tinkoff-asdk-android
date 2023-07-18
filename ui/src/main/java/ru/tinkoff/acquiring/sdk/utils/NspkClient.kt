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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import okhttp3.OkHttpClient
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.NetworkException
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.responses.NspkC2bResponse
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

/**
 * @author Mariya Chernyadieva
 */
internal class NspkClient {

    companion object {
        private const val NSPK_ANDROID_APPS_URL = "https://qr.nspk.ru/proxyapp/c2bmembers.json"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(40000, TimeUnit.MILLISECONDS)
        .readTimeout(40000, TimeUnit.MILLISECONDS)
        .build()

    private val gson: Gson = GsonBuilder().create()

    fun call(request: Request<NspkC2bResponse>, onSuccess: (NspkC2bResponse) -> Unit, onFailure: (Exception) -> Unit) {

        val okHttpRequest = okhttp3.Request.Builder().url(NSPK_ANDROID_APPS_URL).get()
            .header("User-Agent", System.getProperty("http.agent")!!)
            .header("Accept", AcquiringApi.JSON)
            .build()
        val call = okHttpClient.newCall(okHttpRequest)
        AcquiringSdk.log("=== Sending GET request to $NSPK_ANDROID_APPS_URL")
        val okHttpResponse = call.execute()
        val responseCode = okHttpResponse.code
        val response: String = checkNotNull(okHttpResponse.body?.string())

        try {
            AcquiringSdk.log("=== Got server response code: $responseCode")
            if (responseCode == HttpURLConnection.HTTP_OK) {
                AcquiringSdk.log("=== Got server response: $response")
                val info = serializeData(response)
                if (!request.isDisposed()) {
                    onSuccess(info)
                }
            } else {
                AcquiringSdk.log("=== Got server response: $response")
                if (!request.isDisposed()) {
                    onFailure(NetworkException("Got server error response code $responseCode"))
                }
            }

        } catch (e: IOException) {
            AcquiringSdk.log("=== handle error on GET request to $NSPK_ANDROID_APPS_URL")
            if (!request.isDisposed()) {
                onFailure(e)
            }
        } catch (e: JsonParseException) {
            AcquiringSdk.log("=== handle error on GET request to $NSPK_ANDROID_APPS_URL")
            if (!request.isDisposed()) {
                onFailure(e)
            }
        }
    }

    private fun serializeData(response: String): NspkC2bResponse {
       return gson.fromJson(response, NspkC2bResponse::class.java)
    }
}
