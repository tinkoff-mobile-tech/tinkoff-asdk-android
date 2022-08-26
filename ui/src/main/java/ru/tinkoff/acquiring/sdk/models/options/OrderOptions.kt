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

package ru.tinkoff.acquiring.sdk.models.options

import android.os.Parcel
import android.os.Parcelable
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.models.Receipt
import ru.tinkoff.acquiring.sdk.models.Shop
import ru.tinkoff.acquiring.sdk.utils.Money
import ru.tinkoff.acquiring.sdk.utils.readParcelList
import ru.tinkoff.acquiring.sdk.utils.readParcelMap

/**
 * Данные заказа
 *
 * @author Mariya Chernyadieva
 */
class OrderOptions() : Options(), Parcelable {

    /**
     * Номер заказа в системе продавца. Максимальная длина - 20 символов
     */
    lateinit var orderId: String

    /**
     * Сумма в копейках
     */
    lateinit var amount: Money

    /**
     * Указывает, что совершается рекуррентный или не рекуррентный платеж
     */
    var recurrentPayment: Boolean = false

    /**
     * Наименоварие заказа
     */
    var title: String? = null

    /**
     * Описание заказа, максимальная длина - 250 символов
     */
    var description: String? = null

    /**
     * Объект с данными чека
     */
    var receipt: Receipt? = null

    /**
     * Список с данными магазинов
     */
    var shops: List<Shop>? = null

    /**
     * Список с данными чеков
     */
    var receipts: List<Receipt>? = null

    /**
     * Страница успеха
     */
    var successURL: String? = null

    /**
     * Страница ошибки
     */
    var failURL: String? = null

    /**
     * Объект содержащий дополнительные параметры в виде "ключ":"значение".
     * Данные параметры будут переданы в запросе платежа/привязки карты.
     * Максимальная длина для каждого передаваемого параметра:
     * Ключ – 20 знаков,
     * Значение – 100 знаков.
     * Максимальное количество пар "ключ-значение" не может превышать 20
     */
    var additionalData: Map<String, String>? = null

    private constructor(parcel: Parcel) : this() {
        parcel.run {
            orderId = readString() ?: ""
            amount = readSerializable() as Money
            title = readString()
            description = readString()
            recurrentPayment = readByte().toInt() != 0
            receipt = readSerializable() as Receipt?
            shops = readParcelList(Shop::class.java)
            receipts = readParcelList(Receipt::class.java)
            successURL = readString()
            failURL = readString()
            additionalData = readParcelMap(String::class.java)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.run {
            writeString(orderId)
            writeSerializable(amount)
            writeString(title)
            writeString(description)
            writeByte((if (recurrentPayment) 1 else 0).toByte())
            writeSerializable(receipt)
            writeList(shops)
            writeList(receipts)
            writeString(successURL)
            writeString(failURL)
            writeMap(additionalData)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    @Throws(AcquiringSdkException::class)
    override fun validateRequiredFields() {
        check(::orderId.isInitialized) { "Order Id is not set" }
        check(::amount.isInitialized) { "Amount is not set" }
        check(orderId.isNotEmpty()) { "Order Id should not be empty" }
        check(amount.coins > 0L) { "Amount value cannot be less than 0" }
    }

    companion object CREATOR : Parcelable.Creator<OrderOptions> {
        override fun createFromParcel(parcel: Parcel): OrderOptions {
            return OrderOptions(parcel)
        }

        override fun newArray(size: Int): Array<OrderOptions?> {
            return arrayOfNulls(size)
        }
    }
}