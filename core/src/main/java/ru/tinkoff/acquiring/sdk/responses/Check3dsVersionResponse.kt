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
 * Ответ на запрос Check3dsVersion
 *
 * @param version          версия протокола 3DS
 * @param serverTransId    уникальный идентификатор транзакции, генерируемый 3DS-Server,
 *                         обязательный параметр для 3DS второй версии
 * @param threeDsMethodUrl дополнительный параметр для 3DS второй версии, который позволяет
 *                         пройти этап по сбору данных браузера ACS-ом
 * @param paymentSystem    платежная система, через которую будет проводится оплата, участвует
 *                         в прохождении 3DS по app-based flow
 *
 * @author Mariya Chernyadieva
 */
class Check3dsVersionResponse(
        @SerializedName("Version")
        val version: String? = null,

        @SerializedName("TdsServerTransID")
        val serverTransId: String? = null,

        @SerializedName("ThreeDSMethodURL")
        val threeDsMethodUrl: String? = null,

        @SerializedName("PaymentSystem")
        val paymentSystem: String? = null

) : AcquiringResponse() {

    fun is3DsVersionV2() = version?.startsWith("2") ?: false
}