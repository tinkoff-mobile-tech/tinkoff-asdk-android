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

import java.math.BigInteger
import java.net.Inet6Address
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Mariya Chernyadieva
 */
internal fun getIpAddress(): String {
    try {
        NetworkInterface.getNetworkInterfaces().iterator().forEach { network ->
            network.inetAddresses.iterator().forEach { address ->
                if (!address.isLoopbackAddress) {
                    return if (address is Inet6Address) {
                        address.formatIpv6Address()
                    } else {
                        address.hostAddress
                    }
                }
            }
        }
    } catch (e: Throwable) {
        // ignore
    }
    return ""
}

internal fun getTimeZoneOffsetInMinutes(): String {
    val offsetMills = TimeZone.getDefault().rawOffset
    return "${TimeUnit.MILLISECONDS.toMinutes(offsetMills.toLong())}"
}

internal fun Inet6Address.formatIpv6Address(): String {
    val list = BigInteger(1, this.address).toString(16).chunked(4)
    var result = ""
    list.forEachIndexed { index, s ->
        result += s
        if (index != list.lastIndex) {
            result += ":"
        }
    }
    return result
}

