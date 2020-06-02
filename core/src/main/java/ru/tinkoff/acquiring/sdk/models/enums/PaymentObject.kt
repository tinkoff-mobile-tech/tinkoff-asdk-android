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

package ru.tinkoff.acquiring.sdk.models.enums

import com.google.gson.annotations.SerializedName

/**
 * Признак предмета расчета
 *
 * @author Mariya Chernyadieva
 */
enum class PaymentObject {

    /**
     * Подакцизный товар
     */
    @SerializedName("excise")
    EXCISE,

    /**
     * Работа
     */
    @SerializedName("job")
    JOB,

    /**
     * Услуга
     */
    @SerializedName("service")
    SERVICE,

    /**
     * Ставка азартной игры
     */
    @SerializedName("gambling_bet")
    GAMBLING_BET,

    /**
     * Выигрыш азартной игры
     */
    @SerializedName("gambling_prize")
    GAMBLING_PRIZE,

    /**
     * Лотерейный билет
     */
    @SerializedName("lottery")
    LOTTERY,

    /**
     * Выигрыш лотереи
     */
    @SerializedName("lottery_prize")
    LOTTERY_PRIZE,

    /**
     * Предоставление результатов интеллектуальной деятельности
     */
    @SerializedName("intellectual_activity")
    INTELLECTUAL_ACTIVITY,

    /**
     * Платеж
     */
    @SerializedName("payment")
    PAYMENT,

    /**
     * Агентское вознаграждение
     */
    @SerializedName("agent_commission")
    AGENT_COMMISSION,

    /**
     * Составной предмет расчета
     */
    @SerializedName("composite")
    COMPOSITE,

    /**
     * Иной предмет расчета
     */
    @SerializedName("another")
    ANOTHER
}
