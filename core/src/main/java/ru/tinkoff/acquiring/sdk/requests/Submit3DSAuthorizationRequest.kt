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

import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.responses.Submit3DSAuthorizationResponse
import ru.tinkoff.acquiring.sdk.utils.Base64

/**
 * Подтверждает прохождение app-based 3DS версии 2.1
 */
class Submit3DSAuthorizationRequest : AcquiringRequest<Submit3DSAuthorizationResponse>(
    AcquiringApi.SUBMIT_3DS_AUTHORIZATION_V2) {

    override val contentType: String = AcquiringApi.FORM_URL_ENCODED

    /**
     * Уникальный идентификатор транзакции, генерируемый 3DS-Server
     */
    var threeDSServerTransID: String? = null

    /**
     * Статус транзакции
     */
    var transStatus: String? = null

    override fun asMap(): MutableMap<String, Any> {
        val cres = gson.toJson(buildMap {
            this[THREE_DS_SERVER_TRANS_ID] = threeDSServerTransID!!
            this[TRANS_STATUS] = transStatus!!
        })
        val encodedCres = Base64.encodeToString(cres.toByteArray(), Base64.NO_WRAP)
        return mutableMapOf(CRES to encodedCres)
    }

    override fun validate() {
        threeDSServerTransID.validate(THREE_DS_SERVER_TRANS_ID)
        transStatus.validate(TRANS_STATUS)
    }

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (Submit3DSAuthorizationResponse) -> Unit, onFailure: (Exception) -> Unit) {
        super.performRequest(this, Submit3DSAuthorizationResponse::class.java, onSuccess, onFailure)
    }
}
