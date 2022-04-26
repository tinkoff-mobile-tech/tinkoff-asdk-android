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
 * Ответ на запрос TinkoffPayLink
 *
 * @param params Json-объект содержащий дополнительный параметр
 */
class TinkoffPayLinkResponse(
        @SerializedName("Params")
        val params: Params? = null

) : AcquiringResponse() {

    /**
     * @param redirectUrl URL для перехода в приложение Мобильный Банк
     */
    class Params(
            @SerializedName("RedirectUrl")
            val redirectUrl: String
    )
}