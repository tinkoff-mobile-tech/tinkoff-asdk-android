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
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus

/**
 * Ответ на запрос GetAddCardState
 *
 * @param requestKey идентификатор запроса на привязку карты
 * @param status     статус привязки карты
 * @param cardId     идентификатор карты в системе банка
 * @param rebillId   идентификатор рекуррентного платежа
 *
 * @author Mariya Chernyadieva
 */
class GetAddCardStateResponse(
        @SerializedName("RequestKey")
        val requestKey: String? = null,

        @SerializedName("Status")
        val status: ResponseStatus? = null,

        @SerializedName("CardId")
        val cardId: String? = null,

        @SerializedName("RebillId")
        val rebillId: String? = null

) : AcquiringResponse()