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

package ru.tinkoff.acquiring.sdk.ui.customview.editcard

private val REGEX_VISA = "^(4[0-9*]*)\$".toRegex()
private val REGEX_MASTER_CARD = "^(5(?!05827|61468)[0-9*]*)\$".toRegex()
private val REGEX_MIR = "^((220[0-4]|356|505827|561468|623446|629129|629157|629244|676347|676454|676531|671182|676884|676907|677319|677384|8600|9051|9112(?!00|50|39|99)|9417(?!00|99)|9762|9777|9990(?!01))[0-9*]*)\$".toRegex()
private val REGEX_MAESTRO = "^(6(?!2|76347|76454|76531|71182|76884|76907|77319|77384)[0-9*]*)\$".toRegex()
private val REGEX_UNION_PAY = "^((81[0-6]|817[01]|62(?!3446|9129|9157|9244))[0-9*]*)\$".toRegex()
private val REGEX_BELKART = "^(9112(00|50|39|99)[0-9*]*)\$".toRegex()
private val REGEX_ELCART = "^(9417(00|99)[0-9*]*)\$".toRegex()
private val REGEX_APRA = "^(999001[0-9*]*)\$".toRegex()
private val REGEX_ARCA = "^(902100[0-9*]*)\$".toRegex()
private val REGEX_ANY = "[0-9*]*".toRegex()

enum class CardPaymentSystem(val regex: Regex, val range: IntRange, val showLogo: Boolean) {

    VISA(REGEX_VISA, 16..16, true),
    MASTER_CARD(REGEX_MASTER_CARD, 16..16, true),
    MIR(REGEX_MIR, 16..19, true),
    MAESTRO(REGEX_MAESTRO, 13..19, true),
    UNION_PAY(REGEX_UNION_PAY, 13..19, true),
    BELKART(REGEX_BELKART, 16..16, false),
    ELCART(REGEX_ELCART, 16..16, false),
    APRA(REGEX_APRA, 16..16, false),
    ARCA(REGEX_ARCA, 16..16, false),
    UNKNOWN(REGEX_ANY, 13..28, false);

    fun matches(cardNumber: String) = regex.matches(cardNumber) && cardNumber.length <= range.last

    companion object {

        fun resolve(cardNumber: String): CardPaymentSystem {
            if (cardNumber.length == 1 && cardNumber.startsWith("6")) {
                // special case: wait for more digits for MAESTRO/UNION_PAY disambiguation
                return UNKNOWN
            }
            return values().find { it.matches(cardNumber) } ?: UNKNOWN
        }
    }
}