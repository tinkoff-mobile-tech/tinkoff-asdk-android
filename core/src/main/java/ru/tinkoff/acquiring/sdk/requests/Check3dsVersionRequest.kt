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

import ru.tinkoff.acquiring.sdk.models.paysources.CardSource
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.CHECK_3DS_VERSION_METHOD
import ru.tinkoff.acquiring.sdk.responses.Check3dsVersionResponse

/**
 * Проверяет поддерживаемую версию 3DS протокола по карточным данным из входящих параметров
 *
 * @author Mariya Chernyadieva
 */
class Check3dsVersionRequest : AcquiringRequest<Check3dsVersionResponse>(CHECK_3DS_VERSION_METHOD) {

    /**
     * Уникальный идентификатор транзакции в системе банка, полученный в ответе на вызов метода Init
     */
    var paymentId: Long? = null

    /**
     * Источник платежа - карточные данные
     */
    var paymentSource: CardSource? = null

    private var encodedCardData: String? = null

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()

        map.putIfNotNull(PAYMENT_ID, paymentId.toString())
        map.putIfNotNull(CARD_DATA, encodedCardData)

        return map
    }

    override fun validate() {
        paymentSource.validate(PAYMENT_SOURCE)
        paymentId.validate(PAYMENT_ID)
        encodedCardData.validate(CARD_DATA)
    }

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (Check3dsVersionResponse) -> Unit, onFailure: (Exception) -> Unit) {
        fillPaymentData()
        super.performRequest(this, Check3dsVersionResponse::class.java, onSuccess, onFailure)
    }

    private fun fillPaymentData() {
        encodedCardData = paymentSource!!.encode(publicKey)
    }
}