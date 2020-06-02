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
 * Тип проверки при привязке карты
 *
 * @author Mariya Chernyadieva
 */
enum class CheckType(private val checkType: String) {

    /**
     * Привязка без проверки
     */
    NO("NO"),

    /**
     * Привязка с блокировкой в 1 руб
     */
    HOLD("HOLD"),

    /**
     * Привязка с 3DS
     */
    THREE_DS("3DS"),

    /**
     * Привязка с 3DS и блокировкой маленькой суммы до 2 руб
     */
    THREE_DS_HOLD("3DSHOLD");

    override fun toString(): String {
        return checkType
    }
}