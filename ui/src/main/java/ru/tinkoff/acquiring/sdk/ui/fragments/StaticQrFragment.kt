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

package ru.tinkoff.acquiring.sdk.ui.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.tinkoff.acquiring.sdk.R

/**
 * @author Mariya Chernyadieva
 */
internal class StaticQrFragment : BaseQrCodeFragment() {

    override fun onShareButtonClick() {
        viewModel.getStaticQrLink()
    }

    override fun loadQr() {
        viewModel.getStaticQr()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(R.layout.acq_fragment_static_qr, container, false)
    }
}