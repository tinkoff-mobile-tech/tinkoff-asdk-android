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
import ru.tinkoff.acquiring.sdk.models.enums.PaymentMethod
import ru.tinkoff.acquiring.sdk.models.enums.PaymentObject
import ru.tinkoff.acquiring.sdk.models.enums.Tax
import java.io.Serializable
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Информация о товаре
 *
 * @author Mariya Chernyadieva
 */
class Item() : Serializable {

    /**
     * Сумма в копейках. Целочисленное значение не более 10 знаков
     */
    @SerializedName("Price")
    var price: Long = 0

    /**
     * Количество/вес. Целая часть не более 8 знаков
     */
    @SerializedName("Quantity")
    var quantity: Double = 0.0

    /**
     * Наименование товара. Максимальная длина строки – 128 символов
     */
    @SerializedName("Name")
    var name: String? = null

    /**
     * Сумма в копейках. Целочисленное значение не более 10 знаков
     */
    @SerializedName("Amount")
    var amount: Long? = null

    /**
     * Ставка налога
     */
    @SerializedName("Tax")
    var tax: Tax? = null

    /**
     * Штрих-код
     */
    @SerializedName("Ean13")
    var ean13: String? = null

    /**
     * Код магазина
     */
    @SerializedName("ShopCode")
    var shopCode: String? = null

    /**
     * Тип оплаты
     */
    @SerializedName("PaymentMethod")
    var paymentMethod: PaymentMethod? = null

    /**
     * Признак предмета расчета
     */
    @SerializedName("PaymentObject")
    var paymentObject: PaymentObject? = null

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

    init {
        this.quantity = round(quantity, QUANTITY_SCALE_FACTOR)
    }

    constructor(name: String?, price: Long?, quantity: Double?, amount: Long?, tax: Tax?) : this() {
        this.name = name
        this.price = price ?: 0
        this.quantity = round(quantity ?: 0.0, QUANTITY_SCALE_FACTOR)
        this.amount = amount
        this.tax = tax
    }

    fun agentData(agentData: AgentData.() -> Unit) {
        this.agentData = AgentData().apply(agentData)
    }

    fun supplierInfo(supplierInfo: SupplierInfo.() -> Unit) {
        this.supplierInfo = SupplierInfo().apply(supplierInfo)
    }

    companion object {

        private const val QUANTITY_SCALE_FACTOR = 3

        private fun round(value: Double, scale: Int): Double {
            return (value * 10.0.pow(scale.toDouble())).roundToInt() / 10.0.pow(scale.toDouble())
        }
    }
}
