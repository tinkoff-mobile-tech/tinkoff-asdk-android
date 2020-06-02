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

import com.google.gson.annotations.SerializedName

/**
 * Тип оплаты
 *
 * @author Mariya Chernyadieva
 */
enum class PaymentMethod {

    /**
     * Предоплата 100%
     * Полная предварительная оплата до момента передачи предмета расчета
     */
    @SerializedName("full_prepayment")
    FULL_PREPAYMENT,

    /**
     * Предоплата.
     * Частичная предварительная оплата до момента передачи предмета расчета
     */
    @SerializedName("prepayment")
    PREPAYMENT,

    /**
     * Аванс
     */
    @SerializedName("advance")
    ADVANCE,

    /**
     * Полный расчет.
     * Полная оплата, в том числе с учетом аванса (предварительной оплаты) в момент передачи
     */
    @SerializedName("full_payment")
    FULL_PAYMENT,

    /**
     * Частичный расчет и кредит.
     * Частичная оплата предмета расчета в момент его передачи с последующей оплатой в кредит
     */
    @SerializedName("partial_payment")
    PARTIAL_PAYMENT,

    /**
     * Передача в кредит.
     * Передача предмета расчета без его оплаты в момент его передачи с последующей оплатой в кредит
     */
    @SerializedName("credit")
    CREDIT,

    /**
     * Оплата кредита.
     * Оплата предмета расчета после его передачи с оплатой в кредит
     */
    @SerializedName("credit_payment")
    CREDIT_PAYMENT
}
