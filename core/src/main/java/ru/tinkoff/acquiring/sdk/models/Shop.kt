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
import java.io.Serializable

/**
 * Данные магазина
 *
 * @author Mariya Chernyadieva
 */
class Shop() : Serializable {

    /**
     * Код магазина
     */
    @SerializedName("ShopCode")
    var shopCode: String? = null

    /**
     * Наименование позиции
     */
    @SerializedName("Name")
    var name: String? = null

    /**
     * Сумма в копейках, которая относится к указанному в ShopCode партнеру
     */
    @SerializedName("Amount")
    var amount: Long = 0

    /**
     * Сумма комиссии в копейках, удерживаемая из возмещения партнера в пользу маркетплейса.
     * Если не передано, используется комиссия, указанная при регистрации
     */
    @SerializedName("Fee")
    var fee: String? = null

    constructor(shopCode: String, name: String, amount: Long) : this(shopCode, name, amount, null)

    constructor (shopCode: String, name: String, amount: Long, fee: String?) : this() {
        this.shopCode = shopCode
        this.name = name
        this.amount = amount
        this.fee = fee
    }
}
