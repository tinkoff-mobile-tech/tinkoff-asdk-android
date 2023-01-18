package ru.tinkoff.acquiring.yandexpay

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import com.yandex.pay.core.OpenYandexPayContract
import com.yandex.pay.core.YandexPayResult
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.YandexPaymentProcess
import ru.tinkoff.acquiring.yandexpay.models.YandexPayData

/**
 * Created by i.golovachev
 */
internal typealias AcqYandexPayCallback = (AcqYandexPayResult) -> Unit
typealias AcqYandexPayErrorCallback = (Throwable) -> Unit
typealias AcqYandexPayCancelCallback = () -> Unit
typealias AcqYandexPaySuccessCallback = (AcqYandexPayResult.Success) -> Unit


/**
 * Создает обертку для элемента yandex-pay-button для выбора средства оплаты
 *
 * @param activity                контекст для дальнешей навигации платежного флоу из Activity
 * @param yandexPayData           параметры, для настройки yandex-pay библиотеки, полученные от бэка
 * @param options                 настройки платежной сессии
 * @param yandexPayRequestCode    код для получения результата, по завершению работы экрана Acquiring SDK
 * @param isProd                  выбор окружения для яндекса YandexPayEnvironment.Prod или YandexPayEnvironment.Sandbox
 * @param enableLogging           включение логгирования событий YandexPay
 * @param themeId                 идентификатор темы приложения, параметры которого будет использованы для
 *                                отображение yandex-pay-button
 * @param onYandexErrorCallback   дополнительный метод для возможности обработки ошибки от яндекса на
 *                                стороне клиентского приложения
 * @param onYandexCancelCallback  дополнительный метод для возможности обработки отмены
 * @param onYandexSuccessCallback дополнительный метод для возможности обработки успеха оплаты
 *                                рекомендует переопределять только в случае, если запрос инициализации платежа
 *                                реализован на вашем бэкенде.
 */
fun TinkoffAcquiring.createYandexPayButtonFragment(
    activity: FragmentActivity,
    yandexPayData: YandexPayData,
    options: PaymentOptions,
    yandexPayRequestCode: Int,
    isProd: Boolean = false,
    enableLogging: Boolean = false,
    themeId: Int? = null,
    onYandexErrorCallback: AcqYandexPayErrorCallback? = null,
    onYandexCancelCallback: AcqYandexPayCancelCallback? = null,
    onYandexSuccessCallback: AcqYandexPaySuccessCallback? =  {
        openYandexPaymentScreen(
            activity,
            it.paymentOptions,
            yandexPayRequestCode,
            it.token
        )
    }
): YandexButtonFragment {
    YandexPaymentProcess.init(sdk, activity.application)
    val fragment = YandexButtonFragment.newInstance(yandexPayData, options,  isProd, enableLogging, themeId)
    addYandexResultListener(fragment, activity, yandexPayRequestCode, onYandexErrorCallback, onYandexCancelCallback, onYandexSuccessCallback)
    return fragment
}

/**
 * Создает слушатель, который обрабатывает результат флоу yandex-pay
 *
 * @param activity                контекст для дальнешей навигации платежного флоу из Activity
 * @param fragment                экземляр фрагмента - обертки над яндексом
 * @param yandexPayRequestCode    код для получения результата, по завершению работы экрана Acquiring SDK
 * @param onYandexErrorCallback   дополнительный метод для возможности обработки ошибки от яндекса на
 *                                стороне клиентского приложения
 * @param onYandexCancelCallback  дополнительный метод для возможности обработки отмены
 * @param onYandexSuccessCallback дополнительный метод для возможности обработки успеха оплаты
 *                                рекомендует переопределять только в случае, если запрос инициализации платежа
 *                                реализован на вашем бэкенде.
 */
fun TinkoffAcquiring.addYandexResultListener(
    fragment: YandexButtonFragment,
    activity: FragmentActivity,
    yandexPayRequestCode: Int,
    onYandexErrorCallback: AcqYandexPayErrorCallback? = null,
    onYandexCancelCallback: AcqYandexPayCancelCallback? = null,
    onYandexSuccessCallback: AcqYandexPaySuccessCallback? =  {
        openYandexPaymentScreen(
            activity,
            it.paymentOptions,
            yandexPayRequestCode,
            it.token
        )
    }
) {
    fragment.listener = {
        when (it) {
            is AcqYandexPayResult.Success -> onYandexSuccessCallback?.invoke(it)
            is AcqYandexPayResult.Error -> onYandexErrorCallback?.invoke(it.throwable)
            else -> onYandexCancelCallback?.invoke()
        }
    }
}

fun TinkoffAcquiring.openYandexPaymentScreen(
    activity: FragmentActivity,
    requestCode: Int,
    success: AcqYandexPayResult.Success,
    paymentId: Long? = null
) = openYandexPaymentScreen(activity, success.paymentOptions, requestCode, success.token, paymentId)

/**
 * Запуск экрана Acquiring SDK для проведения оплаты
 *
 * @param activity        контекст для запуска экрана из Activity
 * @param requestCode     код для получения результата, по завершению работы экрана Acquiring SDK
 * @param successResult   данные, полученные от фрагмента с yandex pay(подразумевается только успешный результат)
 */
fun TinkoffAcquiring.openYandexPaymentScreen(
    activity: Activity,
    requestCode: Int,
    successResult: AcqYandexPayResult.Success,
    paymentId: Long? = null
) {
    openYandexPaymentScreen(activity, successResult.paymentOptions, requestCode, successResult.token, paymentId)
}