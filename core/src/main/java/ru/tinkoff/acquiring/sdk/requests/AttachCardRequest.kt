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

import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.ATTACH_CARD_METHOD
import ru.tinkoff.acquiring.sdk.responses.AttachCardResponse
import java.util.*

/**
 * Завершает привязку карты к покупателю
 *
 * @author Mariya Chernyadieva
 */
class AttachCardRequest : AcquiringRequest<AttachCardResponse>(ATTACH_CARD_METHOD) {

    /**
     * Зашифрованные данные карты
     */
    var cardData: CardData? = null

    /**
     * Идентификатор запроса на привязку карты
     */
    var requestKey: String? = null

    /**
     * Email адрес покупателя
     */
    var email: String? = null

    /**
     * Объект содержащий дополнительные параметры в виде "ключ":"значение".
     * Данные параметры будут переданы в запросе платежа/привязки карты.
     * Максимальная длина для каждого передаваемого параметра:
     * Ключ – 20 знаков,
     * Значение – 100 знаков.
     * Максимальное количество пар "ключ-значение" не может превышать 20
     */
    var data: Map<String, String>? = null

    private var encodedCardData: String? = null

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()

        map.putIfNotNull(CARD_DATA, encodedCardData)
        map.putIfNotNull(REQUEST_KEY, requestKey)
        map.putDataIfNonNull(data)

        return map
    }

    override fun validate() {
        encodedCardData.validate(CARD_DATA)
        requestKey.validate(REQUEST_KEY)
    }

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (AttachCardResponse) -> Unit, onFailure: (Exception) -> Unit) {
        encodedCardData = cardData?.encode(publicKey)
        super.performRequest(this, AttachCardResponse::class.java, onSuccess, onFailure)
    }

    fun cardData(cardData: CardData.() -> Unit) {
        this.cardData = CardData().apply(cardData)
    }

    private fun MutableMap<String, Any>.putDataIfNonNull(data: Map<String, String>?) {
        val dataMap = HashMap<String, String?>()

        if (data != null) {
            dataMap.putAll(data)
        }

        dataMap[DATA_KEY_EMAIL] = email
        this[DATA] = dataMap
    }
}