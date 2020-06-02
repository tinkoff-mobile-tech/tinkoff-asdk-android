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
import ru.tinkoff.acquiring.sdk.models.options.CustomerOptions
import ru.tinkoff.acquiring.sdk.models.options.FeaturesOptions

/**
 * Настройки экрана привязки карты
 *
 * @author Mariya Chernyadieva
 */
class AttachCardOptions() : BaseAcquiringOptions(), Parcelable {

    /**
     * Данные покупателя
     */
    lateinit var customer: CustomerOptions

    private constructor(parcel: Parcel) : this() {
        parcel.run {
            setTerminalParams(
                    terminalKey = readString() ?: "",
                    password = readString() ?: "",
                    publicKey = readString() ?: ""
            )
            customer = readParcelable(CustomerOptions::class.java.classLoader)!!
            features = readParcelable(FeaturesOptions::class.java.classLoader)!!
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.run {
            writeString(terminalKey)
            writeString(password)
            writeString(publicKey)
            writeParcelable(customer, flags)
            writeParcelable(features, flags)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    @Throws(IllegalStateException::class)
    override fun validateRequiredFields() {
        super.validateRequiredFields()
        check(::customer.isInitialized) { "Customer Options is not set" }
        customer.validateRequiredFields()
    }

    fun setOptions(options: AttachCardOptions.() -> Unit): AttachCardOptions {
        return AttachCardOptions().apply(options)
    }

    fun customerOptions(customerOptions: CustomerOptions.() -> Unit) {
        this.customer = CustomerOptions().apply(customerOptions)
    }

    fun featuresOptions(featuresOptions: FeaturesOptions.() -> Unit) {
        this.features = FeaturesOptions().apply(featuresOptions)
    }

    companion object CREATOR : Parcelable.Creator<AttachCardOptions> {
        override fun createFromParcel(parcel: Parcel): AttachCardOptions {
            return AttachCardOptions(parcel)
        }

        override fun newArray(size: Int): Array<AttachCardOptions?> {
            return arrayOfNulls(size)
        }
    }
}