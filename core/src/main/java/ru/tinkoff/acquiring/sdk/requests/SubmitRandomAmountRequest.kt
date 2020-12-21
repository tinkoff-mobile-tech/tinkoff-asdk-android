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

import ru.tinkoff.acquiring.sdk.network.AcquiringApi.SUBMIT_RANDOM_AMOUNT_METHOD
import ru.tinkoff.acquiring.sdk.responses.SubmitRandomAmountResponse

/**
 * Метод подтверждения при типе привязке карты [ru.tinkoff.acquiring.sdk.models.enums.CheckType.THREE_DS_HOLD]
 *
 * @author Mariya Chernyadieva
 */
class SubmitRandomAmountRequest : AcquiringRequest<SubmitRandomAmountResponse>(SUBMIT_RANDOM_AMOUNT_METHOD) {

    /**
     * Заблокированная сумма в копейках
     */
    var amount: Long = 0

    /**
     * Ключ, полученный при запросе [AddCardRequest]
     */
    var requestKey: String? = null

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()

        map.putIfNotNull(REQUEST_KEY, requestKey)
        map.putIfNotNull(AMOUNT, amount)

        return map
    }

    override fun validate() {
        requestKey.validate(REQUEST_KEY)
        amount.validate(AMOUNT)
    }

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (SubmitRandomAmountResponse) -> Unit, onFailure: (Exception) -> Unit) {
        super.performRequest(this, SubmitRandomAmountResponse::class.java, onSuccess, onFailure)
    }
}