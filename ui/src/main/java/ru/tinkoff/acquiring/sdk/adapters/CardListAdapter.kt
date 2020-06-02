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
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.ui.customview.Shadow
import ru.tinkoff.acquiring.sdk.utils.CardSystemIconsHolder

/**
 * @author Mariya Chernyadieva
 */
internal class CardListAdapter(private val context: Context): BaseAdapter() {

    var moreClickListener: OnMoreIconClickListener? = null

    private var cards = mutableListOf<Card>()
    private val iconsHolder: CardSystemIconsHolder = CardSystemIconsHolder(context)
    private var isDarkMode = false

    init {
        isDarkMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view: View? = convertView

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.acq_item_card_list, parent, false)
            view!!.findViewById<View>(R.id.acq_item_card_background).background = Shadow(context, isDarkMode)
        }

        val cardImage = view.findViewById<ImageView>(R.id.acq_item_card_logo)
        val cardNumber = view.findViewById<TextView>(R.id.acq_item_card_number)
        val cardDate = view.findViewById<TextView>(R.id.acq_item_card_date)

        cardImage.setImageBitmap(iconsHolder.getCardSystemLogo(cards[position].pan!!))
        cardNumber.text = makeTextNumber(cards[position].pan!!)
        cardDate.text = makeCardDate(cards[position].expDate!!)
        val iconMore = view.findViewById<ImageView>(R.id.acq_item_card_more)
        iconMore.setOnClickListener {
            moreClickListener?.onMoreIconClick(cards[position])
        }
        return view
    }

    override fun getItem(position: Int): Any {
        return cards[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return cards.size
    }

    fun setCards(cards: List<Card>) {
        this.cards = cards.toMutableList()
        notifyDataSetChanged()
    }

    fun getLastPanNumbers(number: String): String {
        return number.substring(number.length - 4, number.length)
    }

    private fun makeTextNumber(number: String): String {
        return String.format(AsdkLocalization.resources.cardListCardFormat!!, getLastPanNumbers(number))
    }

    private fun makeCardDate(date: String): String {
        return "${date.subSequence(0, 2)}/${date.subSequence(2, date.length)}"
    }

    interface OnMoreIconClickListener {

        fun onMoreIconClick(card: Card)
    }
}