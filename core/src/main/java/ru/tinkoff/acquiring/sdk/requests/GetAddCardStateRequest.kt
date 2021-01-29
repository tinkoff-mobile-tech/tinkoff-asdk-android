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

import ru.tinkoff.acquiring.sdk.network.AcquiringApi.GET_ADD_CARD_STATE_METHOD
import ru.tinkoff.acquiring.sdk.responses.GetAddCardStateResponse

/**
 * Возвращает статус привязки карты
 *
 * @author Mariya Chernyadieva
 */
class GetAddCardStateRequest : AcquiringRequest<GetAddCardStateResponse>(GET_ADD_CARD_STATE_METHOD) {

    /**
     * Ключ, полученный при запросе [AddCardRequest]
     */
    var requestKey: String? = null

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()

        map.putIfNotNull(REQUEST_KEY, requestKey)

        return map
    }

    override fun validate() {
        requestKey.validate(REQUEST_KEY)
    }

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (GetAddCardStateResponse) -> Unit, onFailure: (Exception) -> Unit) {
        super.performRequest(this, GetAddCardStateResponse::class.java, onSuccess, onFailure)
    }
}