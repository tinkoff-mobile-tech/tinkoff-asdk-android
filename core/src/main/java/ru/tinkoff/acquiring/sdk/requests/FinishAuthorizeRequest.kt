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

package ru.tinkoff.acquiring.sdk.requests

import kotlinx.coroutines.Deferred
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.models.PaymentSource
import ru.tinkoff.acquiring.sdk.models.paysources.*
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.FINISH_AUTHORIZE_METHOD
import ru.tinkoff.acquiring.sdk.responses.FinishAuthorizeResponse

/**
 * Подтверждает инициированный платеж передачей карточных данных
 *
 * @author Mariya Chernyadieva
 */
class FinishAuthorizeRequest : AcquiringRequest<FinishAuthorizeResponse>(FINISH_AUTHORIZE_METHOD) {

    /**
     * true – отправлять клиенту информацию на почту об оплате, false – не отправлять
     */
    var sendEmail: Boolean = false

    /**
     * Уникальный идентификатор транзакции в системе банка, полученный в ответе на вызов метода Init
     */
    var paymentId: Long? = null

    /**
     * Email для отправки информации об оплате
     */
    var email: String? = null

    /**
     * Источник платежа (карточные данные или googlePayToken)
     */
    var paymentSource: PaymentSource? = null

    /**
     * Объект, содержащий дополнительные параметры в виде "ключ":"значение".
     * Данные параметры будут переданы на страницу оплаты (в случае ее кастомизации).
     * Максимальная длина для каждого передаваемого параметра:
     * Ключ – 20 знаков,
     * Значение – 100 знаков.
     * Максимальное количество пар "ключ-значение" не может превышать 20
     */
    var data: Map<String, String>? = null

    /**
     * IP-адрес клиента.
     * Обязательный параметр для 3DS второй версии
     */
    var ip: String? = null

    private var cardId: String? = null
    private var cvv: String? = null
    private var source: String? = null
    private var encryptedToken: String? = null
    private var encodedCardData: String? = null

    override val tokenIgnoreFields: HashSet<String>
        get() {
            val result = HashSet<String>()
            result.add(CARD_ID)
            result.add(CVV)
            result.add(DATA)
            return result
        }

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()

        map.putIfNotNull(PAYMENT_ID, paymentId.toString())
        map.putIfNotNull(SEND_EMAIL, sendEmail)
        map.putIfNotNull(CARD_DATA, encodedCardData)
        map.putIfNotNull(CARD_ID, cardId)
        map.putIfNotNull(CVV, cvv)
        map.putIfNotNull(EMAIL, email)
        map.putIfNotNull(SOURCE, source)
        map.putIfNotNull(ENCRYPTED_PAYMENT_DATA, encryptedToken)
        map.putIfNotNull(IP, ip)
        if (data != null) map.putDataIfNonNull(data)

        return map
    }

    override fun validate() {
        paymentSource.validate(PAYMENT_SOURCE)
        paymentId.validate(PAYMENT_ID)

        when (paymentSource) {
            is CardData, is AttachedCard -> encodedCardData.validate(CARD_DATA)
            is GooglePay, is YandexPay -> encryptedToken.validate(ENCRYPTED_PAYMENT_DATA)
        }
    }

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (FinishAuthorizeResponse) -> Unit, onFailure: (Exception) -> Unit) {
        fillPaymentData()
        super.performRequest(this, FinishAuthorizeResponse::class.java, onSuccess, onFailure)
    }

    override fun performRequestAsync(responseClass: Class<FinishAuthorizeResponse>): Deferred<Result<FinishAuthorizeResponse>> {
        fillPaymentData()
        return super.performRequestAsync(responseClass)
    }

    fun attachedCard(attachedCard: AttachedCard.() -> Unit): PaymentSource {
        return AttachedCard().apply(attachedCard)
    }

    @Deprecated("Not supported yet")
    fun googlePay(googlePay: GooglePay.() -> Unit): PaymentSource {
        return GooglePay().apply(googlePay)
    }

    fun cardData(cardData: CardData.() -> Unit): PaymentSource {
        return CardData().apply(cardData)
    }

    fun is3DsVersionV2(): Boolean {
        return data != null && ip != null
    }

    private fun fillPaymentData() {
        lateinit var data: PaymentSource
        when (paymentSource) {
            is CardSource -> {
                data = paymentSource as CardSource
                encodedCardData = data.encode(publicKey)
            }
            is GooglePay -> {
                data = paymentSource as GooglePay
                this.encryptedToken = data.googlePayToken
                this.source = GOOGLE_PAY
            }
            is YandexPay ->{
                data = paymentSource as YandexPay
                this.encryptedToken = data.yandexPayToken
                this.source = YANDEX_PAY
            }
            else -> throw AcquiringSdkException(IllegalStateException("Unknown type in 'paymentSource'"))
        }
    }

    private fun MutableMap<String, Any>.putDataIfNonNull(data: Map<String, String>?) {
        if (!data.isNullOrEmpty()) {
            this[DATA] = data.toMutableMap()
        }
    }

    companion object {

        private const val GOOGLE_PAY = "GooglePay"
        private const val YANDEX_PAY = "YandexPay"
    }
}
