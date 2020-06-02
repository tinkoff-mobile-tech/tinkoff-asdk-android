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
 * Система налогообложения
 *
 * @author Mariya Chernyadieva
 */
enum class Taxation(private val taxation: String) {

    /**
     * Общая СН
     */
    OSN("osn"),

    /**
     * Упрощенная СН (доходы)
     */
    USN_INCOME("usn_income"),

    /**
     * Упрощенная СН (доходы минус расходы)
     */
    USN_INCOME_OUTCOME("usn_income_outcome"),

    /**
     * Единый налог на вмененный доход
     */
    ENVD("envd"),

    /**
     * Единый сельскохозяйственный налог
     */
    ESN("esn"),

    /**
     * Патентная СН
     */
    PATENT("patent");

    override fun toString(): String {
        return taxation
    }
}
