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
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.localization.LocalizationSource
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.DefaultState
import ru.tinkoff.acquiring.sdk.models.FpsState
import ru.tinkoff.acquiring.sdk.models.GooglePayParams
import ru.tinkoff.acquiring.sdk.models.PaymentSource
import ru.tinkoff.acquiring.sdk.models.options.FeaturesOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.AttachCardOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.BaseAcquiringOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.models.paysources.AttachedCard
import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.models.paysources.GooglePay
import ru.tinkoff.acquiring.sdk.payment.PaymentProcess
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardsListActivity
import ru.tinkoff.acquiring.sdk.responses.TinkoffPayStatusResponse
import ru.tinkoff.acquiring.sdk.ui.activities.AttachCardActivity
import ru.tinkoff.acquiring.sdk.ui.activities.BaseAcquiringActivity
import ru.tinkoff.acquiring.sdk.ui.activities.NotificationPaymentActivity
import ru.tinkoff.acquiring.sdk.ui.activities.PaymentActivity
import ru.tinkoff.acquiring.sdk.ui.activities.QrCodeActivity

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
     * Создает платежную сессию. Для проведения оплаты с помощью привязанной карты.
     * Включает в себя инициирование нового платежа и подтверждение платежа.
     * Процесс асинхронный
     *
     * @param attachedCard   привязанная карта
     * @param paymentOptions настройки платежной сессии
     * @return объект для проведения оплаты
     */
    fun initPayment(attachedCard: AttachedCard, paymentOptions: PaymentOptions): PaymentProcess {
        paymentOptions.setTerminalParams(terminalKey, publicKey)
        return PaymentProcess(sdk, applicationContext).createPaymentProcess(attachedCard, paymentOptions)
    }

    /**
     * Создает платежную сессию. Для проведения оплаты с помощью карты.
     * Включает в себя инициирование нового платежа и подтверждение платежа.
     * Процесс асинхронный
     *
     * @param cardData       данные карты
     * @param paymentOptions настройки платежной сессии
     * @return объект для проведения оплаты
     */
    fun initPayment(cardData: CardData, paymentOptions: PaymentOptions): PaymentProcess {
        paymentOptions.setTerminalParams(terminalKey, publicKey)
        return PaymentProcess(sdk, applicationContext).createPaymentProcess(cardData, paymentOptions)
    }

    /**
     * Создает платежную сессию. Для проведения оплаты с помощью Google Pay.
     * Включает в себя инициирование нового платежа и подтверждение платежа
     * Процесс асинхронный
     *
     * @param googlePayToken токен для оплаты полученный через Google Pay
     * @param paymentOptions настройки платежной сессии
     * @return объект для проведения оплаты
     */
    fun initPayment(googlePayToken: String, paymentOptions: PaymentOptions): PaymentProcess {
        paymentOptions.setTerminalParams(terminalKey, publicKey)
        return PaymentProcess(sdk, applicationContext).createPaymentProcess(GooglePay(googlePayToken), paymentOptions)
    }

    /**
     * Создает платежную сессию для подтверждения ранее инициированного платежа.
     * Включает в себя только подтверждение платежа
     * Процесс асинхронный
     *
     * @param paymentId     уникальный идентификатор транзакции в системе банка,
     *                      полученный после проведения инициации платежа
     * @param paymentSource источник платежа
     * @return объект для проведения оплаты
     */
    fun finishPayment(paymentId: Long, paymentSource: PaymentSource): PaymentProcess {
        return PaymentProcess(sdk, applicationContext).createFinishProcess(paymentId, paymentSource)
    }

    /**
     * Запуск экрана Acquiring SDK для проведения оплаты
     *
     * @param activity    контекст для запуска экрана из Activity
     * @param options     настройки платежной сессии и визуального отображения экрана
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     * @param state       вспомогательный параметр для запуска экрана Acquiring SDK
     *                    с заданного состояния
     */
    @JvmOverloads
    fun openPaymentScreen(activity: Activity, options: PaymentOptions, requestCode: Int, state: AsdkState = DefaultState) {
        options.asdkState = state
        val intent = prepareIntent(activity, options, PaymentActivity::class.java)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана Acquiring SDK для проведения оплаты
     *
     * @param fragment    контекст для запуска экрана из Fragment
     * @param options     настройки платежной сессии и визуального отображения экрана
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     * @param state       вспомогательный параметр для запуска экрана Acquiring SDK
     *                    с заданного состояния
     */
    @JvmOverloads
    fun openPaymentScreen(fragment: Fragment, options: PaymentOptions, requestCode: Int, state: AsdkState = DefaultState) {
        options.asdkState = state
        val intent = prepareIntent(fragment.requireContext(), options, PaymentActivity::class.java)
        fragment.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск SDK для оплаты через Систему быстрых платежей
     *
     * @param activity    контекст для запуска экрана из Activity
     * @param options     настройки платежной сессии
     * @param requestCode код для получения результата, по завершению работы SDK
     */
    fun payWithSbp(activity: Activity, options: PaymentOptions, requestCode: Int) {
        openPaymentScreen(activity, options, requestCode, FpsState)
    }

    /**
     * Запуск SDK для оплаты через Систему быстрых платежей
     *
     * @param fragment    контекст для запуска экрана из Fragment
     * @param options     настройки платежной сессии
     * @param requestCode код для получения результата, по завершению работы SDK
     */
    fun payWithSbp(fragment: Fragment, options: PaymentOptions, requestCode: Int) {
        openPaymentScreen(fragment, options, requestCode, FpsState)
    }

    /**
     * Запуск SDK для оплаты через Систему быстрых платежей
     *
     * @param paymentId     уникальный идентификатор транзакции в системе банка,
     *                      полученный после проведения инициации платежа
     */
    fun payWithSbp(paymentId: Long): PaymentProcess {
        return PaymentProcess(sdk, applicationContext).createInitializedSbpPaymentProcess(paymentId)
    }

    /**
     * Проверка статуса возможности оплата с помощью Tinkoff Pay
     */
    fun checkTinkoffPayStatus(
        onSuccess: (TinkoffPayStatusResponse) -> Unit,
        onFailure: ((Throwable) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            sdk.tinkoffPayStatus().execute({
                launch(Dispatchers.Main) { onSuccess(it) }
            }, {
                launch(Dispatchers.Main) { onFailure?.invoke(it) }
            })
        }
    }

    /**
     * Запуск SDK для оплаты через Tinkoff Pay. У возвращенгого объекта следует указать
     * слушатель событий с помощью метода [PaymentProcess.subscribe] и вызвать метод
     * [PaymentProcess.start] для запуска сценария оплаты.
     *
     * @param options настройки платежной сессии
     * @param version версия Tinkoff Pay
     */
    fun payWithTinkoffPay(options: PaymentOptions, version: String): PaymentProcess {
        return PaymentProcess(sdk, applicationContext).createTinkoffPayPaymentProcess(options, version)
    }

    /**
     * Запуск экрана Acquiring SDK для привязки новой карты
     *
     * @param activity    контекст для запуска экрана из Activity
     * @param options     настройки привязки карты и визуального отображения экрана
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     */
    @Deprecated("registerForActivityResult(AttachCardContract) { cardId -> }.launch(options)",
        ReplaceWith("registerForActivityResult(AttachCardContract) { cardId ->\n" +
                "    // handle result\n" +
                "}.launch(attachCardOptions {\n" +
                "    //setup options\n" +
                "})"))
    fun openAttachCardScreen(activity: Activity, options: AttachCardOptions, requestCode: Int) {
        val intent = prepareIntent(activity, options, AttachCardActivity::class.java)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана Acquiring SDK для привязки новой карты
     *
     * @param fragment    контекст для запуска экрана из Fragment
     * @param options     настройки привязки карты и визуального отображения экрана
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openAttachCardScreen(fragment: Fragment, options: AttachCardOptions, requestCode: Int) {
        val intent = prepareIntent(fragment.requireContext(), options, AttachCardActivity::class.java)
        fragment.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана Acquiring SDK для просмотра сохраненных карт
     *
     * @param activity          контекст для запуска экрана из Activity
     * @param savedCardsOptions настройки экрана сохраненных карт
     * @param requestCode       код для получения результата, по завершению работы экрана Acquiring SDK.
     *                          В случае удаления/добавления карты на экране, возвращается intent с
     *                          параметром Boolean по ключу [TinkoffAcquiring.EXTRA_CARD_LIST_CHANGED]
     *                          В случае выбора покупателем приоритетной карты, возвращается intent
     *                          с параметром String по ключу [TinkoffAcquiring.EXTRA_CARD_ID]
     */
    fun openSavedCardsScreen(activity: Activity, savedCardsOptions: SavedCardsOptions, requestCode: Int) {
        val intent = prepareIntent(activity, savedCardsOptions, CardsListActivity::class.java)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана Acquiring SDK для просмотра сохраненных карт
     *
     * @param fragment          контекст для запуска экрана из Fragment
     * @param savedCardsOptions настройки экрана сохраненных карт
     * @param requestCode       код для получения результата, по завершению работы экрана Acquiring SDK.
     *                          В случае удаления/добавления карты на экране, возвращается intent с
     *                          параметром Boolean по ключу [TinkoffAcquiring.EXTRA_CARD_LIST_CHANGED]
     *                          В случае выбора покупателем приоритетной карты, возвращается intent
     *                          с параметром String по ключу [TinkoffAcquiring.EXTRA_CARD_ID]
     */
    fun openSavedCardsScreen(fragment: Fragment, savedCardsOptions: SavedCardsOptions, requestCode: Int) {
        val intent = prepareIntent(fragment.requireContext(), savedCardsOptions, CardsListActivity::class.java)
        fragment.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана с отображением QR кода для оплаты покупателем
     *
     * @param activity    контекст для запуска экрана из Activity
     * @param options     настройки платежной сессии и визуального отображения экрана
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openDynamicQrScreen(activity: Activity, options: PaymentOptions, requestCode: Int) {
        val intent = prepareIntent(activity, options, QrCodeActivity::class.java)
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
        val intent = prepareIntent(fragment.requireContext(), options, QrCodeActivity::class.java)
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
        val intent = prepareIntent(activity, options, QrCodeActivity::class.java)
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
        val intent = prepareIntent(fragment.requireContext(), options, QrCodeActivity::class.java)
        fragment.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана с отображением QR кода для оплаты покупателем
     *
     * @param activity     контекст для запуска экрана
     * @param localization локализация экрана
     * @param requestCode  код для получения результата, по завершению работы экрана Acquiring SDK
     */
    @Deprecated("Replaced with expanded method",
        ReplaceWith("openStaticQrScreen(activity, FeaturesOptions().apply { localizationSource = localization }, requestCode)"))
    fun openStaticQrScreen(activity: Activity, localization: LocalizationSource, requestCode: Int) {
        openStaticQrScreen(activity, FeaturesOptions().apply { localizationSource = localization }, requestCode)
    }

    /**
     * Создает PendingIntent для вызова оплаты через GooglePay из уведомления.
     * Результат оплаты будет обработан в SDK
     *
     * @param context         контекст для запуска экрана
     * @param googlePayParams параметры GooglePay
     * @param options         настройки платежной сессии
     * @param notificationId  ID уведомления.
     *                        Если передан, уведомлене удалится в случае успешной оплаты
     * @return настроенный PendingIntent
     */
    @JvmOverloads
    fun createGooglePayPendingIntent(context: Context,
                                     googlePayParams: GooglePayParams,
                                     options: PaymentOptions,
                                     notificationId: Int? = null): PendingIntent {
        options.setTerminalParams(terminalKey, publicKey)
        return NotificationPaymentActivity.createPendingIntent(context,
            options,
            null,
            NotificationPaymentActivity.PaymentMethod.GPAY,
            notificationId,
            googlePayParams)
    }

    /**
     * Создает PendingIntent для вызова оплаты через экран оплаты Tinkoff из уведомления.
     * Результат оплаты будет обработан в SDK
     *
     * @param context        контекст для запуска экрана
     * @param options        настройки платежной сессии
     * @param notificationId ID уведомления.
     *                       Если передан, уведомлене удалится в случае успешной оплаты
     * @return настроенный PendingIntent
     */
    @JvmOverloads
    fun createTinkoffPaymentPendingIntent(context: Context, options: PaymentOptions, notificationId: Int? = null): PendingIntent {
        options.setTerminalParams(terminalKey, publicKey)
        return NotificationPaymentActivity.createPendingIntent(context,
            options,
            null,
            NotificationPaymentActivity.PaymentMethod.TINKOFF,
            notificationId)
    }

    /**
     * Создает PendingIntent для вызова оплаты через GooglePay из уведомления.
     * Результат вернется в onActivityResult с кодом [requestCode] (успех, ошибка или отмена)
     *
     * @param activity        контекст для запуска экрана
     * @param googlePayParams параметры GooglePay
     * @param options         настройки платежной сессии
     * @param requestCode     код для получения результата, по завершению оплаты
     * @param notificationId  ID уведомления.
     *                        Если передан, уведомлене удалится в случае успешной оплаты
     * @return настроенный PendingIntent
     */
    @JvmOverloads
    fun createGooglePayPendingIntentForResult(activity: Activity,
                                              googlePayParams: GooglePayParams,
                                              options: PaymentOptions,
                                              requestCode: Int,
                                              notificationId: Int? = null): PendingIntent {
        options.setTerminalParams(terminalKey, publicKey)
        return NotificationPaymentActivity.createPendingIntent(activity,
                options,
                requestCode,
                NotificationPaymentActivity.PaymentMethod.GPAY,
                notificationId,
                googlePayParams)
    }

    /**
     * Создает PendingIntent для вызова оплаты через экран оплаты Tinkoff из уведомления
     *
     * @param activity       контекст для запуска экрана
     * @param options        настройки платежной сессии
     * @param requestCode    код для получения результата, по завершению оплаты
     * @param notificationId ID уведомления.
     *                       Если передан, уведомлене удалится в случае успешной оплаты
     * @return настроенный PendingIntent
     */
    @JvmOverloads
    fun createTinkoffPaymentPendingIntentForResult(activity: Activity,
                                                   options: PaymentOptions,
                                                   requestCode: Int,
                                                   notificationId: Int? = null): PendingIntent {
        options.setTerminalParams(terminalKey, publicKey)
        return NotificationPaymentActivity.createPendingIntent(activity,
            options,
            requestCode,
            NotificationPaymentActivity.PaymentMethod.TINKOFF,
            notificationId)
    }

    private fun prepareIntent(context: Context, options: BaseAcquiringOptions, cls: Class<*>): Intent {
        options.setTerminalParams(terminalKey, publicKey)
        return BaseAcquiringActivity.createIntent(context, options, cls)
    }

    fun attachCardOptions(setup: AttachCardOptions.() -> Unit) = AttachCardOptions().also { options ->
        options.setTerminalParams(terminalKey, publicKey)
        setup(options)
    }

    object AttachCardContract : ActivityResultContract<AttachCardOptions, String?>() {

        override fun createIntent(context: Context, input: AttachCardOptions): Intent =
            BaseAcquiringActivity.createIntent(context, input.apply {
                setTerminalParams(terminalKey, publicKey)
            }, AttachCardActivity::class.java)

        override fun parseResult(resultCode: Int, intent: Intent?): String? = when (resultCode) {
            AppCompatActivity.RESULT_OK -> intent?.getStringExtra(EXTRA_CARD_ID)!!
            else -> null
        }
    }

    companion object {

        const val RESULT_ERROR = 500
        const val EXTRA_ERROR = "extra_error"
        const val EXTRA_CARD_ID = "extra_card_id"
        const val EXTRA_PAYMENT_ID = "extra_payment_id"
        const val EXTRA_REBILL_ID = "extra_rebill_id"

        const val EXTRA_CARD_LIST_CHANGED = "extra_cards_changed"
    }
}