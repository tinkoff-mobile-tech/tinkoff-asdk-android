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

package ru.tinkoff.acquiring.sdk.utils

import java.util.*
import java.util.regex.Pattern

/**
 * @author Mariya Chernyadieva
 */
internal object CardValidator {

    private val allowedLengths = 13..28
    private const val ZERO_NUMBERS_CARD_NUMBER_REGEXP = "[0]{1,}"
    private const val CVC_REGEXP = "^[0-9]{3}$"

    fun validateNumber(cardNumber: String?): Boolean {
        if (cardNumber == null) {
            return false
        }

        if (RegexpValidator.validate(cardNumber, ZERO_NUMBERS_CARD_NUMBER_REGEXP)) {
            return false
        }

        var lengthAllowed = false

        for (allowedLength in allowedLengths) {
            if (cardNumber.length == allowedLength) {
                lengthAllowed = true
            }
        }

        return lengthAllowed && validateWithLuhnAlgorithm(cardNumber)
    }

    fun validateSecurityCode(cvc: String): Boolean {
        return if (cvc.isEmpty()) {
            false
        } else RegexpValidator.validate(cvc, CVC_REGEXP)

    }

    fun validateExpirationDate(expiryDate: String): Boolean {
        if (expiryDate.isEmpty() || expiryDate.length != 5) {
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
            return true
        }

        // disable expiration validation
        /*if (month in 1..12) {
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
        }*/

        return false
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

    private object RegexpValidator {

        fun validate(string: CharSequence, regexp: String): Boolean {
            val pattern = Pattern.compile(regexp)
            val matcher = pattern.matcher(string)
            return matcher.matches()
        }
    }
}