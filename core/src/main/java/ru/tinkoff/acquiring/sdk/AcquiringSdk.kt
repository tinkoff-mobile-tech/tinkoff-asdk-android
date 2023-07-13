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
import ru.tinkoff.acquiring.sdk.responses.TinkoffPayStatusResponse
import ru.tinkoff.acquiring.sdk.utils.EnvironmentMode
import ru.tinkoff.acquiring.sdk.utils.SampleAcquiringTokenGenerator
import ru.tinkoff.acquiring.sdk.utils.keycreators.KeyCreator
import ru.tinkoff.acquiring.sdk.utils.keycreators.StringKeyCreator
import java.security.MessageDigest
import java.security.PublicKey

/**
 * Класс позволяет конфигурировать SDK и осуществлять взаимодействие с Tinkoff Acquiring API.
 * Методы осуществляют обращение к API.
 * Вызов методов выполняется синхронно
 *
 * Для корректного выполнения запросов также необходимо указать [tokenGenerator].
 *
 * @param terminalKey ключ терминала. Выдается после подключения к Tinkoff Acquiring
 * @param publicKey   экземпляр PublicKey созданный из публичного ключа, выдаваемого вместе с
 *                    terminalKey
 *
 * @author Mariya Chernyadieva, Taras Nagorny
 */
class AcquiringSdk(
        private val terminalKey: String,
        private val publicKey: PublicKey
) {
    var tinkoffPayStatusCache: TinkoffPayStatusCache? = null

    constructor(terminalKey: String, publicKey: String) :
            this(terminalKey, StringKeyCreator(publicKey))

    constructor(terminalKey: String, keyCreator: KeyCreator) :
            this(terminalKey, keyCreator.create())


    /**
     * Инициирует платежную сессию
     */
    fun init(request: InitRequest.() -> Unit): InitRequest {
        return InitRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
        }
    }

    /**
     * Проверяет поддерживаемую версию 3DS протокола по карточным данным из входящих параметров
     */
    fun check3DsVersion(request: Check3dsVersionRequest.() -> Unit): Check3dsVersionRequest {
        return Check3dsVersionRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            publicKey = this@AcquiringSdk.publicKey
        }
    }

    /**
     * Подтверждает инициированный платеж передачей карточных данных
     */
    fun finishAuthorize(request: FinishAuthorizeRequest.() -> Unit): FinishAuthorizeRequest {
        return FinishAuthorizeRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            publicKey = this@AcquiringSdk.publicKey
        }
    }

    /**
     * Возвращает список привязанных карт
     */
    fun getCardList(request: GetCardListRequest.() -> Unit): GetCardListRequest {
        return GetCardListRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
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
        }
    }

    /**
     * Метод подтверждает платеж и списывает ранее заблокированные средства.
     * Используется при двухстадийной оплате. При одностадийной оплате вызывается автоматически.
     * Применим к платежу только в статусе AUTHORIZED и только один раз.
     *
     * Сумма подтверждения не может быть больше заблокированной.
     * Если сумма подтверждения меньше заблокированной, будет выполнено частичное подтверждение
     */
    fun confirm(request: ConfirmRequest.() -> Unit): ConfirmRequest {
        return ConfirmRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
        }
    }

    /**
     * Метод отменяет платеж
     */
    fun cancel(request: CancelRequest.() -> Unit): CancelRequest {
        return CancelRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
        }
    }

    /**
     * Регистрирует QR и возвращает информацию о нем. Должен быть вызван после вызова метода Init
     */
    fun getQr(request: GetQrRequest.() -> Unit): GetQrRequest {
        return GetQrRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
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
        }
    }

    /**
     * Возвращает статус платежа
     */
    fun getState(request: GetStateRequest.() -> Unit): GetStateRequest {
        return GetStateRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
        }
    }

    /**
     * Удаляет привязанную карту
     */
    fun removeCard(request: RemoveCardRequest.() -> Unit): RemoveCardRequest {
        return RemoveCardRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
        }
    }

    /**
     * Метод подготовки для привязки карты, необходимо вызвать [AcquiringSdk.addCard]
     * перед методом [AcquiringSdk.attachCard]
     */
    fun addCard(request: AddCardRequest.() -> Unit): AddCardRequest {
        return AddCardRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
        }
    }

    /**
     * Метод привязки карты, вызывается после [AcquiringSdk.addCard]
     */
    fun attachCard(request: AttachCardRequest.() -> Unit): AttachCardRequest {
        return AttachCardRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            publicKey = this@AcquiringSdk.publicKey
        }
    }

    /**
     * Метод проверки состояния привязки карты после 3D-Secure
     */
    fun getAddCardState(request: GetAddCardStateRequest.() -> Unit): GetAddCardStateRequest {
        return GetAddCardStateRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
        }
    }

    /**
     * Метод подтверждения при [ru.tinkoff.acquiring.sdk.models.enums.CheckType.THREE_DS_HOLD]
     * привязки
     */
    fun submitRandomAmount(request: SubmitRandomAmountRequest.() -> Unit): SubmitRandomAmountRequest {
        return SubmitRandomAmountRequest().apply(request).apply {
            terminalKey = this@AcquiringSdk.terminalKey
        }
    }

    fun tinkoffPayStatus(request: (TinkoffPayStatusRequest.() -> Unit)? = null): TinkoffPayStatusRequest {
        return TinkoffPayStatusRequest(this@AcquiringSdk.terminalKey).apply {
            request?.invoke(this)
        }
    }

    fun tinkoffPayLink(paymentId: Long, version: String, request: (TinkoffPayLinkRequest.() -> Unit)? = null): TinkoffPayLinkRequest {
        return TinkoffPayLinkRequest(paymentId.toString(), version).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            request?.invoke(this)
        }
    }

    /**
     * Метод получения Deeplink-a для оплаты с помощью MirPay
     */
    fun mirPayLink(paymentId: Long, request: (MirPayLinkRequest.() -> Unit)? = null): MirPayLinkRequest {
        return MirPayLinkRequest(paymentId.toString()).apply {
            terminalKey = this@AcquiringSdk.terminalKey
            request?.invoke(this)
        }
    }

    fun getTerminalPayMethods() : GetTerminalPayMethodsRequest {
        return GetTerminalPayMethodsRequest(terminalKey)
    }

    fun submit3DSAuthorization(threeDSServerTransID: String, transStatus: String, request: (Submit3DSAuthorizationRequest.() -> Unit)? = null): Submit3DSAuthorizationRequest {
        return Submit3DSAuthorizationRequest().apply {
            terminalKey = this@AcquiringSdk.terminalKey
            this.threeDSServerTransID = threeDSServerTransID
            this.transStatus = transStatus
            request?.invoke(this)
        }
    }

    fun submit3DSAuthorizationFromWebView(paymentId: String?): Submit3DSAuthorizationWebViewRequest {
        return Submit3DSAuthorizationWebViewRequest().apply {
            terminalKey = this@AcquiringSdk.terminalKey
            this.paymentId = paymentId
        }
    }

    class TinkoffPayStatusCache(
        val status: TinkoffPayStatusResponse,
        val time: Long) {

        fun isExpired() = System.currentTimeMillis() - time > CACHE_EXPIRE_TIME_MS

        companion object {

            const val CACHE_EXPIRE_TIME_MS = 300_000L
        }
    }

    companion object {

        /**
         * Позволяет установить мод для окружения по умолчанию (дебаг)
         */
        var environmentMode: EnvironmentMode = EnvironmentMode.IsDebugMode

        /**
         * Объект, который будет использоваться для генерации токена при формировании запросов к api
         * ([документация по формированию токена](https://www.tinkoff.ru/kassa/develop/api/request-sign/)).
         *
         * Передача токена для SDK терминалов в общем случае не обязательна и зависит от настроек терминала.
         */
        var tokenGenerator: AcquiringTokenGenerator? = null

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
         * Позволяет переключать SDK с тестового режима(на другой контур) и обратно. В тестовом режиме деньги с карты не
         * списываются. По-умолчанию выключен
         */
        var isPreprodMode = false

        /**
         * Позволяет переключать SDK на иной апи-контур, работает только в дебаг режиме
         */
        var customUrl : String? = null

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

/**
 * Объект, который будет использоваться для генерации токена при формировании
 * запросов к api ([документация по формированию токена](https://www.tinkoff.ru/kassa/develop/api/request-sign/)).
 * На вход принимает словарь параметров (объекты **Shops**, **Receipt** и **DATA** уже исключены из
 * этого словаря), на выходе должен вернуть строку, являющуюся токеном.
 *
 * Алгоритм формирования токена:
 * 1. Добавить в исходный словарь пароль терминала с ключом **Password**.
 * 2. Отсортировать словарь по ключам в алфавитном порядке.
 * 3. Конкатенировать значения всех пар.
 * 4. Для полученной строки вычислить хэш SHA-256.
 *
 * Полученный хэш и будет являться токеном. При возвращении *null* токен не будет добавляться к запросу.
 *
 * Пример реализации алгоритма генерации токена можно увидеть в [SampleAcquiringTokenGenerator].
 *
 * **Note:** Метод вызывается в фоновом потоке.
 */
fun interface AcquiringTokenGenerator {

    /**
     * @param request запрос, для которого будет гененрироваться токен
     * @param params  словарь параметров, используемый для формирования токена; объекты **Shops**,
     * **Receipt** и **DATA** уже исключены из этого словаря
     *
     * @return токен, сформированный с использоваванием [params], который будет добавлен в параметры
     * запроса к API; при возвращении *null* токен не будет добавляться к запросу
     */
    fun generateToken(request: AcquiringRequest<*>, params: MutableMap<String, Any>): String?

    companion object {

        fun sha256hashString(source: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(source.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
