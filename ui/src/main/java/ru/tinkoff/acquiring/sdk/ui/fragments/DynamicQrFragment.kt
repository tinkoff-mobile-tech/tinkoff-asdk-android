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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.ui.activities.BaseAcquiringActivity

/**
 * @author Mariya Chernyadieva
 */
internal class DynamicQrFragment : BaseQrCodeFragment() {

    private lateinit var paymentOptions: PaymentOptions
    private lateinit var amountLabel: TextView
    private lateinit var amountText: TextView
    private lateinit var orderTitle: TextView
    private lateinit var orderDescription: TextView

    override fun onShareButtonClick() {
        viewModel.getDynamicQrLink(paymentOptions)
    }

    override fun loadQr() {
        viewModel.getDynamicQr(paymentOptions)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(R.layout.acq_fragment_dynamic_qr, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        amountLabel = view.findViewById(R.id.acq_qr_tv_amount_label)
        amountLabel.text = AsdkLocalization.resources.payTitle

        amountText = view.findViewById(R.id.acq_qr_tv_amount)
        orderTitle = view.findViewById(R.id.acq_qr_tv_order_title)
        orderDescription = view.findViewById(R.id.acq_qr_tv_order_description)

        requireActivity().intent.extras?.let { extras ->
            paymentOptions = extras.getParcelable(BaseAcquiringActivity.EXTRA_OPTIONS)!!

            paymentOptions.order.run {
                amountText.text = modifySpan(amount.toHumanReadableString())
                orderTitle.visibility = if (title.isNullOrBlank()) View.GONE else View.VISIBLE
                orderDescription.visibility = if (description.isNullOrBlank()) View.GONE else View.VISIBLE
                orderTitle.text = title
                orderDescription.text = description
                orderDescription.resolveScroll()
            }
        }
    }
}