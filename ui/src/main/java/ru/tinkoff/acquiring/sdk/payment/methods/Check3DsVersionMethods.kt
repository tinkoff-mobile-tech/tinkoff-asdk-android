package ru.tinkoff.acquiring.sdk.payment.methods

import android.app.Application
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.CardSource
import ru.tinkoff.acquiring.sdk.responses.Check3dsVersionResponse
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsAppBasedTransaction
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsDataCollector
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager

/**
 * Created by i.golovachev
 */
interface Check3DsVersionMethods {

    class Data(
        val threeDsVersion: String,
        val threeDsTransaction: ThreeDsAppBasedTransaction? = null,
        val additionalData: Map<String, String>
    )

    suspend fun callCheck3DsVersion(
        paymentId: Long,
        paymentSource: CardSource,
        paymentOptions: PaymentOptions,
        email: String? = null
    ): Data

}

internal class Check3DsVersionMethodsSdkImpl(
    private val sdk: AcquiringSdk,
    private val application: Application,
    private val threeDsDataCollector: ThreeDsDataCollector,
    private val coroutineManager: CoroutineManager = CoroutineManager(),
    private val default3dsVersion: String = "2"
) : Check3DsVersionMethods {
    override suspend fun callCheck3DsVersion(
        paymentId: Long,
        paymentSource: CardSource,
        paymentOptions: PaymentOptions,
        email: String?
    ): Check3DsVersionMethods.Data {
        val check3Ds = check3Ds(paymentId, paymentSource).execute()
        val data = getAdditionalData(check3Ds)
        val check3DsVersion = check3Ds.version ?: default3dsVersion
        val threeDsTransaction = getThreeDsAppBasedTransaction(check3DsVersion, check3Ds, data)
        return Check3DsVersionMethods.Data(
            check3DsVersion, threeDsTransaction, data
        )
    }

    private fun check3Ds(paymentId: Long, paymentSource: CardSource) = sdk.check3DsVersion {
        this.paymentId = paymentId
        this.paymentSource = paymentSource
    }

    private suspend fun getAdditionalData(check3Ds: Check3dsVersionResponse): MutableMap<String, String> {
        val data = mutableMapOf<String, String>()
        if (check3Ds.serverTransId != null) {
            coroutineManager.withMain {
                data.putAll(threeDsDataCollector(application, check3Ds))
            }
        }
        return data
    }

    private suspend fun getThreeDsAppBasedTransaction(
        check3DsVersion: String,
        check3Ds: Check3dsVersionResponse,
        data: MutableMap<String, String>
    ): ThreeDsAppBasedTransaction? {
        val threeDsTransaction: ThreeDsAppBasedTransaction? = null
        if (ThreeDsHelper.isAppBasedFlow(check3DsVersion)) {
            // can throw error
            val paymentSystem = checkNotNull(check3Ds.paymentSystem) {
                "check3Ds Payment system must be not null"
            }
            coroutineManager.withMain {
                ThreeDsHelper.CreateAppBasedTransaction(
                    application, check3DsVersion, paymentSystem, data
                )
            }
        }
        return threeDsTransaction
    }
}