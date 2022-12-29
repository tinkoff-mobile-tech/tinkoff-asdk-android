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

import java.io.Serializable
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Осуществляет преобразование денежных единиц
 *
 * @author Mariya Chernyadieva
 */
class Money private constructor(val coins: Long) : Serializable, Comparable<Money> {

    private var integralDivider = DEFAULT_INT_DIVIDER
    private var integralFractionDivider = DEFAULT_INT_FRACT_DIVIDER

    constructor() : this(0L)

    override fun toString(): String {
        val fractional = coins % COINS_IN_RUBLE

        return if (fractional == 0L) {
            formatIntPart(coins)
        } else String.format("%s%s%s",
                formatIntPart(coins),
                integralFractionDivider,
                formatFractionalPart(fractional))

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val money = other as Money?

        return coins == money!!.coins
    }

    override fun hashCode(): Int {
        return (coins xor coins.ushr(32)).toInt()
    }

    override fun compareTo(other: Money): Int {
        return java.lang.Long.valueOf(coins).compareTo(other.coins)
    }

    fun toHumanReadableString(): String {
        return "${toString()} ₽"
    }

    private fun formatFractionalPart(fractional: Long): String {
        return String.format("%02d", fractional)
    }

    private fun formatIntPart(decimal: Long): String {
        if (decimal < 100) {
            return "0"
        }

        var unformatted = decimal.toString()
        unformatted = unformatted.substring(0, unformatted.length - PRECISION)

        val headLength = unformatted.length % 3
        val result: StringBuilder
        result = if (headLength > 0) {
            StringBuilder(unformatted.substring(0, headLength))
        } else {
            StringBuilder()
        }

        for (i in headLength until unformatted.length) {
            val headlessOffset = i - headLength
            if (headlessOffset % 3 == 0 && i != unformatted.length - 1) {
                result.append(integralDivider)
            }
            result.append(unformatted[i])
        }

        return result.toString()
    }

    companion object {

        const val COINS_IN_RUBLE: Byte = 100
        private const val PRECISION: Byte = 2
        const val DEFAULT_INT_DIVIDER = " "
        const val DEFAULT_INT_FRACT_DIVIDER = ","

        /**
         * Перевод копейки в рубли
         */
        @JvmStatic
        fun ofRubles(rubles: Long): Money {
            return Money(toCoins(rubles))
        }

        @JvmStatic
        fun ofRubles(value: BigDecimal): Money {
            val precised = value.setScale(PRECISION.toInt(), RoundingMode.HALF_EVEN)
            val coins = precised.multiply(BigDecimal(COINS_IN_RUBLE.toInt(), MathContext(0)))
            return Money(coins.toLong())
        }

        @JvmStatic
        fun ofRubles(rubles: Double): Money {
            return ofRubles(BigDecimal(rubles))
        }

        /**
         * Перевод рубли в копейки
         */
        @JvmStatic
        fun ofCoins(coins: Long): Money {
            return Money(coins)
        }

        private fun toCoins(rubles: Long): Long {
            return rubles * COINS_IN_RUBLE
        }
    }
}
