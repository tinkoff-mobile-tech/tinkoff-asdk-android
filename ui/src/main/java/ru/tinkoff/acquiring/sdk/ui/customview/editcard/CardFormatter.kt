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

import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.APRA
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.ARCA
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.BELKART
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.ELCART
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MAESTRO
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MASTER_CARD
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MIR
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.UNKNOWN
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.UNION_PAY
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.VISA
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.validators.CardValidator
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.validators.RegexpValidator

/**
 * @author Mariya Chernyadieva
 */
internal object CardFormatter {

    const val DATE_DELIMITER = "/"

    @Deprecated("Redesign")
    fun resolveCardFormat(cardNumber: String): CardFormat =
        CardFormat(resolveCardNumberMask(cardNumber))

    fun resolveCardNumberMask(cardNumber: String): String {
        val length = cardNumber.length

        return when (CardPaymentSystem.resolve(cardNumber)) {
            VISA -> "#### #### #### ####"
            MASTER_CARD -> "#### #### #### ####"
            MIR -> when (length) {
                in 0..16 -> "#### #### #### ####"
                else -> "###### #############"
            }
            MAESTRO -> when (length) {
                in 0..13 -> "#### #### #####"
                in 0..15 -> "#### ###### #####"
                in 0..16 -> "#### #### #### ####"
                in 0..19 -> "###### #############"
                else -> "###################"
            }
            UNION_PAY -> when (length) {
                in 0..16 -> "#### #### #### ####"
                else -> "###### #############"
            }
            BELKART, ELCART, APRA, ARCA -> "#### #### #### ####"
            else -> "#".repeat(UNKNOWN.range.last)
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

    @Deprecated("Redesign")
    class CardFormat(val mask: String) {

        val blocks = mask.split(' ')

        fun forEachBlockUntil(index: Int, action: (blockStart: Int, blockEnd: Int) -> Unit) {
            var position = 0
            for (i in 0..blocks.size) {
                if (position > index) return
                action(position, (position + blocks[i].length).coerceAtMost(index + 1))
                position += blocks[i].length
            }
        }

        fun getBlockNumber(index: Int): Int {
            var position = 0
            for (i in blocks.indices) {
                position += blocks[i].length
                if (position > index) return i
            }
            return blocks.size - 1
        }
    }
}