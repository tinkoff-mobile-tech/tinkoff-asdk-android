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

package ru.tinkoff.acquiring.sdk.models.paysources

import ru.tinkoff.acquiring.sdk.utils.CardValidator
import ru.tinkoff.acquiring.sdk.utils.CryptoUtils
import java.security.PublicKey

/**
 * Привязанная карта
 *
 * @author Mariya Chernyadieva
 */
class AttachedCard() : CardSource {

    /**
     * Идентификатор карты в системе Банка
     */
    var cardId: String? = null

    /**
     * Секретный код проверки подлинности
     */
    var cvc: String? = null

    /**
     * Идентификатор рекуррентного платежа
     */
    var rebillId: String? = null

    constructor(cardId: String?, cvc: String?) : this() {
        this.cardId = cardId
        this.cvc = cvc
    }

    constructor(rebillId: String?) : this() {
        this.rebillId = rebillId
    }

    override fun encode(publicKey: PublicKey): String {
        validate()
        return if (rebillId.isNullOrEmpty()) {
            val mergedData: String = String.format("%s=%s;%s=%s", KEY_CARD_ID, cardId, KEY_CVC, cvc)
            CryptoUtils.encodeBase64(CryptoUtils.encryptRsa(mergedData, publicKey))
        } else {
            CryptoUtils.encodeBase64(CryptoUtils.encryptRsa(rebillId!!, publicKey))
        }
    }

    override fun validate() {
        if (rebillId.isNullOrEmpty()) {
            check(!cardId.isNullOrEmpty()) { "CardId should not be empty " }
            check(!cvc.isNullOrEmpty() && CardValidator.validateSecurityCode(cvc!!)) {
                "Field security code should not be empty "
            }
        } else {
            check(rebillId!!.isNotEmpty()) { "RebillId should not be empty " }
        }
    }

    companion object {

        private const val KEY_CARD_ID = "CardId"
        private const val KEY_CVC = "CVC"
    }
}