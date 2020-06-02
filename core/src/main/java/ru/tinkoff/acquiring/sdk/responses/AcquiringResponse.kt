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
import java.io.Serializable

/**
 * Базовый класс ответа Acquiring API
 *
 * @param errorCode код ошибки
 * @param isSuccess статус успешного выполнения запроса
 *
 * @author Mariya Chernyadieva
 */
abstract class AcquiringResponse(
        @SerializedName("ErrorCode")
        val errorCode: String? = null,

        @SerializedName("Success")
        val isSuccess: Boolean? = null

) : Serializable {

    /**
     * Краткое описание ошибки
     */
    @SerializedName("Message")
    val message: String? = null

    /**
     * Подробное описание ошибки
     */
    @SerializedName("Details")
    val details: String? = null

    /**
     * Идентификатор терминала
     */
    @SerializedName("TerminalKey")
    val terminalKey: String? = null
}

