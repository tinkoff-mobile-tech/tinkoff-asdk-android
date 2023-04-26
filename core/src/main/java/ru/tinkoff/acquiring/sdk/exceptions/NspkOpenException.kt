package ru.tinkoff.acquiring.sdk.exceptions

/**
 *
 * Исключение, выбрасываемое при неуспешном открытии какого либо банка по контракту NSPK в
 * Cценарии оплаты по СБП
 *
 * @param throwable  - родительская ошибка
 * @param message    - дополнительное сообщение
 * @param deeplink   - диплинк для октрытия прилоежния
 * @param paymentId  - идентификатор платежной сессии
 *
 * Created by i.golovachev
 */
class NspkOpenException(
    throwable: Throwable,
    message: String? = null,
    val deeplink: String? = null,
    val paymentId: Long? = null
) : RuntimeException(message ?: throwable.message, throwable)