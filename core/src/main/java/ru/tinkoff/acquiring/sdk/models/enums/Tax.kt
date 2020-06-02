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

package ru.tinkoff.acquiring.sdk.models.enums

/**
 * Ставка налога
 *
 * @author Mariya Chernyadieva
 */
enum class Tax(private var tax: String) {

    /**
     * Без НДС
     */
    NONE("none"),

    /**
     * НДС по ставке 0%
     */
    VAT_0("vat0"),

    /**
     * НДС чека по ставке 10%
     */
    VAT_10("vat10"),

    /**
     * НДС чека по ставке 18%
     */
    VAT_18("vat18"),

    /**
     * НДС чека по расчетной ставке 10/110
     */
    VAT_110("vat110"),

    /**
     * НДС чека по расчетной ставке 18/118
     */
    VAT_118("vat118"),

    /**
     * НДС чека по ставке 20%
     */
    VAT_20("vat20"),

    /**
     * НДС чека по расчетной ставке 20/120
     */
    VAT_120("vat120");

    override fun toString(): String {
        return tax
    }
}
