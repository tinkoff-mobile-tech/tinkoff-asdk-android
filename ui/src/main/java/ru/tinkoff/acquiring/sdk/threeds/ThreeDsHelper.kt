package ru.tinkoff.acquiring.sdk.threeds

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsCertInfo.CertType.Companion.toWrapperType
import ru.tinkoff.core.components.threedswrapper.ThreeDSWrapper
import ru.tinkoff.core.components.threedswrapper.ThreeDSWrapper.Companion.setSdkAppId
import java.util.UUID
import java.util.concurrent.TimeUnit

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

    suspend fun initWrapper(context: Context): ThreeDSWrapper {
        val wrapper = ThreeDSWrapper(when (AcquiringSdk.isDeveloperMode) {
            true -> ThreeDSWrapper.EmbeddedCertsInfo.TEST
            else -> ThreeDSWrapper.EmbeddedCertsInfo.PROD
        }).init(context, ThreeDSWrapper.newConfigParameters {
            setSdkAppId(getSdkAppId(context).toString())
        })
        val config = updateCertsConfigIfNeeded()
        wrapper.updateCertsIfNeeded(config)
        config?.certsInfo?.let { psToDsIdMap = it.mapPsToDsId() }
        config?.certCheckInterval?.toLongOrNull()?.let { certConfigUpdateInterval = it }
        return wrapper
    }

    fun getDsId(paymentSystem: String): String? = psToDsIdMap[paymentSystem]

    fun isAppBasedFlow(threeDsVersion: String?) = when (threeDsVersion) {
        "2.1.0" -> true
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
}