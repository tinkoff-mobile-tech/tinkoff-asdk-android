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

package ru.tinkoff.acquiring.sdk.localization

import android.content.Context
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.localization.parsers.FileLocalizationParser
import ru.tinkoff.acquiring.sdk.localization.parsers.RawLocalizationParser
import ru.tinkoff.acquiring.sdk.localization.parsers.StringLocalizationParser
import java.util.*

/**
 * @author Mariya Chernyadieva
 */
@Deprecated("Удалить в 3.1.0. Для кастомизации локализации необходимо использовать замену строковых ресурсов")
internal object AsdkLocalization {

    lateinit var resources: LocalizationResources
    var language: Language = Language.RU

    @Throws(LocalizationParseException::class)
    fun init(context: Context, source: LocalizationSource) {
        val parser = when (source) {
            is FileSource -> FileLocalizationParser(source.file)
            is RawSource -> RawLocalizationParser(context, source.idRes)
            is StringSource -> StringLocalizationParser(source.stringJson)
            is AsdkSource -> {
                language = resolveLanguage(source.language)
                when (language) {
                    Language.RU -> RawLocalizationParser(context, R.raw.acq_localization_ru)
                    Language.EN -> RawLocalizationParser(context, R.raw.acq_localization_en)
                }
            }
        }
        resources = parser.parse()
    }

    fun isInitialized() = this::resources.isInitialized

    private fun resolveLanguage(language: Language?): Language {
        return language ?: when (Locale.getDefault().language) {
            "ru", "ua", "kz", "by", "az", "os", "hy", "tg", "tk" -> Language.RU //страны СНГ
            else -> Language.EN
        }
    }
}
