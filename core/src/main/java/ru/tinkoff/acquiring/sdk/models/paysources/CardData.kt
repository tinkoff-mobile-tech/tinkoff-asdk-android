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
 * Данные платёжной карты
 *
 * @author Mariya Chernyadieva
 */
class CardData() : CardSource {

    /**
     * Номер карты
     */
    var pan: String = ""

    /**
     * Срок действия карты
     */
    var expiryDate: String = ""

    /**
     * Секретный код проверки подлинности
     */
    var securityCode: String = ""

    constructor(pan: String, expiryDate: String, securityCode: String) : this() {
        this.pan = pan
        this.expiryDate = expiryDate
        this.securityCode = securityCode
    }

    override fun encode(publicKey: PublicKey): String {
        validate()
        val date = expiryDate.replace("\\D".toRegex(), "")
        val mergedData = String.format("%s=%s;%s=%s;%s=%s",
                KEY_PAN, pan,
                KEY_DATE, date,
                KEY_CVC, securityCode)

        return CryptoUtils.encodeBase64(CryptoUtils.encryptRsa(mergedData, publicKey))
    }

    override fun validate() {
        var wrongField: String? = null

        if (!CardValidator.validateNumber(pan)) {
            wrongField = "number"
        } else if (!CardValidator.validateExpirationDate(expiryDate)) {
            wrongField = "expiration date"
        }

        if (!CardValidator.validateSecurityCode(securityCode) && wrongField == null) {
            wrongField = "security code"
        }

        check(wrongField == null) { "Cannot encode card data. Wrong $wrongField" }
    }

    companion object {

        private const val KEY_PAN = "PAN"
        private const val KEY_DATE = "ExpDate"
        private const val KEY_CVC = "CVC"
    }
}
