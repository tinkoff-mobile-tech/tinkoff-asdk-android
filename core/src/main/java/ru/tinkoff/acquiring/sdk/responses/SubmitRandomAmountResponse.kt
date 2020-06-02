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
 * Ответ на запрос SubmitRandomAmount
 *
 * @param requestKey  идентификатор запроса на привязку карты
 * @param customerKey идентификатор покупателя в системе продавца
 * @param cardId      идентификатор карты в системе банка
 * @param rebillId    идентификатор рекуррентного платежа
 *
 * @author Mariya Chernyadieva
 */
class SubmitRandomAmountResponse(
        @SerializedName("RequestKey")
        val requestKey: String? = null,

        @SerializedName("CustomerKey")
        val customerKey: String? = null,

        @SerializedName("CardId")
        val cardId: String? = null,

        @SerializedName("RebillId")
        val rebillId: String? = null

) : AcquiringResponse()