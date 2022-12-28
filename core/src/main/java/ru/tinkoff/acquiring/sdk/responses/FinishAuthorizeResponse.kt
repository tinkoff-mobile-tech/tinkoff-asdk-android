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
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.models.ThreeDsData
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus

/**
 * Ответ на запрос FinishAuthorize
 *
 * @param paymentId        уникальный идентификатор транзакции в системе банка
 * @param orderId          номер заказа в системе продавца
 * @param amount           сумма списания в копейках
 * @param acsUrl           адрес сервера управления доступом, для проверки 3DS
 * @param md               уникальный номер заказа в системе платежного шлюза, для проверки 3DS (3DS 1.x)
 * @param paReq            параметр из ответа на запрос оплаты, для проверки 3DS (3DS 1.x)
 * @param tdsServerTransId идентификатор транзакции 3DS (3DS 2.x)
 * @param acsTransId       идентификатор транзакции 3DS, присвоенный ACS (3DS 2.x)
 * @param acsRefNumber     идентификатор ACS (3DS 2.1, app-based)
 * @param acsSignedContent JWT-токен, сфоримарованный ACS для проеведения транзацкии; содержит ACS URL, ACS ephemeral
 *                         public key и SDK ephemeral public key (3DS 2.1, app-based)
 * @param status           статус транзакции
 *
 * @author Mariya Chernyadieva
 */
class FinishAuthorizeResponse(
        @SerializedName("PaymentId")
        val paymentId: Long? = null,

        @SerializedName("OrderId")
        val orderId: String? = null,

        @SerializedName("Amount")
        val amount: Long? = null,

        @SerializedName("RebillId")
        val rebillId: String? = null,

        @SerializedName("ACSUrl")
        val acsUrl: String? = null,

        @SerializedName("MD")
        val md: String? = null,

        @SerializedName("PaReq")
        val paReq: String? = null,

        @SerializedName("TdsServerTransId")
        val tdsServerTransId: String? = null,

        @SerializedName("AcsTransId")
        val acsTransId: String? = null,

        @SerializedName("AcsReferenceNumber")
        val acsRefNumber: String? = null,

        @SerializedName("AcsSignedContent")
        val acsSignedContent: String? = null,

        @SerializedName("Status")
        val status: ResponseStatus? = null

) : AcquiringResponse() {

    @Transient
    private lateinit var threeDsData: ThreeDsData

    fun getThreeDsData(threeDsVersion: String?): ThreeDsData {
        threeDsData = when (status) {
            ResponseStatus.CONFIRMED, ResponseStatus.AUTHORIZED -> ThreeDsData.EMPTY_THREE_DS_DATA
            ResponseStatus.THREE_DS_CHECKING -> {
                if (md != null && paReq != null) {
                    ThreeDsData(paymentId, acsUrl).apply {
                        md = this@FinishAuthorizeResponse.md
                        paReq = this@FinishAuthorizeResponse.paReq
                        version = threeDsVersion ?: "1.0.0"
                    }
                } else if (tdsServerTransId != null && acsTransId != null) {
                    ThreeDsData(paymentId, acsUrl).apply {
                        tdsServerTransId = this@FinishAuthorizeResponse.tdsServerTransId
                        acsTransId = this@FinishAuthorizeResponse.acsTransId
                        acsRefNumber = this@FinishAuthorizeResponse.acsRefNumber
                        acsSignedContent = this@FinishAuthorizeResponse.acsSignedContent
                        version = threeDsVersion ?: "2.1.0"
                    }
                } else throw AcquiringSdkException(IllegalStateException("Invalid 3DS params"))
            }
            else -> throw AcquiringSdkException(IllegalStateException("Incorrect ResponseStatus $status"))
        }

        return threeDsData
    }
}
