package ru.tinkoff.acquiring.sdk.threeds

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.view.WindowManager
import android.webkit.WebView
import com.emvco3ds.sdk.spec.CompletionEvent
import com.emvco3ds.sdk.spec.ProtocolErrorEvent
import com.emvco3ds.sdk.spec.RuntimeErrorEvent
import com.emvco3ds.sdk.spec.Transaction
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.models.ThreeDsData
import ru.tinkoff.acquiring.sdk.models.options.screen.BaseAcquiringOptions
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.COMPLETE_3DS_METHOD_V2
import ru.tinkoff.acquiring.sdk.responses.AttachCardResponse
import ru.tinkoff.acquiring.sdk.responses.Check3dsVersionResponse
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsCertInfo.CertType.Companion.toWrapperType
import ru.tinkoff.acquiring.sdk.ui.activities.ThreeDsActivity
import ru.tinkoff.acquiring.sdk.utils.Base64
import ru.tinkoff.acquiring.sdk.utils.getTimeZoneOffsetInMinutes
import ru.tinkoff.core.components.threedswrapper.ChallengeStatusReceiverAdapter
import ru.tinkoff.core.components.threedswrapper.ThreeDSWrapper
import ru.tinkoff.core.components.threedswrapper.ThreeDSWrapper.Companion.cancelButtonCustomization
import ru.tinkoff.core.components.threedswrapper.ThreeDSWrapper.Companion.closeSafe
import ru.tinkoff.core.components.threedswrapper.ThreeDSWrapper.Companion.setSdkAppId
import ru.tinkoff.core.components.threedswrapper.ThreeDSWrapper.Companion.submitButtonCustomization
import ru.tinkoff.core.components.threedswrapper.ThreeDSWrapper.Companion.toolbarCustomization
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws

object ThreeDsHelper {

    private const val CERTS_CONFIG_URL_TEST = "https://asdk-config-test.s3-msk.tinkoff.ru/certs-configs/asdk-certs-config.json"
    private const val CERTS_CONFIG_URL_PROD = "https://asdk-config-prod.s3-msk.tinkoff.ru/certs-configs/asdk-certs-config.json"

    private const val PREFS_NAME = "tinkoff_asdk_prefs"
    private const val SDK_APP_ID_KEY = "sdk_app_id"

    /**
     * Максимальное время, в течение которого пользователь может подтвердить проведение транзакции
     * через 3DS app-based flow; задается в минутах, минимальное допустимое значение - 5 минут.
     */
    var maxTimeout = 5
        set(value) {
            field = value.coerceAtLeast(5)
        }

    /**
     * Минимальное время, через которое ASDK попытается снова обновить конфиг с актуальными
     * сертификатами для использования в 3DS SDK; задается в минутах.
     */
    var certConfigUpdateInterval = 240L

    private var certsConfig: ThreeDsCertsConfig? = null

    /**
     * Map of Payment System to Directory Server ID values.
     */
    private var psToDsIdMap = mapOf(
        "visa" to "A000000003",
        "mir" to "A000000658",
        "mc" to "A000000004",
        "upi" to "A000000333")

    private var lastCertConfigUpdate = 0L

    private val okHttpClient = OkHttpClient()
    private val gson = Gson()

    internal var threeDsStatus: ThreeDsStatus? = null

    fun getDsId(paymentSystem: String): String? = psToDsIdMap[paymentSystem]

    fun isAppBasedFlow(threeDsVersion: String?) = when (threeDsVersion) {
//        "2.1.0" -> true // todo uncomment then app-based 3DS is fixed
        else -> false
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun updateCertsConfigIfNeeded(): ThreeDsCertsConfig? {
        if (certsConfig != null && (System.currentTimeMillis() - lastCertConfigUpdate) <
            TimeUnit.MINUTES.toMillis(certConfigUpdateInterval)) {
            return certsConfig
        }
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(getCertsConfigUrl()).build()
                val response = okHttpClient.newCall(request).execute()
                val newConfig = gson.fromJson(response.body?.charStream(), ThreeDsCertsConfig::class.java)
                lastCertConfigUpdate = System.currentTimeMillis()
                if (newConfig != null) {
                    certsConfig = newConfig
                }
            } catch (ignored: Throwable) {
                // ignore
            }
            certsConfig
        }
    }

    private suspend fun ThreeDSWrapper.updateCertsIfNeeded(config: ThreeDsCertsConfig?) {
        val checkCerts = checkCerts()
        val certsUpdate = mutableListOf<ThreeDSWrapper.DsCertsUpdate>()
        config?.certsInfo?.forEach { certInfo ->
            val existing = checkCerts.find { it.dsId == certInfo.dsId }?.let {
                when (certInfo.type) {
                    ThreeDsCertInfo.CertType.DS -> it.dsCertInfo
                    ThreeDsCertInfo.CertType.CA -> it.caCertInfo
                }
            }
            if (existing?.certHash != certInfo.sha256Fingerprint || certInfo.forceUpdate) {
                certsUpdate.add(ThreeDSWrapper.DsCertsUpdate(
                    certInfo.dsId, certInfo.type.toWrapperType(), certInfo.url))
            }
        }
        updateCerts(certsUpdate)
    }

    private fun List<ThreeDsCertInfo>.mapPsToDsId() = mutableMapOf<String, String>().apply {
        this@mapPsToDsId.forEach { this[it.paymentSystem] = it.dsId }
    }

    private fun getSdkAppId(context: Context): UUID {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sdkAppIdString = prefs.getString(SDK_APP_ID_KEY, null)
        var sdkAppId = try {
            sdkAppIdString?.let { UUID.fromString(it) }
        } catch (ignored: IllegalArgumentException) {
            null
        }
        if (sdkAppId == null) {
            sdkAppId = UUID.randomUUID()
            prefs.edit().putString(SDK_APP_ID_KEY, sdkAppId.toString()).apply()
        }
        return sdkAppId!!
    }

    private fun getCertsConfigUrl() = when (AcquiringSdk.isDeveloperMode) {
        true -> CERTS_CONFIG_URL_TEST
        else -> CERTS_CONFIG_URL_PROD
    }

    object CollectData : ThreeDsDataCollector {

        private const val THREE_DS_CALLED_FLAG = "Y"
        private const val THREE_DS_NOT_CALLED_FLAG = "N"

        private val NOTIFICATION_URL = "${AcquiringApi.getUrl(COMPLETE_3DS_METHOD_V2)}/$COMPLETE_3DS_METHOD_V2"
        private val TERM_URL_V2 = ThreeDsActivity.TERM_URL_V2

        override operator fun invoke(context: Context, response: Check3dsVersionResponse?): MutableMap<String, String> {
            var threeDSCompInd = THREE_DS_NOT_CALLED_FLAG
            if (response?.threeDsMethodUrl != null) {
                val hiddenWebView = WebView(context)

                val threeDsMethodData = JSONObject().apply {
                    put("threeDSMethodNotificationURL", NOTIFICATION_URL)
                    put("threeDSServerTransID", response.serverTransId)
                }

                val dataBase64 = Base64.encodeToString(threeDsMethodData.toString().toByteArray(),
                    Base64.NO_PADDING or Base64.NO_WRAP).trim()
                val params = "threeDSMethodData=${URLEncoder.encode(dataBase64, "UTF-8")}"

                hiddenWebView.postUrl(response.threeDsMethodUrl!!, params.toByteArray())
                threeDSCompInd = THREE_DS_CALLED_FLAG
            }

            val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            val point = Point()
            display.getSize(point)

            return mutableMapOf<String, String>().apply {
                put("threeDSCompInd", threeDSCompInd)
                put("language", Locale.getDefault().toString().replace("_", "-"))
                put("timezone", getTimeZoneOffsetInMinutes())
                put("screen_height", "${point.y}")
                put("screen_width", "${point.x}")
                put("cresCallbackUrl", TERM_URL_V2)
            }
        }

        fun addExtraData(data: MutableMap<String, String>, response: Check3dsVersionResponse) {
            data.apply {
                put("version", response.version!!)
                put("tdsServerTransId" , response.serverTransId!!)
            }
        }

        fun addExtraThreeDsData(data: ThreeDsData,
                                acsTransId: String,
                                serverTransId: String,
                                version:String
        ) {
            data.apply {
                this.version = version
                this.tdsServerTransId = serverTransId
                this.acsTransId = acsTransId
            }
        }
    }

    object CreateAppBasedTransaction {

        @Throws(Throwable::class)
        suspend operator fun invoke(
            context: Context,
            threeDsVersion: String,
            paymentSystem: String,
            data: MutableMap<String, String>
        ): ThreeDsAppBasedTransaction {
            val wrapper = initWrapper(context)
            val transaction = initTransaction(context, wrapper, threeDsVersion, paymentSystem, data)
            return ThreeDsAppBasedTransaction(wrapper, transaction)
        }

        suspend fun initWrapper(context: Context): ThreeDSWrapper {
            val localisation = if (AsdkLocalization.isInitialized()) AsdkLocalization.resources else null

            val wrapper = ThreeDSWrapper(when (AcquiringSdk.isDeveloperMode) {
                true -> ThreeDSWrapper.EmbeddedCertsInfo.TEST
                else -> ThreeDSWrapper.EmbeddedCertsInfo.PROD
            }).init(context, ThreeDSWrapper.newConfigParameters {
                setSdkAppId(getSdkAppId(context).toString())
            }, Locale.getDefault().toString(), ThreeDSWrapper.newUiCustomization {
                toolbarCustomization {
                    headerText = localisation?.threeDsConfirmation ?: "Confirmation"
                    buttonText = localisation?.threeDsCancel ?: "Cancel"
                    backgroundColor = "#888888"
                }
                submitButtonCustomization {
                    backgroundColor = "#ffdd2d"
                }
                cancelButtonCustomization {
                    textColor = "#ffffff"
                }
            })
            val config = updateCertsConfigIfNeeded()
            wrapper.updateCertsIfNeeded(config)
            config?.certsInfo?.let { psToDsIdMap = it.mapPsToDsId() }
            config?.certCheckInterval?.toLongOrNull()?.let { certConfigUpdateInterval = it }
            return wrapper
        }

        @Throws(Throwable::class)
        fun initTransaction(
            context: Context,
            threeDSWrapper: ThreeDSWrapper,
            threeDsVersion: String,
            paymentSystem: String,
            data: MutableMap<String, String>
        ): Transaction {
            val dsId = getDsId(paymentSystem)
            if (dsId == null) {
                threeDSWrapper.cleanupSafe(context)
                throw AcquiringSdkException(IllegalArgumentException(
                    "Directory server ID for payment system \"$paymentSystem\" can't be found"))
            }
            var transaction: Transaction? = null

            try {
                transaction = threeDSWrapper.createTransaction(dsId, threeDsVersion)
                val authParams = transaction.authenticationRequestParameters

                data["sdkAppID"] = authParams.sdkAppID
                data["sdkEncData"] = android.util.Base64.encodeToString(
                    authParams.deviceData.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                data["sdkEphemPubKey"] = android.util.Base64.encodeToString(
                    authParams.sdkEphemeralPublicKey.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                data["sdkMaxTimeout"] = ThreeDsHelper.maxTimeout.toString()
                data["sdkReferenceNumber"] = authParams.sdkReferenceNumber
                data["sdkTransID"] = authParams.sdkTransactionID
                data["sdkInterface"] = "03"
                data["sdkUiType"] = "01,02,03,04,05"

            } catch (e: Throwable) {
                transaction?.closeSafe()
                threeDSWrapper.cleanupSafe(context)
                throw e
            }
            return transaction
        }
    }

    object Launch {

        const val RESULT_DATA = "result_data"
        const val ERROR_DATA = "result_error"
        const val RESULT_ERROR = 564

        @Throws(Throwable::class)
        suspend operator fun invoke(
            activity: Activity,
            requestCode: Int,
            options: BaseAcquiringOptions?,
            threeDsData: ThreeDsData,
            appBasedTransaction: ThreeDsAppBasedTransaction?,
            panSuffix: String = ""
        ) {
            if (isAppBasedFlow(threeDsData.version)) {
                launchAppBased(activity, threeDsData, appBasedTransaction!!)
            } else {
                launchBrowserBased(activity, requestCode, options!!, threeDsData, panSuffix)
            }
        }

        @Throws(Throwable::class)
        suspend fun launchAppBased(
            activity: Activity,
            threeDsData: ThreeDsData,
            appBasedTransaction: ThreeDsAppBasedTransaction
        ) {
            val wrapper = appBasedTransaction.wrapper
            val transaction = appBasedTransaction.transaction

            val challengeParameters = ThreeDSWrapper.newChallengeParameters {
                set3DSServerTransactionID(threeDsData.tdsServerTransId)
                acsTransactionID = threeDsData.acsTransId
                acsRefNumber = threeDsData.acsRefNumber
                acsSignedContent = threeDsData.acsSignedContent
            }
            val progressDialog = try {
                transaction.getProgressView(activity)
            } catch (e: Throwable) {
                wrapper.cleanupSafe(activity)
                transaction.closeSafe()
                throw e
            }
            withContext(Dispatchers.IO) {
                transaction.doChallenge(activity, challengeParameters,
                    object : ChallengeStatusReceiverAdapter(transaction, progressDialog) {
                        override fun completed(event: CompletionEvent?) {
                            super.completed(event)
                            wrapper.cleanupSafe(activity)
                            threeDsStatus = ThreeDsStatusSuccess(threeDsData, event!!.transactionStatus)
                        }

                        override fun cancelled() {
                            super.cancelled()
                            wrapper.cleanupSafe(activity)
                            threeDsStatus = ThreeDsStatusCanceled()
                        }

                        override fun timedout() {
                            super.timedout()
                            wrapper.cleanupSafe(activity)
                            val error = RuntimeException("3DS SDK transaction timeout")
                            threeDsStatus = ThreeDsStatusError(error)
                        }

                        override fun protocolError(event: ProtocolErrorEvent?) {
                            super.protocolError(event)
                            wrapper.cleanupSafe(activity)
                            val error = RuntimeException("3DS SDK protocol error: sdkTransactionID - ${event?.sdkTransactionID}, message - ${event?.errorMessage}")
                            threeDsStatus = ThreeDsStatusError(error)
                        }

                        override fun runtimeError(event: RuntimeErrorEvent?) {
                            super.runtimeError(event)
                            wrapper.cleanupSafe(activity)
                            val error = RuntimeException("3DS SDK runtime error: code - ${event?.errorCode}, message - ${event?.errorMessage}")
                            threeDsStatus = ThreeDsStatusError(error)
                        }
                    }, maxTimeout)
            }
        }

        fun launchBrowserBased(
            activity: Activity,
            requestCode: Int,
            options: BaseAcquiringOptions,
            threeDsData: ThreeDsData,
            panSuffix: String = ""
        ) {
            val intent = ThreeDsActivity.createIntent(activity, options, threeDsData, panSuffix)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    fun checkoutTransactionStatus(action: (status: ThreeDsStatus?) -> Unit) {
        action(threeDsStatus)
        threeDsStatus = null
    }

    fun ThreeDSWrapper.cleanupSafe(context: Context) {
        if (isInitialized()) {
            try {
                cleanup(context)
            } catch (ignored: Throwable) {
                // ignore
            }
        }
    }
}

class ThreeDsAppBasedTransaction(
    val wrapper: ThreeDSWrapper,
    val transaction: Transaction
)

interface ThreeDsDataCollector {

    operator fun invoke(context: Context, response: Check3dsVersionResponse?): MutableMap<String, String>
}
