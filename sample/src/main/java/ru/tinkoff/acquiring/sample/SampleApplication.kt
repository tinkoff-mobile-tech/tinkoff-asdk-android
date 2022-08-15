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

package ru.tinkoff.acquiring.sample

import android.app.Application
import ru.tinkoff.acquiring.sample.utils.SessionParams
import ru.tinkoff.acquiring.sample.utils.SettingsSdkManager
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.payment.PaymentProcess

/**
 * @author Mariya Chernyadieva
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val settings = SettingsSdkManager(this)
        val params = SessionParams[settings.terminalKey]
        tinkoffAcquiring = TinkoffAcquiring(this, params.terminalKey, params.publicKey)
        AcquiringSdk.isDeveloperMode = true
        AcquiringSdk.isDebug = true
    }

    override fun onTerminate() {
        super.onTerminate()
        paymentProcess?.stop()
    }

    companion object {
        lateinit var tinkoffAcquiring: TinkoffAcquiring
            private set
        var paymentProcess: PaymentProcess? = null
    }
}