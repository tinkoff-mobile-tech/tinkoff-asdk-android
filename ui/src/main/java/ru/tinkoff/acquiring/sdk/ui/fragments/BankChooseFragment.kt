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
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.adapters.SbpBanksListAdapter
import ru.tinkoff.acquiring.sdk.models.OpenBankClickedEvent
import ru.tinkoff.acquiring.sdk.viewmodel.BaseAcquiringViewModel

/**
 * @author Mariya Chernyadieva
 */
internal class BankChooseFragment : BaseAcquiringFragment(), SbpBanksListAdapter.BankSelectListener {

    private lateinit var sbpBanksAdapter: SbpBanksListAdapter
    private lateinit var viewModel: BaseAcquiringViewModel

    private lateinit var descriptionTextView: TextView
    private lateinit var continueButton: Button
    private lateinit var titleTextView: TextView
    private lateinit var banksListView: ListView
    private lateinit var content: View

    private var selectedPackageName = TINKOFF_PACKAGE_NAME

    companion object {

        private const val TINKOFF_PACKAGE_NAME = "com.idamob.tinkoff.android"
        private const val BANKS_LIST = "banks_list"

        fun newInstance(banks: ArrayList<String>): Fragment {
            val args = Bundle()
            args.putStringArrayList(BANKS_LIST, banks)

            val fragment = BankChooseFragment()
            fragment.arguments = args

            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.acq_fragment_bank_choose, container, false)

        descriptionTextView = view.findViewById(R.id.acq_sbp_banks_tv_description)
        continueButton = view.findViewById(R.id.acq_sbp_banks_btn_continue)
        titleTextView = view.findViewById(R.id.acq_sbp_banks_tv_title)
        banksListView = view.findViewById(R.id.acq_sbp_banks_list)
        content = view.findViewById(R.id.acq_content)

        sbpBanksAdapter = SbpBanksListAdapter(requireContext(), TINKOFF_PACKAGE_NAME).apply {
            bankSelectListener = this@BankChooseFragment
        }

        continueButton.setOnClickListener {
            openBank(selectedPackageName)
        }
        return view
    }

    private fun openBank(packageName: String) {
        viewModel.createEvent(OpenBankClickedEvent(packageName))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(BaseAcquiringViewModel::class.java)

        titleTextView.text = localization.sbpWidgetTitle
        descriptionTextView.text = localization.sbpWidgetDescription
        continueButton.text = localization.sbpWidgetButton


        val banks = requireArguments().getStringArrayList(BANKS_LIST)?.toMutableList() ?: mutableListOf()
        val tBank = banks.find { it == TINKOFF_PACKAGE_NAME }
        if (tBank != null && banks.indexOf(tBank) != 0) {
            banks.remove(tBank)
            banks.add(0, tBank)
        }
        sbpBanksAdapter.setBanks(banks)
        banksListView.adapter = sbpBanksAdapter

        if (banks.find { it == TINKOFF_PACKAGE_NAME } == null) {
            continueButton.isEnabled = false
        }

        observeLiveData()
    }

    private fun observeLiveData() {

    }

    override fun onBankSelected(bankPackageName: String) {
        selectedPackageName = bankPackageName
        continueButton.isEnabled = true
    }
}