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
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.GET_STATIC_QR_METHOD
import ru.tinkoff.acquiring.sdk.responses.GetStaticQrResponse

/**
 * При первом вызове регистрирует QR и возвращает информацию о нем при последующих вызовах вовзращает
 * информацию о ранее сгенерированном QR. Перерегистрация статического QR происходит только при смене
 * расчетного счета
 *
 * @author Mariya Chernyadieva
 */
class GetStaticQrRequest : AcquiringRequest<GetStaticQrResponse>(GET_STATIC_QR_METHOD) {

    /**
     * Тип возвращаемых данных
     */
    var data: DataTypeQr? = null

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()
        map.putIfNotNull(DATA_TYPE, data.toString())

        return map
    }

    override fun validate() {
        data.validate(DATA)
    }

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (GetStaticQrResponse) -> Unit, onFailure: (Exception) -> Unit) {
        super.performRequest(this, GetStaticQrResponse::class.java, onSuccess, onFailure)
    }
}