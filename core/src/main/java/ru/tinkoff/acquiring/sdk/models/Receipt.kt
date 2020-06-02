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
class Receipt() : Serializable {

    /**
     * Код магазина
     */
    @SerializedName("ShopCode")
    var shopCode: String? = null

    /**
     * Электронный адрес для отправки чека покупателю
     */
    @SerializedName("Email")
    var email: String? = null

    /**
     * Система налогообложения
     */
    @SerializedName("Taxation")
    var taxation: Taxation? = null

    /**
     * Телефон покупателя
     */
    @SerializedName("Phone")
    var phone: String? = null

    /**
     * Массив, содержащий в себе информацию о товарах
     */
    @SerializedName("Items")
    var items: ArrayList<Item> = arrayListOf()

    /**
     * Данные агента
     */
    @SerializedName("AgentData")
    var agentData: AgentData? = null

    /**
     * Данные поставщика платежного агента
     */
    @SerializedName("SupplierInfo")
    var supplierInfo: SupplierInfo? = null

    /**
     * Идентификатор покупателя
     */
    @SerializedName("Сustomer")
    var customer: String? = null

    /**
     * Инн покупателя. Если ИНН иностранного гражданина, необходимо указать 00000000000
     */
    @SerializedName("СustomerInn")
    var customerInn: String? = null


    constructor(items: ArrayList<Item>, email: String, taxation: Taxation) : this() {
        this.items = items
        this.email = email
        this.taxation = taxation
    }

    constructor (shopCode: String, items: ArrayList<Item>, email: String, taxation: Taxation) : this() {
        this.shopCode = shopCode
        this.items = items
        this.email = email
        this.taxation = taxation
    }

    fun item(item: Item.() -> Unit) {
        items.add(Item().apply(item))
    }
}
