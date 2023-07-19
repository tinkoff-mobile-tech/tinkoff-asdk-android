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

package ru.tinkoff.acquiring.sdk.ui.customview.editcard.validators

import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem
import java.util.*

/**
 * @author Mariya Chernyadieva
 */
internal object CardValidator {

    const val MAX_DATE_LENGTH = 5
    const val MAX_CVC_LENGTH = 3

    private const val CVC_CODE_REGEXP = "^[0-9]{3}$"

    fun validateCardNumber(cardNumber: String): Boolean {
        if (!RegexpValidator.validate(cardNumber, RegexpValidator.NUMBER_REGEXP)) {
            return false
        }

        val cardType = CardPaymentSystem.resolve(cardNumber)
        val allowedLengths = cardType.range
        var lengthAllowed = false

        for (allowedLength in allowedLengths) {
            if (cardNumber.length == allowedLength) {
                lengthAllowed = true
            }
        }

        return lengthAllowed && validateWithLuhnAlgorithm(cardNumber)
    }

    fun validateExpireDate(expiryDate: String, validateNotExpired: Boolean): Boolean {
        if (expiryDate.isEmpty() || expiryDate.isBlank() || expiryDate.length != MAX_DATE_LENGTH) {
            return false
        }

        val month: Int
        val year: Int

        try {
            month = Integer.parseInt(expiryDate.substring(0, 2))
            year = Integer.parseInt(expiryDate.substring(3, 5))
        } catch (e: NumberFormatException) {
            return false
        }

        if (month in 1..12) {
            if (!validateNotExpired) return true

            val c = Calendar.getInstance()
            val currentYearStr = c.get(Calendar.YEAR).toString().substring(2)
            val currentMonth = c.get(Calendar.MONTH) + 1
            val currentYear = Integer.parseInt(currentYearStr)
            if (year == currentYear && month >= currentMonth) {
                return true
            }
            if (year > currentYear && year <= currentYear + 20) {
                return true
            }
        }

        return false
    }

    fun validateSecurityCode(cvc: String): Boolean {
        return if (cvc.isEmpty()) {
            false
        } else RegexpValidator.validate(
                cvc,
                CVC_CODE_REGEXP
        )
    }

    fun validateSecurityCodeOrFalse(cvc: String?): Boolean {
        cvc ?: return false
        return validateSecurityCode(cvc)
    }

    //http://en.wikipedia.org/wiki/Luhn_algorithm
    private fun validateWithLuhnAlgorithm(cardNumber: String): Boolean {
        var sum = 0
        var value: Int
        for (i in cardNumber.length - 1 downTo 0) {
            value = Character.getNumericValue(cardNumber[i])
            if (value == -1 || value == -2) {
                return false
            }
            val shouldBeDoubled = (cardNumber.length - i) % 2 == 0
            if (shouldBeDoubled) {
                value *= 2
                sum += if (value > 9) 1 + value % 10 else value
            } else {
                sum += value
            }
        }
        return sum % 10 == 0
    }
}