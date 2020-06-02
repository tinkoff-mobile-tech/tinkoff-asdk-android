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

package ru.tinkoff.acquiring.sdk.ui.customview.editcard.editable

import android.text.Editable
import android.text.InputFilter
import android.text.Selection
import android.text.TextWatcher
import java.util.*

/**
 * @author Mariya Chernyadieva
 */
internal open class EditCardEditable : Editable {

    override var length: Int = 0
    open var text: CharSequence = ""

    private val textWatchers = mutableSetOf<TextWatcher>()
    private var filters: Array<InputFilter>? = null

    private var spanStart = 0
    private var spanEnd = 0

    override fun setSpan(what: Any, start: Int, end: Int, flags: Int) {
        if (start < 0 || end < 0) {
            return
        }

        when (what) {
            Selection.SELECTION_START -> spanStart = start
            Selection.SELECTION_END -> spanEnd = end
        }
    }

    override fun insert(where: Int, text: CharSequence, start: Int, end: Int): Editable {
        replace(where, where, text, start, end)
        return this
    }

    override fun insert(where: Int, text: CharSequence): Editable {
        replace(where, where, text, 0, text.length)
        return this
    }

    override fun delete(st: Int, en: Int): Editable {
        if (length == 0) {
            return this
        }
        val start = if (st < 0) 0 else st
        return replace(start, en, "", start, en)
    }

    override fun clear() {
        replace(0, text.length, "", 0, 0)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> getSpans(start: Int, end: Int, type: Class<T>?): Array<T> {
        if (type == null)
            return arrayOf<Any>() as Array<T>
        val arrayList = ArrayList<T>()
        val ts = java.lang.reflect.Array.newInstance(type, arrayList.size) as Array<T>
        return arrayList.toArray(ts)
    }

    override fun removeSpan(what: Any?) {
        when (what) {
            Selection.SELECTION_START -> spanStart = 0
            Selection.SELECTION_END -> spanEnd = 0
        }
    }

    override fun getSpanStart(tag: Any?): Int {
        return when (tag) {
            Selection.SELECTION_START -> spanStart
            Selection.SELECTION_END -> spanEnd
            else -> -1
        }
    }

    override fun getSpanEnd(tag: Any?): Int {
        return when (tag) {
            Selection.SELECTION_START -> spanStart
            Selection.SELECTION_END -> spanEnd
            else -> -1
        }
    }

    override fun clearSpans() {
        spanStart = 0
        spanEnd = 0
    }

    override fun getSpanFlags(tag: Any?): Int {
        return 0
    }

    override fun nextSpanTransition(start: Int, limit: Int, type: Class<*>?): Int {
        return 0
    }

    override fun append(text: CharSequence?): Editable {
        replace(length, length, text ?: "", 0, text?.length ?: 0)
        return this
    }

    override fun append(newText: CharSequence?, start: Int, end: Int): Editable {
        replace(length, length, newText ?: "", start, end)
        return this
    }

    override fun append(text: Char): Editable {
        append(text.toString())
        return this
    }

    override fun replace(st: Int, en: Int, tb: CharSequence, tbst: Int, tben: Int): Editable {
        if (st < 0 || en < 0) {
            return this
        }

        var filtered: CharSequence? = null
        if (filters != null && tb.isNotEmpty()) {
            for (inputFilter in filters as Array<InputFilter>) {
                filtered = inputFilter.filter(tb, st, en, this, st, en)
                if (filtered != null) {
                    return this
                }
            }
        }

        sendTextBeforeChanged(st, length, en)
        text = text.toString().replaceRange(st, en, tb)

        var newPosition = st
        when {
            st == en && filtered == null -> newPosition += 1
            st != en && tb.isNotEmpty() -> newPosition = text.length
        }

        length = text.length
        sendTextChanged(newPosition, st, tb.length)
        sendAfterTextChanged()

        return this
    }

    override fun replace(st: Int, en: Int, text: CharSequence): Editable {
        return replace(st, en, text, 0, text.length)
    }

    override fun getChars(start: Int, end: Int, dest: CharArray?, destoff: Int) {
        var destoffIndex = destoff
        for (i in start until end) {
            dest?.set(destoffIndex++, text[i])
        }
    }

    override fun get(index: Int): Char {
        return text[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return text.subSequence(startIndex, endIndex)
    }

    override fun setFilters(filters: Array<InputFilter>?) {
        this.filters = filters
    }

    override fun getFilters(): Array<InputFilter>? {
        return filters
    }

    fun setTextWatcher(textWatcher: TextWatcher) {
        textWatchers.add(textWatcher)
    }

    fun updateText(text: CharSequence) {
        this.text = text
        sendTextBeforeChanged(text.length, text.length, 0)
        sendTextChanged(text.length, 0, text.length)
        sendAfterTextChanged()
    }

    private fun sendTextBeforeChanged(start: Int, count: Int, after: Int) {
        for (textWatcher in textWatchers)
            textWatcher.beforeTextChanged(text, start, count, after)
    }

    private fun sendTextChanged(start: Int, before: Int, count: Int) {
        for (textWatcher in textWatchers)
            textWatcher.onTextChanged(text, start, before, count)
    }

    private fun sendAfterTextChanged() {
        for (textWatcher in textWatchers)
            textWatcher.afterTextChanged(this)
    }
}