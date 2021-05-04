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

package ru.tinkoff.acquiring.sdk.adapters

import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import ru.tinkoff.acquiring.sdk.R

/**
 * @author Mariya Chernyadieva
 */
internal class SbpBanksListAdapter(private val context: Context,
                                   private var selectedPackageName: String) : BaseAdapter() {

    var bankSelectListener: BankSelectListener? = null

    private var bankPackageNames: List<String> = listOf()
    private val packageManager: PackageManager = context.packageManager

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view: View? = convertView
        val packageName = bankPackageNames[position]

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.acq_item_sbp_banks_list, parent, false)
        }

        val bankLogo = view!!.findViewById<ImageView>(R.id.acq_item_sbp_bank_logo)
        val bankName = view.findViewById<TextView>(R.id.acq_item_sbp_bank_number)
        val iconCheckmark = view.findViewById<ImageView>(R.id.acq_item_sbp_bank_checkmark)

        bankLogo.setImageDrawable(packageManager.getApplicationIcon(packageName))
        bankName.text = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))

        if (packageName == selectedPackageName) {
            iconCheckmark.visibility = View.VISIBLE
        } else {
            iconCheckmark.visibility = View.INVISIBLE
        }

        view.setOnClickListener {
            selectedPackageName = packageName
            notifyDataSetChanged()
            bankSelectListener?.onBankSelected(packageName)
        }

        return view
    }

    override fun getItem(position: Int): Any {
        return bankPackageNames[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return bankPackageNames.size
    }

    fun setBanks(banksList: List<String>) {
        bankPackageNames = banksList
        notifyDataSetChanged()
    }

    interface BankSelectListener {

        fun onBankSelected(bankPackageName: String)
    }
}