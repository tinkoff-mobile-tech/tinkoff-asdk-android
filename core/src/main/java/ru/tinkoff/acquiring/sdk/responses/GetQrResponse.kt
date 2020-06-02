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
 * Ответ на запрос GetStaticQr
 *
 * @param data в зависимости от параметра DataType в запросе это:
 *             Payload - информация, которая должна быть закодирована в QR или
 *             SVG изображение QR в котором уже закодирован Payload закодирована в QR
 *
 * @author Mariya Chernyadieva
 */
class GetQrResponse(
        @SerializedName("Data")
        val data: String? = null
) : AcquiringResponse()