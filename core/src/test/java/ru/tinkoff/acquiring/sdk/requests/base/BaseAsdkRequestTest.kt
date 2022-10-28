package ru.tinkoff.acquiring.sdk.requests.base

import org.junit.Before
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.utils.SampleAcquiringTokenGenerator

open class BaseAsdkRequestTest {

    lateinit var sdk: AcquiringSdk

    @Before
    fun setUp() {
        sdk = AcquiringSdk(DEFAULT_CUSTOMER_KEY, PUBLIC_KEY)
        AcquiringSdk.tokenGenerator = SampleAcquiringTokenGenerator(PASSWORD)
    }

    companion object {
        private const val PASSWORD = "12345678"
        private const val PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5Yg3RyEkszggDVMDHCAG\n" +
                    "zJm0mYpYT53BpasrsKdby8iaWJVACj8ueR0Wj3Tu2BY64HdIoZFvG0v7UqSFztE/\n" +
                    "zUvnznbXVYguaUcnRdwao9gLUQO2I/097SHF9r++BYI0t6EtbbcWbfi755A1EWfu\n" +
                    "9tdZYXTrwkqgU9ok2UIZCPZ4evVDEzDCKH6ArphVc4+iKFrzdwbFBmPmwi5Xd6CB\n" +
                    "9Na2kRoPYBHePGzGgYmtKgKMNs+6rdv5v9VB3k7CS/lSIH4p74/OPRjyryo6Q7Nb\n" +
                    "L+evz0+s60Qz5gbBRGfqCA57lUiB3hfXQZq5/q1YkABOHf9cR6Ov5nTRSOnjORgP\n" +
                    "jwIDAQAB"

        private const val DEFAULT_CUSTOMER_KEY = "TestSDK_CustomerKey1"
        private const val DEFAULT_CUSTOMER_EMAIL = "user@example.com"
    }
}