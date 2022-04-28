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

package ru.tinkoff.acquiring.sdk.responses

import com.google.gson.annotations.SerializedName

/**
 * Ответ на запрос TinkoffPayStatus
 *
 * @param params Json-объект содержащий дополнительные параметры
 */
class TinkoffPayStatusResponse(
        @SerializedName("Params")
        val params: Params? = null

) : AcquiringResponse() {

    fun isTinkoffPayAvailable(): Boolean = params?.allowed == true

    /**
     * @return "1.0" - deeplink, "2.0" - applink
     */
    fun getTinkoffPayVersion(): String? = params?.version

    /**
     * @param allowed Наличие возможности проведения оплаты TinkoffPay по API, SDK
     * @param version Версия TinkoffPay, доступная на терминале:
     * - 1.0 (e-invoice);
     * - 2.0 (TinkoffPay)
     */
    class Params(
            @SerializedName("Allowed")
            val allowed: Boolean,

            @SerializedName("Version")
            val version: String? = null
    )
}