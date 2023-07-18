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
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * @author Mariya Chernyadieva
 */
data class SessionParams(
    @SerializedName("terminalKey")
    val terminalKey: String,
    @SerializedName("password")
    val password: String?,
    @SerializedName("publicKey")
    val publicKey: String,
    @SerializedName("customerKey")
    val customerKey: String,
    @SerializedName("customerEmail")
    val customerEmail: String,
    @SerializedName("description")
    val description: String? = null
) {

    companion object {

        var GPAY_TEST_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST

        private const val PASSWORD = "12345678"
        private const val PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5Yg3RyEkszggDVMDHCAG\n" +
                "zJm0mYpYT53BpasrsKdby8iaWJVACj8ueR0Wj3Tu2BY64HdIoZFvG0v7UqSFztE/\n" +
                "zUvnznbXVYguaUcnRdwao9gLUQO2I/097SHF9r++BYI0t6EtbbcWbfi755A1EWfu\n" +
                "9tdZYXTrwkqgU9ok2UIZCPZ4evVDEzDCKH6ArphVc4+iKFrzdwbFBmPmwi5Xd6CB\n" +
                "9Na2kRoPYBHePGzGgYmtKgKMNs+6rdv5v9VB3k7CS/lSIH4p74/OPRjyryo6Q7Nb\n" +
                "L+evz0+s60Qz5gbBRGfqCA57lUiB3hfXQZq5/q1YkABOHf9cR6Ov5nTRSOnjORgP\n" +
                "jwIDAQAB"

        private const val DEFAULT_CUSTOMER_KEY = "TestSDK_CustomerKey1123413431"
        private const val DEFAULT_CUSTOMER_EMAIL = "user@example.com"

        val TEST_SDK = SessionParams("TestSDK", PASSWORD, PUBLIC_KEY, DEFAULT_CUSTOMER_KEY, DEFAULT_CUSTOMER_EMAIL)

        fun getDefaultTerminals(): List<SessionParams> = listOf(
            TEST_SDK,
            SessionParams("1521204415922", PASSWORD, PUBLIC_KEY, DEFAULT_CUSTOMER_KEY, DEFAULT_CUSTOMER_EMAIL, "SBP 1"),
            SessionParams("1562595669054", PASSWORD, PUBLIC_KEY, DEFAULT_CUSTOMER_KEY, DEFAULT_CUSTOMER_EMAIL, "SBP 2"),
            SessionParams("1578942570730", PASSWORD, PUBLIC_KEY, DEFAULT_CUSTOMER_KEY, DEFAULT_CUSTOMER_EMAIL, "SBP 3"),
            SessionParams("1661351612593", "45tnvz0kkyyz82mw", PUBLIC_KEY, DEFAULT_CUSTOMER_KEY, DEFAULT_CUSTOMER_EMAIL, "With token"),
            SessionParams("1661161705205", null, PUBLIC_KEY, DEFAULT_CUSTOMER_KEY, DEFAULT_CUSTOMER_EMAIL, "Without token"),
            SessionParams("1584440932619", "dniplpm7ct3tg9e3", PUBLIC_KEY, DEFAULT_CUSTOMER_KEY, DEFAULT_CUSTOMER_EMAIL, "Sbp pay with token"),
            SessionParams("1674123391307", "rpcmn7osqle5sj2r", PUBLIC_KEY, DEFAULT_CUSTOMER_KEY, DEFAULT_CUSTOMER_EMAIL, "new merchant")
        )
    }
}
