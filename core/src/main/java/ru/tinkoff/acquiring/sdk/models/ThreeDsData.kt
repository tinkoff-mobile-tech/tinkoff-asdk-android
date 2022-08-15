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

import java.io.Serializable

/**
 * Данные для инициализации 3D-Secure
 *
 * @param paymentId     уникальный идентификатор транзакции в системе банка
 * @param requestKey    идентификатор запроса на привязку карты
 * @param acsUrl        адрес сервера управления доступом, для проверки 3DS
 * @param isThreeDsNeed флаг, указывающий должна ли производиться проверка 3DS
 *
 * @author Mariya Chernyadieva
 */
class ThreeDsData(
        var paymentId: Long? = null,
        var requestKey: String? = null,
        var acsUrl: String? = null,
        var isThreeDsNeed: Boolean
) : Serializable {

    /**
     *  Уникальный номер заказа в системе платежного шлюза, для проверки 3DS (3DS 1.x)
     */
    var md: String? = null

    /**
     * Параметр из ответа на запрос оплаты, для проверки 3DS (3DS 1.x)
     */
    var paReq: String? = null

    /**
     * Идентификатор транзакции из ответа метода (3DS 2.x)
     */
    var tdsServerTransId: String? = null

    /**
     * Идентификатор транзакции, присвоенный ACS (3DS 2.x)
     */
    var acsTransId: String? = null

    /**
     * Идентификатор ACS (3DS 2.1, app-based)
     */
    var acsRefNumber: String? = null

    /**
     * JWT-токен, сфоримарованный ACS для проеведения транзацкии; содержит ACS URL, ACS ephemeral
     * public key и SDK ephemeral public key (3DS 2.1, app-based)
     */
    var acsSignedContent: String? = null

    /**
     * Версия протокола 3DS
     */
    var version: String? = null

    val isPayment: Boolean
        get() = paymentId != null && requestKey == null

    val isAttaching: Boolean
        get() = paymentId == null && requestKey != null

    val is3DsVersion2: Boolean
        get() = tdsServerTransId != null && acsTransId != null

    constructor(paymentId: Long?, acsUrl: String?) : this(isThreeDsNeed = true) {
        this.isThreeDsNeed = true
        this.paymentId = paymentId
        this.requestKey = null
        this.acsUrl = acsUrl
    }

    constructor(requestKey: String?, acsUrl: String?) : this(isThreeDsNeed = true) {
        this.isThreeDsNeed = true
        this.paymentId = null
        this.requestKey = requestKey
        this.acsUrl = acsUrl
    }

    override fun toString(): String {
        return "Data: paymentId = $paymentId, " +
                "acsUrl = $acsUrl, " +
                "md = $md, " +
                "paReq = $paReq, " +
                "tdsServerTransId = $tdsServerTransId, " +
                "acsTransId = $acsTransId, " +
                "acsRefNumber = $acsRefNumber, " +
                "isThreeDsNeed = $isThreeDsNeed, " +
                "version = $version;"
    }

    companion object {

        val EMPTY_THREE_DS_DATA = ThreeDsData(isThreeDsNeed = false)
    }
}
