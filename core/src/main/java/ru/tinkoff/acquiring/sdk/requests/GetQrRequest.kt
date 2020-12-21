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

import ru.tinkoff.acquiring.sdk.models.enums.DataTypeQr
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.GET_QR_METHOD
import ru.tinkoff.acquiring.sdk.responses.GetQrResponse

/**
 * Регистрирует QR и возвращает информацию о нем
 *
 * @author Mariya Chernyadieva
 */
class GetQrRequest : AcquiringRequest<GetQrResponse>(GET_QR_METHOD) {

    /**
     * Уникальный идентификатор транзакции в системе банка
     */
    var paymentId: Long? = null

    /**
     * Тип возвращаемых данных
     */
    var dataType: DataTypeQr? = null

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()

        map.putIfNotNull(PAYMENT_ID, paymentId.toString())
        map.putIfNotNull(DATA_TYPE, dataType.toString())

        return map
    }

    override fun validate() {
        paymentId.validate(PAYMENT_ID)
        dataType.validate(DATA_TYPE)
    }

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (GetQrResponse) -> Unit, onFailure: (Exception) -> Unit) {
        super.performRequest(this, GetQrResponse::class.java, onSuccess, onFailure)
    }
}