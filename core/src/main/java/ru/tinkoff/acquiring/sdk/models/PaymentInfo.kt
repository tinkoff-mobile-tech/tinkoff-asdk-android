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

package ru.tinkoff.acquiring.sdk.models

/**
 * Содержит информацию о проведенном рекуррентном платеже
 *
 * @param orderId   номер заказа в системе продавца
 * @param paymentId уникальный идентификатор транзакции в системе банка
 * @param amount    сумма списания в копейках
 * @param cardId    идентификатор карты в системе банка
 * @param errorCode код ошибки
 *
 * @author Mariya Chernyadieva
 */
class PaymentInfo(
        val orderId: String?,
        val paymentId: Long?,
        val amount: Long?,
        val cardId: String?,
        val errorCode: String?
) {

    val isSuccess: Boolean
        get() = CHARGE_SUCCESS == errorCode

    val isRejected: Boolean
        get() = CHARGE_REJECTED_ERROR == errorCode

    companion object {

        const val CHARGE_SUCCESS = "0"
        const val CHARGE_REJECTED_ERROR = "104"
    }
}
