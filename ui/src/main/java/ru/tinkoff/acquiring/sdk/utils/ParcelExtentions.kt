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

package ru.tinkoff.acquiring.sdk.utils

import android.os.Parcel

/**
 * @author Mariya Chernyadieva
 */
internal fun <T> Parcel.readParcelList(clazz: Class<*>): List<T>? {
    var resultList: List<T>? = null
    val outList: List<T> = mutableListOf()

    readList(outList, clazz.classLoader)
    if (outList.isNotEmpty()) {
        resultList = mutableListOf()
        resultList.addAll(outList)
    }
    return resultList
}

internal fun <K, V> Parcel.readParcelMap(clazz: Class<*>): Map<K, V>? {
    var resultMap: Map<K, V>? = null
    val outMap: Map<K, V> = mutableMapOf()

    readMap(outMap, clazz.classLoader)
    if (outMap.isNotEmpty()) {
        resultMap = mutableMapOf()
        resultMap.putAll(outMap)
    }
    return resultMap
}