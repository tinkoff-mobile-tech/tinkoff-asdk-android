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
import ru.tinkoff.acquiring.sdk.models.ThreeDsData
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus

/**
 * Ответ на запрос AttachCard
 *
 * @param requestKey  идентификатор запроса на привязку карты
 * @param customerKey идентификатор покупателя в системе продавца
 * @param cardId      идентификатор карты в системе банка
 * @param rebillId    идентификатор рекуррентного платежа
 * @param status      статус привязки карты
 * @param acsUrl      адрес сервера управления доступом, для проверки 3DS
 * @param md          уникальный номер заказа в системе платежного шлюза, для проверки 3DS
 * @param paReq       параметр из ответа на запрос оплаты, для проверки 3DS
 *
 * @author Mariya Chernyadieva
 */
class AttachCardResponse(
        @SerializedName("RequestKey")
        val requestKey: String? = null,

        @SerializedName("CustomerKey")
        val customerKey: String? = null,

        @SerializedName("CardId")
        val cardId: String? = null,

        @SerializedName("RebillId")
        val rebillId: String? = null,

        @SerializedName("Status")
        val status: ResponseStatus? = null,

        @SerializedName("ACSUrl")
        val acsUrl: String? = null,

        @SerializedName("MD")
        val md: String? = null,

        @SerializedName("PaReq")
        val paReq: String? = null,

        @SerializedName("AcsTransId")
        val acsTransId: String? = null

) : AcquiringResponse() {

    @Transient
    private lateinit var threeDsData: ThreeDsData

    fun getThreeDsData(): ThreeDsData {
        threeDsData = when (status) {
            ResponseStatus.THREE_DS_CHECKING -> ThreeDsData(requestKey, acsUrl).apply {
                md = this@AttachCardResponse.md
                paReq = this@AttachCardResponse.paReq
            }
            else -> ThreeDsData.EMPTY_THREE_DS_DATA
        }

        return threeDsData
    }
}
