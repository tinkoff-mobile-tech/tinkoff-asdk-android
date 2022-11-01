package ru.tinkoff.acquiring.sdk.requests

import org.junit.Assert.*
import org.junit.Test
import ru.tinkoff.acquiring.sdk.requests.base.BaseAsdkRequestTest

class Submit3DSAuthorizationWebViewRequestTest : BaseAsdkRequestTest() {

    @Test
    fun `check request params`() {
        val request = sdk.submit3DSAuthorizationFromWebView("paymentId")
        val urlEncoded = request.getRequestBody()
        val params = urlEncoded.split("&")

        assertTrue(params.contains("PaymentId=paymentId"))
        assertTrue(params.contains("TerminalKey=TestSDK_CustomerKey1"))
        assertTrue(params.contains("Token=361a2ea4f43a21ec09313df73df3cb14d2c3016ef9e520f92cbb19c79f8cd759"))

        assertFalse(params.contains("Token"))
        assertFalse(params.contains("PaymentId=TestSDK_CustomerKey1"))
    }
}