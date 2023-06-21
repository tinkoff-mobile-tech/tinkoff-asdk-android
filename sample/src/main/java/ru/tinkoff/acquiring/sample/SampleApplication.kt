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
import android.content.Context
import ru.tinkoff.acquiring.sample.utils.SessionParams
import ru.tinkoff.acquiring.sample.utils.SettingsSdkManager
import ru.tinkoff.acquiring.sample.utils.TerminalsManager
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.utils.SampleAcquiringTokenGenerator

/**
 * @author Mariya Chernyadieva
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initSdk(this, TerminalsManager.init(this).selectedTerminal)
        AcquiringSdk.isDeveloperMode = true
        AcquiringSdk.isDebug = true
        AcquiringSdk.customUrl = SettingsSdkManager(this).customUrl
    }

    companion object {
        lateinit var tinkoffAcquiring: TinkoffAcquiring
            private set

        fun initSdk(context: Context, params: SessionParams) {
            tinkoffAcquiring = TinkoffAcquiring(
                context.applicationContext,
                params.terminalKey,
                params.publicKey
            )
            AcquiringSdk.tokenGenerator = params.password?.let { SampleAcquiringTokenGenerator(it) }
        }
    }
}
