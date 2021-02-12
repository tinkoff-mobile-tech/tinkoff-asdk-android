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

package ru.tinkoff.acquiring.sdk.models.enums

/**
 * Тип проведения платежа
 *
 * @author Mariya Chernyadieva
 */
enum class PayType(private val value: String) {

    /**
     * Одностадийный.
     * Платёж сразу списывается с карты в пользу продавца
     */
    ONE_STEP("O"),

    /**
     * Двухстадийный.
     * Деньги блокируются (холдируются) на карте клиента, для списания средств необходим вызов
     * дополнительного метода или подтверждение платежа в Личном кабинете
     */
    TWO_STEP("T");

    override fun toString(): String {
        return value
    }
}
