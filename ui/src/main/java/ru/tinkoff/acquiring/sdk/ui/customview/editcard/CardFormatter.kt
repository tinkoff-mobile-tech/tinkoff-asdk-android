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

package ru.tinkoff.acquiring.sdk.ui.customview.editcard

import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MAESTRO
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MASTER_CARD
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MIR
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.VISA
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.validators.CardValidator
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.validators.RegexpValidator

/**
 * @author Mariya Chernyadieva
 */
internal object CardFormatter {

    const val DEFAULT_FORMAT = 0 //xxxx xxxx xxxx xxxx
    const val MAESTRO_FORMAT = 1 //xxxxxxxx xxxx...x
    const val UNKNOWN_FORMAT = 2 //xxxxxxxxxx...

    const val DATE_DELIMITER = "/"

    fun resolveCardFormat(cardNumber: String): Int {
        return if (cardNumber.length >= 4) {
            val cardType = CardPaymentSystem.resolvePaymentSystem(cardNumber)
            when {
                (cardType == MIR && cardNumber.length <= CardPaymentSystem.getLengthRanges(MIR).first()) ||
                        cardType == VISA || cardType == MASTER_CARD -> DEFAULT_FORMAT
                (cardType == MIR && cardNumber.length > CardPaymentSystem.getLengthRanges(MIR).first()) ||
                        cardType == MAESTRO -> MAESTRO_FORMAT
                else -> UNKNOWN_FORMAT
            }
        } else {
            UNKNOWN_FORMAT
        }
    }

    fun formatDate(cardDate: String): String {
        var resultDate: String

        resultDate = getRawDate(cardDate)
        if (!RegexpValidator.validate(resultDate, RegexpValidator.NUMBER_REGEXP)) {
            return ""
        }

        if (resultDate.length >= CardValidator.MAX_DATE_LENGTH) {
            resultDate = resultDate.dropLast(resultDate.length - CardValidator.MAX_DATE_LENGTH + 1)
        }

        if (resultDate.length > 1) {
            resultDate = "${resultDate.subSequence(0, 2)}/${resultDate.subSequence(2, resultDate.length)}"
        }

        return resultDate
    }

    fun formatSecurityCode(cvc: String): String {
        var resultCvc = cvc

        if (!RegexpValidator.validate(cvc, RegexpValidator.NUMBER_REGEXP)) {
            return ""
        }
        if (cvc.length > CardValidator.MAX_CVC_LENGTH) {
            resultCvc = cvc.dropLast(cvc.length - CardValidator.MAX_CVC_LENGTH)
        }

        return resultCvc
    }

    fun getRawNumber(cardNumber: String): String {
        val resultNumber = cardNumber.replace(" ", "")
        if (!RegexpValidator.validate(resultNumber, RegexpValidator.MASKED_NUMBER_REGEXP)) {
            return ""
        }
        return resultNumber
    }

    fun getRawDate(cardDate: String): String {
        return cardDate.replace(DATE_DELIMITER, "")
    }
}