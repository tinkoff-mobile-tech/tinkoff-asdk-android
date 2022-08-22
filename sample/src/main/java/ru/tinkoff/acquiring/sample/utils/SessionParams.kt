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

package ru.tinkoff.acquiring.sample.utils

import com.google.android.gms.wallet.WalletConstants
import java.util.*

/**
 * @author Mariya Chernyadieva
 */
class SessionParams(
        val terminalKey: String,
        val password: String,
        val publicKey: String,
        val customerKey: String,
        val customerEmail: String) {
    companion object {

        var GPAY_TEST_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST

        private const val DEFAULT_CUSTOMER_KEY = "user-key"
        private const val DEFAULT_CUSTOMER_EMAIL = "user@example.com"

        private const val SDK_TERMINAL_ID = "TestSDK"
        private const val NON_3DS_TERMINAL_ID = "sdkNon3DS"

        private const val PASSWORD = "12345678"
        private const val PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5Yg3RyEkszggDVMDHCAG\n" +
                "zJm0mYpYT53BpasrsKdby8iaWJVACj8ueR0Wj3Tu2BY64HdIoZFvG0v7UqSFztE/\n" +
                "zUvnznbXVYguaUcnRdwao9gLUQO2I/097SHF9r++BYI0t6EtbbcWbfi755A1EWfu\n" +
                "9tdZYXTrwkqgU9ok2UIZCPZ4evVDEzDCKH6ArphVc4+iKFrzdwbFBmPmwi5Xd6CB\n" +
                "9Na2kRoPYBHePGzGgYmtKgKMNs+6rdv5v9VB3k7CS/lSIH4p74/OPRjyryo6Q7Nb\n" +
                "L+evz0+s60Qz5gbBRGfqCA57lUiB3hfXQZq5/q1YkABOHf9cR6Ov5nTRSOnjORgP\n" +
                "jwIDAQAB"

        val TEST_SDK = SessionParams(
                SDK_TERMINAL_ID, PASSWORD, PUBLIC_KEY, DEFAULT_CUSTOMER_KEY, DEFAULT_CUSTOMER_EMAIL
        )

        val NON_3DS = SessionParams(
                NON_3DS_TERMINAL_ID, PASSWORD, PUBLIC_KEY, DEFAULT_CUSTOMER_KEY, DEFAULT_CUSTOMER_EMAIL
        )

        val DEFAULT = TEST_SDK

        private val terminals = object : HashMap<String, SessionParams>() {

            private fun addTerminal(sessionParams: SessionParams) {
                put(sessionParams.terminalKey, sessionParams)
            }

            init {
                addTerminal(DEFAULT)
                addTerminal(TEST_SDK)
                addTerminal(NON_3DS)
            }
        }

        fun terminals(): Collection<SessionParams> {
            return terminals.values
        }

        operator fun get(terminalKey: String): SessionParams {
            return terminals[terminalKey]!!
        }
    }
}
