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

package ru.tinkoff.acquiring.sdk.payment

import ru.tinkoff.acquiring.sdk.models.AsdkState

/**
 * События, возникающие в процессе оплаты [PaymentProcess]
 *
 * @author Mariya Chernyadieva
 */
interface PaymentListener {

    /**
     * Оплата прошла успешно
     *
     * @param paymentId уникальный идентификатор транзакции в системе банка
     * @param cardId    идентификатор карты в системе банка. Значение, отличное от null,
     *                  возвращается в случае, если платеж совершался с использованием привязанной карты
     * @param rebillId  идентификатор рекуррентного платежа. Значение, отличное от null,
     *                  возвращается в случае, если совершался рекуррентный платеж
     */
    fun onSuccess(paymentId: Long, cardId: String? = null, rebillId: String? = null)

    /**
     * В процессе оплаты возникла необходимость показать экран Acquiring SDK.
     * Вызывается в случае проверки 3DS или отклонения карты при рекуррентном платеже
     *
     * @param state состояние, которое нужно указать при открытии экрана оплаты
     * [ru.tinkoff.acquiring.sdk.TinkoffAcquiring.openPaymentScreen]
     */
    fun onUiNeeded(state: AsdkState)

    /**
     * В процессе оплаты произошла ошибка
     */
    fun onError(throwable: Throwable, paymentId: Long?)

    /**
     * Событие изменения состояния процесса оплаты
     */
    fun onStatusChanged(state: PaymentState?)
}

/**
 * Вспомогательный класс, позволяющий реализовать события выборочно
 */
abstract class PaymentListenerAdapter : PaymentListener {

    override fun onSuccess(paymentId: Long, cardId: String?, rebillId: String?) = Unit

    override fun onUiNeeded(state: AsdkState) = Unit

    override fun onError(throwable: Throwable, paymentId: Long?) = Unit

    override fun onStatusChanged(state: PaymentState?) = Unit
}