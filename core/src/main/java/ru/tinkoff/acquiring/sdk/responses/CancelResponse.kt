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
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus

/**
 * Ответ на запрос Cancel
 *
 * @param orderId           идентификатор заказа в системе продавца
 * @param status            статус платежа
 * @param paymentId         уникальный идентификатор транзакции в системе банка
 * @param originalAmount    сумма до возврата в копейках
 * @param newAmount         сумма после возврата в копейках
 *
 * @author Taras Nagorny
 */
class CancelResponse(

        @SerializedName("OrderId")
        val orderId: String? = null,

        @SerializedName("Status")
        val status: ResponseStatus? = null,

        @SerializedName("PaymentId")
        val paymentId: Long? = null,

        @SerializedName("OriginalAmount")
        val originalAmount: Long? = null,

        @SerializedName("NewAmount")
        val newAmount: Long? = null

) : AcquiringResponse()
