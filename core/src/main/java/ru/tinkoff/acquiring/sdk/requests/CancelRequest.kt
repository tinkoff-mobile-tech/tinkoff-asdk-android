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

package ru.tinkoff.acquiring.sdk.requests

import ru.tinkoff.acquiring.sdk.models.Receipt
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.CANCEL_METHOD
import ru.tinkoff.acquiring.sdk.responses.CancelResponse

/**
 * Метод отменяет платеж
 *
 * @author Taras Nagorny
 */
class CancelRequest : AcquiringRequest<CancelResponse>(CANCEL_METHOD) {

    /**
     * Идентификатор платежа в системе банка
     */
    var paymentId: Long? = null

    /**
     * Сумма возврата в копейках
     */
    var amount: Long = 0

    /**
     * Объект с данными чека
     */
    var receipt: Receipt? = null

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()

        map.putIfNotNull(PAYMENT_ID, paymentId.toString())
        map.putIfNotNull(AMOUNT, amount.toString())
        map.putIfNotNull(RECEIPT, receipt)

        return map
    }

    override fun validate() {
        paymentId.validate(PAYMENT_ID)
    }

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (CancelResponse) -> Unit, onFailure: (Exception) -> Unit) {
        super.performRequest(this, CancelResponse::class.java, onSuccess, onFailure)
    }
}
