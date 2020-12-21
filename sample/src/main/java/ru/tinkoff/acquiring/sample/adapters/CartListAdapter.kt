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

package ru.tinkoff.acquiring.sample.adapters

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.models.Book
import ru.tinkoff.acquiring.sample.models.BooksRegistry
import ru.tinkoff.acquiring.sample.models.Cart

/**
 * @author Mariya Chernyadieva
 */
class CartListAdapter(
        context: Context,
        cartEntityList: List<Cart.CartEntry>,
        private val booksRegistry: BooksRegistry,
        private val listener: DeleteCartItemListener
) : ArrayAdapter<Cart.CartEntry>(context, R.layout.list_item_cart, cartEntityList) {

    private val countFormat: String = context.getString(R.string.cart_list_item_count_format)
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    interface DeleteCartItemListener {
        fun onDeleteItemPressed(cartEntry: Cart.CartEntry)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView

        if (view == null) {
            view = inflater.inflate(R.layout.list_item_cart, parent, false)
        }
        val holder = ViewHolder(view!!)
        val entry = getItem(position)
        val book = booksRegistry.getBook(context, entry?.bookId ?: 0)
        holder.fillWith(entry!!, book!!)
        return view
    }

    private inner class ViewHolder constructor(view: View) : View.OnClickListener {

        private var textViewPrice: TextView = view.findViewById(R.id.tv_book_price)
        private var textViewTitle: TextView = view.findViewById(R.id.tv_book_title)

        private lateinit var cartEntry: Cart.CartEntry

        init {
            val textViewDelete = view.findViewById<TextView>(R.id.tv_delete)
            textViewDelete.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            listener.onDeleteItemPressed(cartEntry)
        }

        fun fillWith(cartEntry: Cart.CartEntry, book: Book) {
            this.cartEntry = cartEntry
            textViewTitle.text = book.title
            val count = cartEntry.count
            if (count > 1) {
                val countPart = String.format(countFormat, count)
                val pricePart = book.shoppingTitle
                val label = SpannableStringBuilder(countPart)
                label.run {
                    setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.common_gray)),
                            0,
                            countPart.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    append(pricePart)
                }
                textViewPrice.text = label
            } else {
                textViewPrice.text = book.shoppingTitle
            }
        }
    }
}
