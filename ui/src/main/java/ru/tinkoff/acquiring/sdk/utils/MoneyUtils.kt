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

import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.regex.Pattern

/**
 * @author Mariya Chernyadieva
 */
internal object MoneyUtils {

    private val RUS_LOCALE = Locale("ru", "RU")

    private const val DEFAULT_MONEY_DECIMAL_SEPARATOR = ','
    private const val DEFAULT_MONEY_GROUPING_SEPARATOR = '\u00a0'

    private val MONEY_FORMAT: DecimalFormat
    private val MONEY_FORMAT_PRECISE: DecimalFormat

    private const val POINT_SEPARATOR = '.'
    private const val MONEY_FRACTIONAL_PART = ".00"
    private const val DEFAULT_NORMALIZED = "0.00"

    init {
        val decimalFormatSymbols = DecimalFormatSymbols(RUS_LOCALE)
        decimalFormatSymbols.decimalSeparator = DEFAULT_MONEY_DECIMAL_SEPARATOR
        decimalFormatSymbols.groupingSeparator = DEFAULT_MONEY_GROUPING_SEPARATOR

        MONEY_FORMAT = DecimalFormat("#,##0.##", decimalFormatSymbols)
        MONEY_FORMAT_PRECISE = DecimalFormat("#,##0.####", decimalFormatSymbols)
    }

    fun replaceArtifacts(string: String): String {
        return string.replace(DEFAULT_MONEY_DECIMAL_SEPARATOR.toString(), ".").replace(DEFAULT_MONEY_GROUPING_SEPARATOR.toString(), "")
    }

    fun format(s: String): String {
        val integral: String
        var fraction = ""
        val commaIndex = s.indexOf(DEFAULT_MONEY_DECIMAL_SEPARATOR)
        if (commaIndex != -1) {
            integral = s.substring(0, commaIndex)
            fraction = s.substring(commaIndex, s.length)
        } else {
            integral = s
        }

        return if (integral.isEmpty()) {
            fraction
        } else {
            val formatString = formatMoney(BigDecimal(replaceArtifacts(integral)))
            "$formatString$fraction"
        }
    }

    fun normalize(rawMoney: String): String {
        var normalized: String
        if (TextUtils.isEmpty(rawMoney)) {
            normalized = DEFAULT_NORMALIZED
        } else {
            normalized = replaceArtifacts(rawMoney)
            if (!normalized.contains(POINT_SEPARATOR.toString())) {
                normalized += MONEY_FRACTIONAL_PART
            } else {
                if (normalized[0] == POINT_SEPARATOR) {
                    normalized = "0$normalized"
                }
            }
        }
        return normalized
    }

    private fun formatMoney(amount: BigDecimal): String {
        return MONEY_FORMAT.format(amount)
    }

    class MoneyWatcher : TextWatcher {

        private var pattern: Pattern? = null
        private var beforeEditing = ""
        private var selfEdit: Boolean = false

        init {
            setLengthLimit(DEFAULT_LIMIT)
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if (selfEdit) {
                return
            }
            beforeEditing = s.toString()
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}


        override fun afterTextChanged(editable: Editable) {
            if (selfEdit) {
                return
            }

            var resultString = replaceArtifacts(editable.toString())
            resultString = resultString.replace('.', DEFAULT_MONEY_DECIMAL_SEPARATOR)
            if (!TextUtils.isEmpty(resultString)) {
                val isValidCharacters = pattern!!.matcher(resultString).matches()
                resultString = if (!isValidCharacters) {
                    beforeEditing
                } else {
                    format(resultString)
                }
            }
            selfEdit = true
            editable.replace(0, editable.length, resultString, 0, resultString.length)
            selfEdit = false
        }

        /**
         * Sets length limit and updates regex pattern for validation.
         * 9 is suggested for RUB, 7 is suggested for other currencies.
         *
         * @param lengthLimit the length limit.
         */
        fun setLengthLimit(lengthLimit: Int) {
            val patternValue = String.format(Locale.getDefault(), FORMAT_PATTERN, DEFAULT_MONEY_GROUPING_SEPARATOR, lengthLimit, DEFAULT_MONEY_DECIMAL_SEPARATOR)
            pattern = Pattern.compile(patternValue)
        }

        companion object {

            private const val DEFAULT_LIMIT = 7
            private const val FORMAT_PATTERN = "^((\\d%s?){1,%d})?(%s\\d{0,2})?$"
        }
    }
}
