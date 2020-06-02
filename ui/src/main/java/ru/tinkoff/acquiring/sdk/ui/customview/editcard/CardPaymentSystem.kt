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

import java.util.regex.Pattern

/**
 * @author Mariya Chernyadieva
 */
internal object CardPaymentSystem {

    const val UNKNOWN = 0
    const val MASTER_CARD = 1
    const val MAESTRO = 2
    const val MIR = 3
    const val VISA = 4

    private val defaultRangers = intArrayOf(16)
    private val unknownRangers = intArrayOf(13, 14, 15, 16, 17, 18, 19)
    private val maestroRangers = intArrayOf(13, 14, 15, 16, 17, 18, 19)
    private val mirRanges = intArrayOf(16, 18, 19)
    private val mirPattern = Pattern.compile("^220[0-4]")

    fun resolvePaymentSystem(cardNumber: String): Int {
        return if (cardNumber.length >= 4) {
            val firstChar = cardNumber[0]
            when {
                mirPattern.matcher(cardNumber).find() -> MIR
                firstChar == '2' || firstChar == '5' -> MASTER_CARD
                firstChar == '4' -> VISA
                firstChar == '6' -> MAESTRO
                else -> UNKNOWN
            }
        } else {
            UNKNOWN
        }
    }

    fun getLengthRanges(paymentSystem: Int): IntArray {
        return when (paymentSystem) {
            MASTER_CARD, VISA -> defaultRangers
            MIR -> mirRanges
            MAESTRO -> maestroRangers
            else -> unknownRangers
        }
    }
}