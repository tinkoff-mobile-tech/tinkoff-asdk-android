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

import ru.tinkoff.acquiring.sdk.network.AcquiringApi.GET_STATE_METHOD
import ru.tinkoff.acquiring.sdk.responses.GetStateResponse

/**
 * Возвращает статус платежа
 *
 * @author Mariya Chernyadieva
 */
class GetStateRequest : AcquiringRequest<GetStateResponse>(GET_STATE_METHOD) {

    /**
     * Уникальный идентификатор транзакции в системе банка
     */
    var paymentId: Long? = null

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()

        map.putIfNotNull(PAYMENT_ID, paymentId.toString())

        return map
    }

    override fun validate() {
        paymentId.validate(PAYMENT_ID)
    }

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (GetStateResponse) -> Unit, onFailure: (Exception) -> Unit) {
        super.performRequest(this, GetStateResponse::class.java, onSuccess, onFailure)
    }
}
