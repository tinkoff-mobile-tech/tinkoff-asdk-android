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

import kotlinx.coroutines.flow.Flow
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.GET_CARD_LIST_METHOD
import ru.tinkoff.acquiring.sdk.responses.GetCardListResponse
import ru.tinkoff.acquiring.sdk.utils.RequestResult

/**
 * Возвращает список привязанных карт у покупателя
 *
 * @author Mariya Chernyadieva
 */
class GetCardListRequest : AcquiringRequest<GetCardListResponse>(GET_CARD_LIST_METHOD) {

    /**
     * Идентификатор покупателя в системе продавца
     */
    var customerKey: String? = null

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()

        map.putIfNotNull(CUSTOMER_KEY, customerKey)

        return map
    }

    override fun validate() {
        customerKey.validate(CUSTOMER_KEY)
    }

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (GetCardListResponse) -> Unit, onFailure: (Exception) -> Unit) {
        super.performRequest(this, GetCardListResponse::class.java, onSuccess, onFailure)
    }

    /**
     * Реактивный вызов метода API
     */
    fun executeFlow(): Flow<RequestResult<out GetCardListResponse>> {
        return super.performRequestFlow(this, GetCardListResponse::class.java)
    }
}