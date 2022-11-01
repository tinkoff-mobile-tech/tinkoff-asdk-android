import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.*
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.NetworkException
import ru.tinkoff.acquiring.sdk.models.ThreeDsData
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.SUBMIT_3DS_AUTHORIZATION
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.SUBMIT_3DS_AUTHORIZATION_V2
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.SUBMIT_RANDOM_AMOUNT_METHOD
import ru.tinkoff.acquiring.sdk.requests.Submit3DSAuthorizationWebViewRequest
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsSubmitV2Delegate
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader

internal class ThreeDsSubmitDelegateTest : BaseAsdkUiTest() {

    private val threeDsSubmitV2Delegate = ThreeDsSubmitV2Delegate(
        AcquiringSdk(customerKey, publicKey)
    )

    private val submitV1Segments = listOf(SUBMIT_3DS_AUTHORIZATION)
    private val submitV2Segments = listOf(SUBMIT_3DS_AUTHORIZATION_V2)
    private val otherRequestSegments = listOf(SUBMIT_RANDOM_AMOUNT_METHOD)

    private val mockedV2Uri = mock<Uri> { on { pathSegments } doReturn submitV2Segments }

    @Test
    fun `when any request posted`() {
        listOf(submitV1Segments, otherRequestSegments).forEach {
            Assert.assertFalse(
                threeDsSubmitV2Delegate.shouldIntercept(
                    it,
                    "POST"
                )
            )
        }
    }

    @Test
    fun `when get request send`() {
        listOf(submitV1Segments, otherRequestSegments, submitV2Segments).forEach {
            Assert.assertFalse(
                threeDsSubmitV2Delegate.shouldIntercept(
                    it,
                    "GET"
                )
            )
        }
    }

    @Test
    fun `when submit v2 request send`() {
        Assert.assertTrue(
            threeDsSubmitV2Delegate.shouldIntercept(
                submitV2Segments,
                "POST"
            )
        )
    }

    @Test
    fun `test submit v2 request fail`() {
        val webResourceRequest = mock<WebResourceRequest> {
            on { url } doReturn mockedV2Uri
            on { method } doReturn "POST"
        }
        val submit3DSAuthorizationWebViewRequest = mock<Submit3DSAuthorizationWebViewRequest> {
            on { call() } doAnswer { throw NetworkException("error") }
        }
        Assert.assertNull(
            ThreeDsSubmitV2Delegate(
                mock {
                    on { submit3DSAuthorizationFromWebView(any()) } doReturn submit3DSAuthorizationWebViewRequest
                }
            ).shouldInterceptRequest(webResourceRequest, ThreeDsData(isThreeDsNeed = true))
        )
    }
}