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

package ru.tinkoff.acquiring.sdk.models.options.screen

import android.os.Parcel
import android.os.Parcelable
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.models.options.FeaturesOptions
import ru.tinkoff.acquiring.sdk.models.options.Options

/**
 * Содержит базовые настройки
 *
 * @author Mariya Chernyadieva
 */
open class BaseAcquiringOptions() : Options(), Parcelable {

    lateinit var terminalKey: String
        private set
    lateinit var publicKey: String
        private set

    /**
     * Настройки для конфигурирования визуального отображения и функций экрана оплаты
     */
    var features: FeaturesOptions = FeaturesOptions()

    private constructor(parcel: Parcel) : this() {
        parcel.run {
            terminalKey = readString() ?: ""
            publicKey = readString() ?: ""
            features = readParcelable(FeaturesOptions::class.java.classLoader)!!
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.run {
            writeString(terminalKey)
            writeString(publicKey)
            writeParcelable(features, flags)
        }
    }

    @Throws(AcquiringSdkException::class)
    override fun validateRequiredFields() {
        check(terminalKey.isNotEmpty()) { "Terminal Key should not be empty" }
        check(publicKey.isNotEmpty()) { "Public Key should not be empty" }
    }

    fun setTerminalParams(terminalKey: String, publicKey: String) {
        this.terminalKey = terminalKey
        this.publicKey = publicKey
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BaseAcquiringOptions> {
        override fun createFromParcel(parcel: Parcel): BaseAcquiringOptions {
            return BaseAcquiringOptions(parcel)
        }

        override fun newArray(size: Int): Array<BaseAcquiringOptions?> {
            return arrayOfNulls(size)
        }
    }
}