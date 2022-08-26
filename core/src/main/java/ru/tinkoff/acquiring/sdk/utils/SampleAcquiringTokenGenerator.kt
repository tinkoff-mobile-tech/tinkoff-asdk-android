package ru.tinkoff.acquiring.sdk.utils

import ru.tinkoff.acquiring.sdk.AcquiringTokenGenerator
import ru.tinkoff.acquiring.sdk.requests.AcquiringRequest
import java.lang.StringBuilder

/**
 * Пример реализации алгоритма генерации токена. Использование этой реализации в реальных
 * Android-приложениях не рекомендуется.
 *
 * @param password пароль терминала, который будет использоваться при формировании токена.
 * **В целях безопасности пароль не рекомендуется хранить в коде Android-приложения.**
 */
class SampleAcquiringTokenGenerator(private val password: String) : AcquiringTokenGenerator {

    override fun generateToken(request: AcquiringRequest<*>, params: MutableMap<String, Any>): String? {
        params[AcquiringRequest.PASSWORD] = password
        val sorted = params.toSortedMap()

        val token = StringBuilder()
        sorted.values.forEach { token.append(it) }
        return AcquiringTokenGenerator.sha256hashString(token.toString())
    }
}