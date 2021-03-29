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
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.models.BrowserButtonClickedEvent
import ru.tinkoff.acquiring.sdk.models.ConfirmButtonClickedEvent
import ru.tinkoff.acquiring.sdk.viewmodel.BaseAcquiringViewModel

/**
 * @author Mariya Chernyadieva
 */
internal class BanksNotFoundFragment : BaseAcquiringFragment() {

    private lateinit var viewModel: BaseAcquiringViewModel

    private lateinit var descriptionTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var confirmButton: Button
    private lateinit var browserButton: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.acq_fragment_banks_not_found, container, false)

        viewModel = ViewModelProvider(requireActivity()).get(BaseAcquiringViewModel::class.java)

        descriptionTextView = view.findViewById(R.id.acq_banks_not_found_description)
        titleTextView = view.findViewById(R.id.acq_banks_not_found_title)
        confirmButton = view.findViewById(R.id.acq_confirm_button)
        browserButton = view.findViewById(R.id.acq_browser_button)

        confirmButton.setOnClickListener {
            viewModel.createEvent(ConfirmButtonClickedEvent)
        }

        browserButton.setOnClickListener {
            viewModel.createEvent(BrowserButtonClickedEvent)
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        descriptionTextView.text = localization.sbpWidgetAppsNotFoundDescription
        titleTextView.text = localization.sbpWidgetAppsNotFoundTitle
        confirmButton.text = localization.sbpWidgetAppsNotFoundButton
        browserButton.text = localization.sbpWidgetAppsNotFoundButtonBrowser
    }
}