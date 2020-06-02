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
import ru.tinkoff.acquiring.sdk.models.enums.AgentSign
import java.io.Serializable

/**
 * Данные агента
 *
 * @author Mariya Chernyadieva
 */
class AgentData : Serializable {

    /**
     * Признак агента
     */
    @SerializedName("AgentSign")
    var agentSign: AgentSign? = null

    /**
     * Наименование операции
     */
    @SerializedName("OperationName")
    var operationName: String? = null

    /**
     * Телефоны платежного агента
     */
    @SerializedName("Phones")
    var phones: Array<String>? = null

    /**
     * Телефоны оператора по приему платежей
     */
    @SerializedName("ReceiverPhones")
    var receiverPhones: Array<String>? = null

    /**
     * Телефоны оператора перевода
     */
    @SerializedName("TransferPhones")
    var transferPhones: Array<String>? = null

    /**
     * Наименование оператора перевода
     */
    @SerializedName("OperatorName")
    var operatorName: String? = null

    /**
     * Адрес оператора перевода
     */
    @SerializedName("OperatorAddress")
    var operatorAddress: String? = null

    /**
     * ИНН оператора перевода
     */
    @SerializedName("OperatorInn")
    var operatorInn: String? = null
}
