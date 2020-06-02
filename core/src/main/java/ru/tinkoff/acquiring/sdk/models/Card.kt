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

import com.google.gson.annotations.SerializedName
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import java.io.Serializable

/**
 * Данные карты, в ответе на запрос [ru.tinkoff.acquiring.sdk.requests.GetCardListRequest]
 *
 * @param pan      номер карты
 * @param cardId   идентификатор карты в системе банка
 * @param status   статус карты
 * @param rebillId идентификатор рекуррентного платежа
 *
 * @author Mariya Chernyadieva
 */
data class Card(
        @SerializedName("Pan")
        var pan: String? = null,

        @SerializedName("CardId")
        var cardId: String? = null,

        @SerializedName("ExpDate")
        var expDate: String? = null,

        @SerializedName("Status")
        var status: CardStatus? = null,

        @SerializedName("RebillId")
        var rebillId: String? = null

) : Serializable