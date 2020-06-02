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

package ru.tinkoff.acquiring.sdk.ui.customview.editcard

import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.text.Selection
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.*
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.editable.EditCardEditable
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.keyboard.SecureKeyboard

/**
 * @author Mariya Chernyadieva
 */
internal class EditCardInputConnection(private val view: View) : InputConnection, SecureKeyboard.OnKeyClickListener {

    var editable: EditCardEditable
        private set

    init {
        editable = EditCardEditable()
        editable.setTextWatcher(view as TextWatcher)
    }

    fun setCurrentEditable(editable: EditCardEditable) {
        this.editable = editable
        this.editable.setTextWatcher(view as TextWatcher)
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        val str = text.toString()
        if (str.toIntOrNull() != null) {
            val start = Selection.getSelectionStart(editable)
            val end = Selection.getSelectionEnd(editable)

            editable.replace(start, end, str)
        }

        return true
    }

    override fun closeConnection() = Unit

    override fun commitCompletion(text: CompletionInfo?): Boolean {
        return true
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        return true
    }

    override fun performContextMenuAction(id: Int): Boolean {
        return false
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        var st = start
        var en = end

        val len = editable.length
        if (st > len || en > len || st < 0 || en < 0) {
            st = len
            en = len
        }

        Selection.setSelection(editable, st, en)
        return true
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        return true
    }

    override fun getHandler(): Handler? {
        return null
    }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
        return false
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
        return null
    }

    override fun beginBatchEdit(): Boolean {
        return true
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        return true
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        return true
    }

    override fun endBatchEdit(): Boolean {
        return true
    }

    override fun getSelectedText(flags: Int): CharSequence {
        val st = Selection.getSelectionStart(editable)
        val en = Selection.getSelectionEnd(editable)

        return if (flags and InputConnection.GET_TEXT_WITH_STYLES != 0) {
            editable.subSequence(st, en)
        } else ""
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        return enabled
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        val start = Selection.getSelectionStart(editable)
        val end = Selection.getSelectionEnd(editable)
        editable.delete(start - 1, end)

        return true
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        return 0
    }

    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence {
        var before = n
        if (editable.length < before) {
            before = editable.length
        } else if (n < 0) {
            return ""
        }

        return editable.substring(0, before)
    }

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence {
        if (editable.length < n || n < 0) {
            return ""
        }

        return editable.substring(n, editable.length)
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
        return true
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                val start = Selection.getSelectionStart(editable)
                val end = Selection.getSelectionEnd(editable)

                when (event.keyCode) {
                    KeyEvent.KEYCODE_DEL -> {
                        editable.delete(start - 1, end)
                    }
                    KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3,
                    KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7,
                    KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9 -> {
                        editable.replace(start, end, event.number.toString())
                    }
                    KeyEvent.KEYCODE_ENTER -> view.onKeyDown(event.keyCode, event)
                }
            }
            KeyEvent.ACTION_UP -> view.onKeyUp(event.keyCode, event)
        }
        return true
    }

    override fun finishComposingText(): Boolean {
        return true
    }

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean {
        return true
    }

    override fun commitContent(inputContentInfo: InputContentInfo, flags: Int, opts: Bundle?): Boolean {
        return true
    }

    override fun performEditorAction(editorAction: Int): Boolean {
        val eventTime = SystemClock.uptimeMillis()
        sendKeyEvent(KeyEvent(
                eventTime,
                eventTime,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ENTER,
                0,
                0,
                -1,
                0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE or KeyEvent.FLAG_EDITOR_ACTION)
        )
        sendKeyEvent(KeyEvent(
                SystemClock.uptimeMillis(),
                eventTime,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_ENTER,
                0,
                0,
                -1,
                0,
                (KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE or KeyEvent.FLAG_EDITOR_ACTION))
        )
        return true
    }

    override fun onKeyClick(keyCode: Int) {
        val start = Selection.getSelectionStart(editable)
        val end = Selection.getSelectionEnd(editable)

        if (keyCode <= 9) {
            editable.replace(start, end, keyCode.toString())
        } else {
            editable.delete(start - 1, end)
        }
        view.onKeyUp(keyCode, null)
    }
}