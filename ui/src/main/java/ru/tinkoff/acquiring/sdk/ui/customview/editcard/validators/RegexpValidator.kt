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

import java.util.regex.Pattern

/**
 * @author Mariya Chernyadieva
 */
internal object RegexpValidator {

    const val NUMBER_REGEXP = "^[0-9]+$"
    const val MASKED_NUMBER_REGEXP = "^[0-9*]+$"

    fun validate(string: CharSequence, regexp: String): Boolean {
        val pattern = Pattern.compile(regexp)
        val matcher = pattern.matcher(string)
        return matcher.matches()
    }
}