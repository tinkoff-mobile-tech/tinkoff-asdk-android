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

package ru.tinkoff.acquiring.sdk.utils.serialization

import com.google.gson.*
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import java.lang.reflect.Type

/**
 * @author Mariya Chernyadieva
 */
internal class CardStatusSerializer : JsonSerializer<CardStatus>, JsonDeserializer<CardStatus> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement?, typeOfT: Type, context: JsonDeserializationContext): CardStatus? {
        if (json != null) {
            val stringRepresentation = json.asString
            if (stringRepresentation.length != 1) {
                throw JsonParseException("Card Status has wrong format: $stringRepresentation")
            }

            return CardStatus.fromChar(stringRepresentation[0])
        }

        return null
    }

    override fun serialize(src: CardStatus?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement? {
        return if (src != null) {
            JsonPrimitive(src.toString())
        } else null
    }
}