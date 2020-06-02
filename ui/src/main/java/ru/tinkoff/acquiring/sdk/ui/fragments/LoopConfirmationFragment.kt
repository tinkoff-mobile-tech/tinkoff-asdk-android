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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.models.ErrorScreenState
import ru.tinkoff.acquiring.sdk.models.ScreenState
import ru.tinkoff.acquiring.sdk.utils.Money
import ru.tinkoff.acquiring.sdk.utils.MoneyUtils
import ru.tinkoff.acquiring.sdk.viewmodel.AttachCardViewModel
import java.math.BigDecimal

/**
 * @author Mariya Chernyadieva
 */
internal class LoopConfirmationFragment : BaseAcquiringFragment() {

    private lateinit var attachCardViewModel: AttachCardViewModel
    private lateinit var requestKey: String

    private lateinit var amountEditText: EditText
    private lateinit var titleTextView: TextView
    private lateinit var checkButton: Button

    companion object {
        private const val REQUEST_KEY = "request_key"

        fun newInstance(requestKey: String): Fragment {
            val args = Bundle()
            args.putString(REQUEST_KEY, requestKey)

            val fragment = LoopConfirmationFragment()
            fragment.arguments = args

            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.acq_fragment_loop_confirmation, container, false)

        titleTextView = view.findViewById(R.id.acq_loop_tv_title)
        amountEditText = view.findViewById(R.id.acq_loop_et_amount)

        val moneyWatcher = MoneyUtils.MoneyWatcher()
        moneyWatcher.setLengthLimit(3)
        amountEditText.addTextChangedListener(moneyWatcher)

        checkButton = view.findViewById(R.id.acq_loop_btn_check)
        checkButton.setOnClickListener { checkAmount(amountEditText.text.toString()) }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        requireActivity().run {
            attachCardViewModel = ViewModelProvider(this).get(AttachCardViewModel::class.java)
            observeLiveData()

            arguments?.let {
                requestKey = it.getString(REQUEST_KEY)!!
            }
        }

        checkButton.text = localization.confirmationLoopCheckButton
        amountEditText.hint = localization.confirmationLoopAmount
        titleTextView.text = localization.confirmationLoopDescription
    }

    private fun observeLiveData() {
        with(attachCardViewModel) {
            screenStateLiveData.observe(viewLifecycleOwner, Observer { handleScreenState(it) })
        }
    }

    private fun handleScreenState(screenState: ScreenState) {
        when (screenState) {
            is ErrorScreenState -> {
                Toast.makeText(
                        activity,
                        localization.confirmationLoopDialogValidationInvalidAmount,
                        Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkAmount(enteredAmount: String) {
        val value = MoneyUtils.normalize(enteredAmount)
        val amount = Money.ofRubles(BigDecimal(value)).coins
        attachCardViewModel.submitRandomAmount(requestKey, amount)
    }
}