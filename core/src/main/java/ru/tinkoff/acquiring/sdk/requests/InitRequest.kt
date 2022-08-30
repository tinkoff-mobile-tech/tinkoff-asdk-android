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

import ru.tinkoff.acquiring.sdk.models.Receipt
import ru.tinkoff.acquiring.sdk.models.Shop
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.INIT_METHOD
import ru.tinkoff.acquiring.sdk.responses.InitResponse
import java.text.SimpleDateFormat
import java.util.*

/**
 * Инициирует новый платеж
 *
 * @author Mariya Chernyadieva, Taras Nagorny
 */
class InitRequest : AcquiringRequest<InitResponse>(INIT_METHOD) {

    /**
     * Сумма в копейках
     */
    var amount: Long = 0

    /**
     * Номер заказа в системе продавца
     */
    var orderId: String? = null

    /**
     * Название шаблона формы оплаты продавца
     */
    var payForm: String? = null

    /**
     * Идентификатор покупателя в системе продавца
     */
    var customerKey: String? = null

    /**
     * Краткое описание заказа, макс. длина 250 символов
     */
    var description: String? = null
        set(value) {
            field = value?.take(250)
        }

    /**
     * Язык платёжной формы.
     * ru - форма оплаты на русском языке;
     * en - форма оплаты на английском языке.
     * По-умолчанию - форма оплаты на русском языке
     */
    var language: String? = null

    /**
     * Форма проведения платежа [ru.tinkoff.acquiring.sdk.models.enums.PayType]
     */
    var payType: String? = null

    /**
     * Объект с данными чека
     */
    var receipt: Receipt? = null

    /**
     * Указывает, что совершается рекуррентный или нерекуррентный платеж
     */
    var recurrent: Boolean = false

    /**
     * Флаг, что происходит оплата в рекуретном режиме, и вместо вызова FinishAuthorize
     * необходимо вызвать Charge
     */
    var chargeFlag: Boolean = false

    /**
     * Объект, содержащий дополнительные параметры в виде "ключ":"значение".
     * Данные параметры будут переданы в запросе платежа/привязки карты.
     * Максимальная длина для каждого передаваемого параметра:
     * Ключ – 20 знаков,
     * Значение – 100 знаков.
     * Максимальное количество пар "ключ-значение" не может превышать 20
     */
    var data: Map<String, String>? = null

    /**
     * Список с данными магазинов
     */
    var shops: List<Shop>? = null

    /**
     * Список с данными чеков
     */
    var receipts: List<Receipt>? = null

    /**
     * Срок жизни ссылки
     */
    var redirectDueDate: Date? = null
        set(value) {
            field = value
            redirectDueDateFormat = dateFormat.format(value).let {
                StringBuilder(it).insert(it.length - 2, ":").toString()
            }
        }

    /**
     * Адрес для получения http нотификаций
     */
    var notificationURL: String? = null

    /**
     * Страница успеха
     */
    var successURL: String? = null

    /**
     * Страница ошибки
     */
    var failURL: String? = null

    var sdkVersion: String? = null

    var softwareVersion: String? = null

    var deviceModel: String? = null

    private var redirectDueDateFormat: String? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()

        map.putIfNotNull(AMOUNT, amount.toString())
        map.putIfNotNull(ORDER_ID, orderId)
        map.putIfNotNull(CUSTOMER_KEY, customerKey)
        map.putIfNotNull(DESCRIPTION, description)
        map.putIfNotNull(PAY_FORM, payForm)
        map.putIfNotNull(RECURRENT, if (recurrent) RECURRENT_FLAG_Y else null)
        map.putIfNotNull(LANGUAGE, language)
        map.putIfNotNull(PAY_TYPE, payType)
        map.putIfNotNull(RECEIPT, receipt)
        map.putIfNotNull(RECEIPTS, receipts)
        map.putIfNotNull(SHOPS, shops)
        map.putIfNotNull(REDIRECT_DUE_DATE, redirectDueDateFormat)
        map.putIfNotNull(NOTIFICATION_URL, notificationURL)
        map.putIfNotNull(SUCCESS_URL, successURL)
        map.putIfNotNull(FAIL_URL, failURL)
        map.putDataIfNonNull(data)

        return map
    }

    override fun validate() {
        orderId.validate(ORDER_ID)
        amount.validate(AMOUNT)
    }

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (InitResponse) -> Unit, onFailure: (Exception) -> Unit) {
        super.performRequest(this, InitResponse::class.java, onSuccess, onFailure)
    }

    fun receipt(receipt: Receipt.() -> Unit) {
        this.receipt = Receipt().apply(receipt)
    }

    private fun MutableMap<String, Any>.putDataIfNonNull(data: Map<String, String>?) {
        val dataMap = HashMap<String, String>()

        if (data != null) {
            dataMap.putAll(data)
        }

        dataMap[CHARGE_FLAG] = chargeFlag.toString()
        dataMap[CONNECTION_TYPE] = CONNECTION_TYPE_MOBILE_SDK
        sdkVersion?.let { dataMap[SDK_VERSION] = it }
        softwareVersion?.let { dataMap[SOFTWARE_VERSION] = it }
        deviceModel?.let { dataMap[DEVICE_MODEL] = it }
        this[DATA] = dataMap
    }

    companion object {
        private const val RECURRENT_FLAG_Y = "Y"
        private const val CONNECTION_TYPE_MOBILE_SDK = "mobile_sdk"
    }
}
