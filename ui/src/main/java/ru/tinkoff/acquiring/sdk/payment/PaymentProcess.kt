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

package ru.tinkoff.acquiring.sdk.payment

import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.BrowseSbpBankState
import ru.tinkoff.acquiring.sdk.models.CollectDataState
import ru.tinkoff.acquiring.sdk.models.PaymentSource
import ru.tinkoff.acquiring.sdk.models.RejectedState
import ru.tinkoff.acquiring.sdk.models.ThreeDsState
import ru.tinkoff.acquiring.sdk.models.enums.DataTypeQr
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.AttachedCard
import ru.tinkoff.acquiring.sdk.models.paysources.CardSource
import ru.tinkoff.acquiring.sdk.models.paysources.GooglePay
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.FAIL_MAPI_SESSION_ID
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.RECURRING_TYPE_KEY
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.RECURRING_TYPE_VALUE
import ru.tinkoff.acquiring.sdk.requests.InitRequest
import ru.tinkoff.acquiring.sdk.responses.ChargeResponse
import ru.tinkoff.acquiring.sdk.responses.Check3dsVersionResponse
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager
import ru.tinkoff.acquiring.sdk.utils.getIpAddress
import java.util.*

/**
 * Позволяет создавать и управлять процессом оплаты
 *
 * @author Mariya Chernyadieva
 */
class PaymentProcess internal constructor(private val sdk: AcquiringSdk) {

    /**
     * Возвращает текущее состояние процесса оплаты
     */
    var state: PaymentState? = null
        private set

    private val coroutine = CoroutineManager(exceptionHandler = { handleException(it) })
    private var listener: PaymentListener? = null

    private lateinit var paymentSource: PaymentSource
    private var check3dsVersionResponse: Check3dsVersionResponse? = null
    private var collectedDeviceData: Map<String, String>? = null
    private var initRequest: InitRequest? = null
    private var paymentType: PaymentType? = null
    private var paymentId: Long? = null
    private var email: String? = null

    private var paymentResult: PaymentResult? = null
    private var sdkState: AsdkState? = null
    private var error: Throwable? = null

    private var isChargeWasRejected = false
    private var rejectedPaymentId: Long? = null

    /**
     * Создает объект полного процесса - инициация и подтверждение, устанавливает настройки оплаты
     * @return сконфигурированный объект для проведения оплаты
     */
    fun createPaymentProcess(paymentSource: PaymentSource, paymentOptions: PaymentOptions, email: String? = null): PaymentProcess {
        this.paymentSource = paymentSource
        this.initRequest = configureInitRequest(paymentOptions)
        this.paymentType = CardPaymentType
        this.email = email

        sendToListener(PaymentState.CREATED)
        return this
    }

    /**
     * Создает объект процесса для проведения подтверждения оплаты, устанавливает настройки оплаты
     * @return сконфигурированный объект для проведения оплаты
     */
    fun createFinishProcess(paymentId: Long, paymentSource: PaymentSource, email: String? = null): PaymentProcess {
        this.paymentId = paymentId
        this.paymentSource = paymentSource
        this.paymentType = FinishPaymentType
        this.email = email

        sendToListener(PaymentState.CREATED)
        return this
    }

    /**
     * Создает объект процесса для проведения оплаты с помощью Системы быстрых платежей
     * @return сконфигурированный объект для проведения оплаты
     */
    fun createSbpPaymentProcess(paymentOptions: PaymentOptions): PaymentProcess {
        this.initRequest = configureInitRequest(paymentOptions)
        this.paymentType = SbpPaymentType

        sendToListener(PaymentState.CREATED)
        return this
    }

    /**
     * Позволяет подписаться на события процесса
     * @return сконфигурированный объект для проведения оплаты
     */
    fun subscribe(listener: PaymentListener): PaymentProcess {
        this.listener = listener
        sendToListener(state)
        return this
    }

    /**
     * Позволяет отписаться от событий процесса
     */
    fun unsubscribe() {
        this.listener = null
    }

    /**
     * Запускает полный или подтверждающий процесс оплаты в зависимости от созданного процесса
     * @return сконфигурированный объект для проведения оплаты
     */
    fun start(): PaymentProcess {
        when (paymentType) {
            SbpPaymentType, CardPaymentType -> callInitRequest(initRequest!!)
            FinishPaymentType -> finishPayment(paymentId!!, paymentSource)
        }
        sendToListener(PaymentState.STARTED)
        return this
    }

    /**
     * Останавливает процесс оплаты
     */
    fun stop() {
        coroutine.cancelAll()
        sendToListener(PaymentState.STOPPED)
    }

    private fun sendToListener(state: PaymentState?) {
        this.state = state
        when (state) {
            PaymentState.SUCCESS -> listener?.onSuccess(paymentResult!!.paymentId!!, paymentResult!!.cardId)
            PaymentState.ERROR -> listener?.onError(error!!)
            PaymentState.CHARGE_REJECTED, PaymentState.THREE_DS_NEEDED, PaymentState.BROWSE_SBP_BANK -> listener?.onUiNeeded(sdkState!!)
            PaymentState.THREE_DS_DATA_COLLECTING -> {
                listener?.onUiNeeded(sdkState!!)
                collectedDeviceData = (sdkState as CollectDataState).data
            }
            else -> Unit
        }
        listener?.onStatusChanged(state)
    }

    private fun finishPayment(paymentId: Long, paymentSource: PaymentSource, email: String? = null) {
        when {
            paymentSource is AttachedCard && initRequest?.recurrent == true && paymentSource.rebillId != null -> {
                callChargeRequest(paymentId, paymentSource)
            }
            paymentSource is GooglePay || state == PaymentState.THREE_DS_V2_REJECTED -> {
                callFinishAuthorizeRequest(paymentId, paymentSource, email)
            }
            paymentSource is CardSource -> callCheck3DsVersion(paymentId, paymentSource, email)
            else -> {
                error = IllegalStateException("Unsupported payment source. Use AttachedCard, CardData or GooglePay source")
                sendToListener(PaymentState.ERROR)
            }
        }
    }

    private fun callInitRequest(request: InitRequest) {
        if (isChargeWasRejected && rejectedPaymentId != null) {
            request.data = modifyRejectedData(request)
        }

        coroutine.call(request,
                onSuccess = {
                    if (paymentType == CardPaymentType) {
                        finishPayment(it.paymentId!!, paymentSource, email)
                    } else if (paymentType == SbpPaymentType) {
                        callGetQr(it.paymentId!!)
                    }
                })
    }

    private fun callGetQr(paymentId: Long) {
        val request = sdk.getQr {
            this.paymentId = paymentId
            dataType = DataTypeQr.PAYLOAD
        }

        coroutine.call(request,
                onSuccess = { response ->
                    sdkState = BrowseSbpBankState(paymentId, response.data!!)
                    sendToListener(PaymentState.BROWSE_SBP_BANK)
                }
        )
    }

    private fun callCheck3DsVersion(paymentId: Long, paymentSource: CardSource, email: String? = null) {
        val check3DsRequest = sdk.check3DsVersion {
            this.paymentId = paymentId
            this.paymentSource = paymentSource
        }

        coroutine.call(check3DsRequest,
                onSuccess = { response ->
                    var data: MutableMap<String, String>? = null
                    if (response.serverTransId != null) {
                        if (!response.threeDsMethodUrl.isNullOrEmpty()) {
                            this.check3dsVersionResponse = response
                        }
                        sdkState = CollectDataState(response)
                        sendToListener(PaymentState.THREE_DS_DATA_COLLECTING)
                        data = mutableMapOf()
                        this.collectedDeviceData?.let { data.putAll(it) }
                    }

                    callFinishAuthorizeRequest(paymentId, paymentSource, email, data, response.version)
                })
    }

    private fun callFinishAuthorizeRequest(
            paymentId: Long,
            paymentSource: PaymentSource,
            email: String? = null,
            data: Map<String, String>? = null,
            threeDsVersion: String? = null
    ) {
        val ipAddress = if (data != null) getIpAddress() else null

        val finishRequest = sdk.finishAuthorize {
            this.paymentId = paymentId
            this.email = email
            this.paymentSource = paymentSource
            this.data = data
            ip = ipAddress
            sendEmail = email != null
        }

        coroutine.call(finishRequest,
                onSuccess = { response ->
                    val threeDsData = response.getThreeDsData()
                    val cardId = if (paymentSource is AttachedCard) paymentSource.cardId else null

                    if (threeDsData.isThreeDsNeed) {
                        threeDsData.version = threeDsVersion
                        sdkState = ThreeDsState(threeDsData)
                        sendToListener(PaymentState.THREE_DS_NEEDED)
                    } else {
                        paymentResult = PaymentResult(response.paymentId, cardId)
                        sendToListener(PaymentState.SUCCESS)
                    }
                })
    }

    private fun callChargeRequest(paymentId: Long, paymentSource: AttachedCard) {
        val chargeRequest = sdk.charge {
            this.paymentId = paymentId
            rebillId = paymentSource.rebillId
        }

        coroutine.call(chargeRequest,
                onSuccess = {
                    val payInfo = it.getPaymentInfo()
                    if (payInfo.isSuccess) {
                        paymentResult = PaymentResult(it.paymentId, paymentSource.cardId)
                        sendToListener(PaymentState.SUCCESS)
                    } else {
                        error = IllegalStateException("Unknown charge state with error code: ${payInfo.errorCode}")
                        sendToListener(PaymentState.ERROR)
                    }
                },
                onFailure = {
                    if (it is AcquiringApiException && it.response != null &&
                            it.response!!.errorCode == AcquiringApi.API_ERROR_CODE_CHARGE_REJECTED) {
                        val payInfo = (it.response as ChargeResponse).getPaymentInfo()
                        isChargeWasRejected = true
                        rejectedPaymentId = payInfo.paymentId
                        sdkState = RejectedState(payInfo.cardId!!)
                        sendToListener(PaymentState.CHARGE_REJECTED)
                    } else {
                        handleException(it)
                    }
                })
    }

    private fun handleException(throwable: Throwable) {
        if (throwable is AcquiringApiException && throwable.response != null &&
                throwable.response!!.errorCode == AcquiringApi.API_ERROR_CODE_3DSV2_NOT_SUPPORTED) {
            sendToListener(PaymentState.THREE_DS_V2_REJECTED)
            callInitRequest(initRequest!!)
        } else {
            error = throwable
            sendToListener(PaymentState.ERROR)
        }
    }

    private fun configureInitRequest(paymentOptions: PaymentOptions): InitRequest {
        val order = paymentOptions.order

        return sdk.init {
            orderId = order.orderId
            amount = order.amount.coins
            description = order.description
            chargeFlag = order.recurrentPayment
            recurrent = order.recurrentPayment
            receipt = order.receipt
            receipts = order.receipts
            shops = order.shops
            data = order.additionalData
            customerKey = paymentOptions.customer.customerKey
            language = AsdkLocalization.language.name
        }
    }

    private fun modifyRejectedData(request: InitRequest): Map<String, String> {
        val map = HashMap<String, String>()
        map[RECURRING_TYPE_KEY] = RECURRING_TYPE_VALUE
        map[FAIL_MAPI_SESSION_ID] = rejectedPaymentId.toString()

        val data = request.data?.toMutableMap() ?: mutableMapOf()
        data.putAll(map)

        return data.toMap()
    }
}

