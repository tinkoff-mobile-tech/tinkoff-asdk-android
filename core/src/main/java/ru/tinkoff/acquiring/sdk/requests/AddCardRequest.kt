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

import ru.tinkoff.acquiring.sdk.network.AcquiringApi.ADD_CARD_METHOD
import ru.tinkoff.acquiring.sdk.responses.AddCardResponse

/**
 * Инициирует привязку карты к покупателю
 *
 * @author Mariya Chernyadieva
 */
class AddCardRequest : AcquiringRequest<AddCardResponse>(ADD_CARD_METHOD) {

    /**
     * Идентификатор покупателя в системе продавца
     */
    var customerKey: String? = null

    /**
     * Тип проверки при привязке карты
     */
    var checkType: String? = null

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()

        map.putIfNotNull(CUSTOMER_KEY, customerKey)
        map.putIfNotNull(CHECK_TYPE, checkType)

        return map
    }

    override fun validate() {
        customerKey.validate(CUSTOMER_KEY)
        checkType.validate(CHECK_TYPE)
    }

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (AddCardResponse) -> Unit, onFailure: (Exception) -> Unit) {
        super.performRequest(this, AddCardResponse::class.java, onSuccess, onFailure)
    }
}
