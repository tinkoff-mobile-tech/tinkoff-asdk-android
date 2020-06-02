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

package ru.tinkoff.acquiring.sdk

import ru.tinkoff.acquiring.sdk.loggers.JavaLogger
import ru.tinkoff.acquiring.sdk.loggers.Logger
import ru.tinkoff.acquiring.sdk.requests.*
import ru.tinkoff.acquiring.sdk.utils.keycreators.KeyCreator
import ru.tinkoff.acquiring.sdk.utils.keycreators.StringKeyCreator
import java.security.PublicKey

/**
 * Класс позволяет конфигурировать SDK и осуществлять взаимодействие с Tinkoff Acquiring API.
 * Методы осуществляют обращение к API
 *
 * @param terminalKey ключ терминала. Выдается после подключения к Tinkoff Acquiring
 * @param password    пароль от терминала. Выдается вместе с terminalKey
 * @param publicKey   экземпляр PublicKey созданный из публичного ключа, выдаваемого вместе с
 *                    terminalKey
 *
 * @author Mariya Chernyadieva
 */
class AcquiringSdk(
        private val terminalKey: String,
        private val password: String,
        private val publicKey: PublicKey
) {

    constructor(terminalKey: String, password: String, publicKey: String) :
            this(terminalKey, password, StringKeyCreator(publicKey))

    constructor(terminalKey: String, password: String, keyCreator: KeyCreator) :
            this(terminalKey, password, keyCreator.create())


    /**
     * Инициирует платежную сессию
     */
    fun init(request: InitRequest.() -> Unit): InitRequest {
        return InitRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            password = this@AcquiringSdk.password
        }
    }

    /**
     * Проверяет поддерживаемую версию 3DS протокола по карточным данным из входящих параметров
     */
    fun check3DsVersion(request: Check3dsVersionRequest.() -> Unit): Check3dsVersionRequest {
        return Check3dsVersionRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            password = this@AcquiringSdk.password
            publicKey = this@AcquiringSdk.publicKey
        }
    }

    /**
     * Подтверждает инициированный платеж передачей карточных данных
     */
    fun finishAuthorize(request: FinishAuthorizeRequest.() -> Unit): FinishAuthorizeRequest {
        return FinishAuthorizeRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            password = this@AcquiringSdk.password
            publicKey = this@AcquiringSdk.publicKey
        }
    }

    /**
     * Возвращает список привязанных карт
     */
    fun getCardList(request: GetCardListRequest.() -> Unit): GetCardListRequest {
        return GetCardListRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            password = this@AcquiringSdk.password
        }
    }

    /**
     * Осуществляет рекуррентный (повторный) платеж — безакцептное списание денежных средств со
     * счета банковской карты покупателя. Для возможности его использования покупатель должен
     * совершить хотя бы один платеж в пользу продавца, который должен быть указан как рекуррентный
     * (см. параметр [InitRequest.recurrent]), фактически являющийся первичным.
     *
     * Другими словами, для использования рекуррентных платежей необходима следующая
     * последовательность действий:
     * 1. Совершить родительский платеж путем вызова Init с указанием дополнительного параметра
     * recurrent=true
     * 2. Получить RebillId, предварительно вызвав метод GetCardList
     * 3. Для совершения рекуррентного платежа необходимо вызвать метод Init со стандартным
     * набором параметров (параметр Recurrent здесь не нужен).
     * 4. Получить в ответ на Init параметр PaymentId.
     * 5. Вызвать метод Charge c параметром RebillId полученным в п.2 и параметром PaymentId
     * полученным в п.4
     */
    fun charge(request: ChargeRequest.() -> Unit): ChargeRequest {
        return ChargeRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            password = this@AcquiringSdk.password
        }
    }

    /**
     * Регистрирует QR и возвращает информацию о нем. Должен быть вызван после вызова метода Init
     */
    fun getQr(request: GetQrRequest.() -> Unit): GetQrRequest {
        return GetQrRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            password = this@AcquiringSdk.password
        }
    }

    /**
     * При первом вызове регистрирует QR и возвращает информацию о нем при последующих вызовах вовзращает
     * информацию о ранее сгенерированном QR. Перерегистрация статического QR происходит только при смене
     * расчетного счета. Не привязан к конкретному платежу, может быть вызван в любое время
     * без предварительного вызова Init
     */
    fun getStaticQr(request: GetStaticQrRequest.() -> Unit): GetStaticQrRequest {
        return GetStaticQrRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            password = this@AcquiringSdk.password
        }
    }

    /**
     * Возвращает статус платежа
     */
    fun getState(request: GetStateRequest.() -> Unit): GetStateRequest {
        return GetStateRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            password = this@AcquiringSdk.password
        }
    }

    /**
     * Удаляет привязанную карту
     */
    fun removeCard(request: RemoveCardRequest.() -> Unit): RemoveCardRequest {
        return RemoveCardRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            password = this@AcquiringSdk.password
        }
    }

    /**
     * Метод подготовки для привязки карты, необходимо вызвать [AcquiringSdk.addCard]
     * перед методом [AcquiringSdk.attachCard]
     */
    fun addCard(request: AddCardRequest.() -> Unit): AddCardRequest {
        return AddCardRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            password = this@AcquiringSdk.password
        }
    }

    /**
     * Метод привязки карты, вызывается после [AcquiringSdk.addCard]
     */
    fun attachCard(request: AttachCardRequest.() -> Unit): AttachCardRequest {
        return AttachCardRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            password = this@AcquiringSdk.password
            publicKey = this@AcquiringSdk.publicKey
        }
    }

    /**
     * Метод проверки состояния привязки карты после 3D-Secure
     */
    fun getAddCardState(request: GetAddCardStateRequest.() -> Unit): GetAddCardStateRequest {
        return GetAddCardStateRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            password = this@AcquiringSdk.password
        }
    }

    /**
     * Метод подтверждения при [ru.tinkoff.acquiring.sdk.models.enums.CheckType.THREE_DS_HOLD]
     * привязки
     */
    fun submitRandomAmount(request: SubmitRandomAmountRequest.() -> Unit): SubmitRandomAmountRequest {
        return SubmitRandomAmountRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            password = this@AcquiringSdk.password
        }
    }

    companion object AsdkLogger {

        /**
         * Позволяет использовать свой логгер или заданный
         */
        var logger: Logger = JavaLogger()

        /**
         * Позволяет включить логирование. По-умолчанию выключен
         */
        var isDebug = false

        /**
         * Позволяет переключать SDK с тестового режима и обратно. В тестовом режиме деньги с карты не
         * списываются. По-умолчанию выключен
         */
        var isDeveloperMode = false


        /**
         * Логирует сообщение
         */
        fun log(message: CharSequence) {
            if (isDebug) {
                logger.log(message)
            }
        }

        /**
         * Логирует ошибку/исключение
         */
        fun log(e: Throwable) {
            if (isDebug) {
                logger.log(e)
            }
        }
    }
}