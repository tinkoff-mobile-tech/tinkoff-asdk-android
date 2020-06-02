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

package ru.tinkoff.acquiring.sdk.responses

import com.google.gson.annotations.SerializedName
import ru.tinkoff.acquiring.sdk.models.PaymentInfo
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus

/**
 * Ответ на запрос Charge
 *
 * @param orderId   номер заказа в системе продавца
 * @param paymentId уникальный идентификатор транзакции в системе банка
 * @param amount    сумма списания в копейках
 * @param status    статус транзакции
 * @param cardId    идентификатор карты в системе банка, с которой производилось списание
 *
 * @author Mariya Chernyadieva
 */
class ChargeResponse(
        @SerializedName("OrderId")
        val orderId: String? = null,

        @SerializedName("PaymentId")
        val paymentId: Long? = null,

        @SerializedName("Amount")
        val amount: Long? = null,

        @SerializedName("Status")
        val status: ResponseStatus? = null,

        @SerializedName("CardId")
        val cardId: String? = null

) : AcquiringResponse() {

    @Transient
    private lateinit var paymentInfo: PaymentInfo

    fun getPaymentInfo(): PaymentInfo {
        paymentInfo = PaymentInfo(orderId, paymentId, amount, cardId, errorCode)
        return paymentInfo
    }
}

