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

import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

/**
 * @author Mariya Chernyadieva, Taras Nagorny
 */
internal object CryptoUtils {

    fun encryptRsa(string: String, publicKey: PublicKey): ByteArray {
        try {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            return cipher.doFinal(string.toByteArray())
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: BadPaddingException) {
            throw RuntimeException(e)
        } catch (e: IllegalBlockSizeException) {
            throw RuntimeException(e)
        }
    }

    fun encodeBase64(value: ByteArray): String {
        return Base64.encodeToString(value, Base64.DEFAULT).trim()
    }

    fun String.sha256(): String {
        return hashString(this)
    }

    private fun hashString(input: String): String {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}
