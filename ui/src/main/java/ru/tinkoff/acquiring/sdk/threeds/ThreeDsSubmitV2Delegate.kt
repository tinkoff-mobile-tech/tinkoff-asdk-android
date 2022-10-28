package ru.tinkoff.acquiring.sdk.threeds

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import okhttp3.Response
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.NetworkException
import ru.tinkoff.acquiring.sdk.models.ThreeDsData
import ru.tinkoff.acquiring.sdk.network.AcquiringApi

internal class ThreeDsSubmitV2Delegate(private val sdk: AcquiringSdk) {

    fun shouldInterceptRequest(
        webResourceRequest: WebResourceRequest?,
        data: ThreeDsData
    ): WebResourceResponse? {
        webResourceRequest ?: return null
        return if (shouldIntercept(webResourceRequest.url.pathSegments, webResourceRequest.method)) {
            try {
                formWebResponse(
                    sdk.submit3DSAuthorizationFromWebView(data.paymentId.toString()).call()
                )
            } catch (e: NetworkException) {
                null
            }
        } else {
            null
        }
    }

    fun shouldIntercept(segments: List<String>?, method: String): Boolean {
        segments ?: return false
        return (segments.contains(AcquiringApi.SUBMIT_3DS_AUTHORIZATION_V2) && method == "POST")
    }

    private fun formWebResponse(response: Response?) = response?.let {
        WebResourceResponse(
            "application/json",
            "UTF-8",
            it.body?.byteStream()
        )
    }
}