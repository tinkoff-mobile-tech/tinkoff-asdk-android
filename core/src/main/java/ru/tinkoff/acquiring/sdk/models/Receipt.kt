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

package ru.tinkoff.acquiring.sdk.models

import com.google.gson.annotations.SerializedName
import ru.tinkoff.acquiring.sdk.models.enums.Taxation
import java.io.Serializable
import java.util.*

/**
 * Данные чека
 *
 * @author Mariya Chernyadieva
 */
enum class FfdVersion(val value: String) {
    VERSION1_05("1.05"), VERSION1_2("1.2");
}

sealed class Receipt : Serializable

/**
 * По умолчанию версия ФФД - '1.05'
 */
data class ReceiptFfd105(

    /**
     * Код магазина
     */
    @SerializedName("ShopCode")
    var shopCode: String? = null,

    /**
     * Электронный адрес для отправки чека покупателю
     */
    @SerializedName("Email")
    var email: String? = null,

    /**
     * Телефон покупателя
     */
    @SerializedName("Phone")
    var phone: String? = null,

    /**
     * Система налогообложения
     */
    @SerializedName("Taxation")
    var taxation: Taxation? = null,

    /**
     * Массив, содержащий в себе информацию о товарах
     */
    @SerializedName("Items")
    var items: ArrayList<Item> = arrayListOf(),

    /**
     * Данные агента
     */
    @SerializedName("AgentData")
    var agentData: AgentData? = null,

    /**
     * Данные поставщика платежного агента
     */
    @SerializedName("SupplierInfo")
    var supplierInfo: SupplierInfo? = null

) : Receipt() {

    val ffdVersion: FfdVersion = FfdVersion.VERSION1_05

    fun item(item: Item.() -> Unit) {
        items.add(Item().apply(item))
    }
}

data class ReceiptFfd12(

    /**
     * Общие проперти
     */
    val base: ReceiptFfd105,

    /**
     * Идентификатор покупателя
     */
    @SerializedName("Customer")
    var customer: String? = null,

    /**
     * Инн покупателя. Если ИНН иностранного гражданина, необходимо указать 00000000000
     */
    @SerializedName("CustomerInn")
    var customerInn: String? = null

) : Receipt() {

    val ffdVersion: FfdVersion = FfdVersion.VERSION1_2

    fun item(item: Item.() -> Unit) {
        base.items.add(Item().apply(item))
    }
}
