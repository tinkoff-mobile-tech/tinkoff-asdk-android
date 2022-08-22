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
import ru.tinkoff.acquiring.sdk.responses.TinkoffPayLinkResponse

/**
 * Возвращает deeplink для TinkoffPay в зависимости от версии
 */
class TinkoffPayLinkRequest(paymentId: String, version: String) :
        AcquiringRequest<TinkoffPayLinkResponse>(
                "TinkoffPay/transactions/${paymentId}/versions/${version}/link") {

    override val httpRequestMethod: String = AcquiringApi.API_REQUEST_METHOD_GET

    override fun asMap(): MutableMap<String, Any> = mutableMapOf()

    override fun getToken(): String? = null

    override fun validate() = Unit

    /**
     * Синхронный вызов метода API
     */
    override fun execute(onSuccess: (TinkoffPayLinkResponse) -> Unit, onFailure: (Exception) -> Unit) {
        super.performRequest(this, TinkoffPayLinkResponse::class.java, onSuccess, onFailure)
    }
}
