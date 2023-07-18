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

package ru.tinkoff.acquiring.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import ru.tinkoff.acquiring.sdk.models.*
import ru.tinkoff.acquiring.sdk.models.options.FeaturesOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.*
import ru.tinkoff.acquiring.sdk.payment.MirPayProcess
import ru.tinkoff.acquiring.sdk.payment.SbpPaymentProcess
import ru.tinkoff.acquiring.sdk.payment.TpayProcess
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo
import ru.tinkoff.acquiring.sdk.ui.activities.BaseAcquiringActivity
import ru.tinkoff.acquiring.sdk.ui.activities.QrCodeActivity
import ru.tinkoff.acquiring.sdk.ui.activities.YandexPaymentActivity
import kotlin.reflect.KClass

/**
 * Точка входа для взаимодействия с Acquiring SDK
 *
 * В некоторых случаях для формирования запросов к API может потребоваться генерация токена для
 * подписи запроса, см. [AcquiringSdk.tokenGenerator].
 *
 * @param terminalKey    ключ терминала. Выдается после подключения к Tinkoff Acquiring
 * @param publicKey      экземпляр PublicKey созданный из публичного ключа, выдаваемого вместе с
 *                       terminalKey
 *
 * @author Mariya Chernyadieva
 */
class TinkoffAcquiring(
    private val applicationContext: Context,
    private val terminalKey: String,
    private val publicKey: String
) {

    val sdk = AcquiringSdk(terminalKey, publicKey)

    /**
     * Запуск экрана Acquiring SDK для проведения оплаты
     *
     * @param activity        контекст для запуска экрана из Activity
     * @param options         настройки платежной сессии и визуального отображения экрана
     * @param requestCode     код для получения результата, по завершению работы экрана Acquiring SDK
     * @param yandexPayToken  параметр платежной сессии от яндекса
     */
    fun openYandexPaymentScreen(activity: Activity,
                          options: PaymentOptions,
                          requestCode: Int,
                          yandexPayToken: String,
                          paymentId: Long? = null
    ) {
        options.asdkState = YandexPayState(yandexPayToken, paymentId)
        val intent = prepareIntent(activity, options, YandexPaymentActivity::class)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Создает платежную сессию в рамках оплаты по Системе быстрых платежей
     */
    @MainThread
    fun initSbpPaymentSession() {
        SbpPaymentProcess.init(sdk, applicationContext.packageManager)
    }

    /**
     * Создает платежную сессию в рамках оплаты по tinkoffPay
     */
    @MainThread
    fun initTinkoffPayPaymentSession() {
        TpayProcess.init(sdk)
    }

    /**
     * Создает платежную сессию в рамках оплаты по MirPay
     */
    @MainThread
    fun initMirPayPaymentSession() {
        MirPayProcess.init(sdk)
    }

    /**
     * Проверка доступных спосбов оплаты
     */
    fun checkTerminalInfo(onSuccess: (TerminalInfo?) -> Unit,
                          onFailure: ((Throwable) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val mainScope = this
            val result = sdk.getTerminalPayMethods()
                .performSuspendRequest()
                .map { it.terminalInfo }
            withContext(Dispatchers.Main) {
                result.fold(onSuccess = onSuccess, onFailure = { onFailure?.invoke(it) })
                mainScope.cancel()
            }
        }
    }

    /**
     * Запуск экрана с отображением QR кода для оплаты покупателем
     *
     * @param activity    контекст для запуска экрана из Activity
     * @param options     настройки платежной сессии и визуального отображения экрана
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openDynamicQrScreen(activity: Activity, options: PaymentOptions, requestCode: Int) {
        val intent = prepareIntent(activity, options, QrCodeActivity::class)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана с отображением QR кода для оплаты покупателем
     *
     * @param fragment    контекст для запуска экрана из Fragment
     * @param options     настройки платежной сессии и визуального отображения экрана
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openDynamicQrScreen(fragment: Fragment, options: PaymentOptions, requestCode: Int) {
        val intent = prepareIntent(fragment.requireContext(), options, QrCodeActivity::class)
        fragment.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана с отображением QR кода для оплаты покупателем
     *
     * @param activity        контекст для запуска экрана из Activity
     * @param featuresOptions конфигурация визуального отображения экрана
     * @param requestCode     код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openStaticQrScreen(activity: Activity, featuresOptions: FeaturesOptions, requestCode: Int) {
        val options = BaseAcquiringOptions().apply {
            features = featuresOptions
        }
        val intent = prepareIntent(activity, options, QrCodeActivity::class)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана с отображением QR кода для оплаты покупателем
     *
     * @param fragment        контекст для запуска экрана из Fragment
     * @param featuresOptions конфигурация визуального отображения экрана
     * @param requestCode     код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openStaticQrScreen(fragment: Fragment, featuresOptions: FeaturesOptions, requestCode: Int) {
        val options = BaseAcquiringOptions().apply {
            features = featuresOptions
        }
        val intent = prepareIntent(fragment.requireContext(), options, QrCodeActivity::class)
        fragment.startActivityForResult(intent, requestCode)
    }

    private fun prepareIntent(context: Context, options: BaseAcquiringOptions, cls: KClass<*>): Intent {
        options.setTerminalParams(terminalKey, publicKey)
        return BaseAcquiringActivity.createIntent(context, options, cls)
    }

    fun attachCardOptions(setup: AttachCardOptions.() -> Unit) = AttachCardOptions().also { options ->
        options.setTerminalParams(terminalKey, publicKey)
        setup(options)
    }

    fun savedCardsOptions(setup: SavedCardsOptions.() -> Unit) = SavedCardsOptions().also { options ->
        options.setTerminalParams(terminalKey, publicKey)
        setup(options)
    }
}
