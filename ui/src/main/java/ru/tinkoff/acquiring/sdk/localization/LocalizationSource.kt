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

import androidx.annotation.RawRes
import java.io.File
import java.io.Serializable

/**
 * Обозначает ресурс, из которого парсится локализация
 *
 * @author Mariya Chernyadieva
 */
sealed class LocalizationSource : Serializable

/**
 * Стандартные ресурсы Acquiring SDK
 */
class AsdkSource(val language: Language? = null) : LocalizationSource()

/**
 * Файловый ресурс
 */
class FileSource(val file: File) : LocalizationSource()

/**
 * Строковый ресурс формата json
 */
class StringSource(val stringJson: String) : LocalizationSource()

/**
 * Ресурс Android Raw Resources
 */
class RawSource(@RawRes val idRes: Int) : LocalizationSource()
