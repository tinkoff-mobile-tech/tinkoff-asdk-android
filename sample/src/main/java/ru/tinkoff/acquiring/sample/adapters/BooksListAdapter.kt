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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.models.Book
import java.util.*

/**
 * @author Mariya Chernyadieva
 */
class BooksListAdapter(
        context: Context,
        bookList: ArrayList<Book>,
        private val listener: BookDetailsClickListener?
) : ArrayAdapter<Book>(context, R.layout.list_item_book, bookList) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    interface BookDetailsClickListener {
        fun onBookDetailsClicked(book: Book)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView

        if (view == null) {
            view = inflater.inflate(R.layout.list_item_book, parent, false)
        }
        val viewHolder = ViewHolder(view!!)
        viewHolder.fillWith(getItem(position)!!)

        return view
    }

    private inner class ViewHolder internal constructor(view: View) : View.OnClickListener {

        private val textViewAnnotation: TextView = view.findViewById(R.id.tv_book_annotation)
        private val imageViewCover: ImageView = view.findViewById(R.id.iv_book_cover)
        private val textViewAuthor: TextView = view.findViewById(R.id.tv_book_author_year)
        private val textViewTitle: TextView = view.findViewById(R.id.tv_book_title)
        private val textViewPrice: TextView = view.findViewById(R.id.tv_book_price)

        private lateinit var book: Book

        init {
            val textViewDetails = view.findViewById<TextView>(R.id.tv_book_details)
            textViewDetails.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            listener?.onBookDetailsClicked(book)
        }

        fun fillWith(book: Book) {
            this.book = book
            imageViewCover.setImageResource(book.coverDrawableId)
            textViewTitle.text = book.title
            val priceText = context.getString(R.string.book_price, book.price)
            textViewPrice.text = priceText
            textViewAuthor.text = book.shoppingTitle
            textViewAnnotation.text = book.annotation
        }
    }
}
