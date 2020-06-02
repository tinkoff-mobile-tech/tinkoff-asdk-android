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

package ru.tinkoff.acquiring.sdk.localization.parsers

import com.google.gson.Gson
import ru.tinkoff.acquiring.sdk.localization.LocalizationParseException
import ru.tinkoff.acquiring.sdk.localization.LocalizationResources
import ru.tinkoff.acquiring.sdk.localization.LocalizationSource
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * @author Mariya Chernyadieva
 */
internal abstract class LocalizationParser<T : LocalizationSource>(private val source: T) {

    protected abstract fun getString(source: T): String

    @Throws(LocalizationParseException::class)
    fun parse(): LocalizationResources {
        return Gson().fromJson(getString(source), LocalizationResources::class.java)
    }

    @Throws(LocalizationParseException::class)
    protected fun InputStream.readStream(): String {
        val buffer = CharArray(1024)
        val result = StringBuilder()
        val inputStreamReader = InputStreamReader(this)
        var read: Int = -1

        try {
            this.use {
                while ({ read = inputStreamReader.read(buffer, 0, buffer.size); read }() != -1) {
                    result.append(buffer, 0, read)
                }
            }
        } catch (e: IOException) {
            throw LocalizationParseException(e)
        }

        return result.toString()
    }
}