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
import ru.tinkoff.acquiring.sdk.exceptions.NetworkException
import ru.tinkoff.acquiring.sdk.models.NspkResponse
import ru.tinkoff.acquiring.sdk.responses.NspkC2bResponse
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * @author Mariya Chernyadieva
 */
internal class NspkClient {

    companion object {
        private const val NSPK_ANDROID_APPS_URL = "https://qr.nspk.ru/proxyapp/c2bmembers.json"
        private const val STREAM_BUFFER_SIZE = 4096
    }

    private val gson: Gson = GsonBuilder().create()

    fun call(request: Request<NspkC2bResponse>, onSuccess: (NspkC2bResponse) -> Unit, onFailure: (Exception) -> Unit) {
        var responseReader: InputStreamReader? = null

        try {
            val targetUrl = URL(NSPK_ANDROID_APPS_URL)
            val connection = targetUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                responseReader = InputStreamReader(connection.inputStream)
                val response = read(responseReader)
                val nspkInfo = serializeData(response)
                if (!request.isDisposed()) {
                    onSuccess(nspkInfo)
                }
            } else {
                if (!request.isDisposed()) {
                    onFailure(NetworkException("Got server error response code $responseCode"))
                }
            }

        } catch (e: IOException) {
            if (!request.isDisposed()) {
                onFailure(e)
            }
        } catch (e: JsonParseException) {
            if (!request.isDisposed()) {
                onFailure(e)
            }
        } finally {
            responseReader?.close()
        }
    }

    private fun serializeData(response: String): NspkC2bResponse {
       return gson.fromJson(response, NspkC2bResponse::class.java)
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
}