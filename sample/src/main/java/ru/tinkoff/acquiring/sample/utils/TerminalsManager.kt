package ru.tinkoff.acquiring.sample.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.tinkoff.acquiring.sample.SampleApplication

@Suppress("ObjectPropertyName", "StaticFieldLeak")
object TerminalsManager {

    private const val PREFS_NAME = "terminals"
    private const val TERMINALS_KEY = "terminals"
    private const val SELECTED_TERMINAL_KEY = "selected_terminal"

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences

    private var _terminals: List<SessionParams>? = null
    var terminals: List<SessionParams>
        get() = _terminals!!
        set(value) {
            _terminals = when {
                value.find { it.terminalKey == SessionParams.TEST_SDK.terminalKey } != null -> value
                else -> value.toMutableList().apply { add(0, SessionParams.TEST_SDK) }
            }
            prefs.edit().putString(TERMINALS_KEY, gson.toJson(_terminals)).commit()

            if (terminals.find { it.terminalKey == _selectedTerminalKey } == null) {
                selectedTerminalKey = terminals.first().terminalKey
            }
        }

    private var _selectedTerminalKey: String? = null
    var selectedTerminalKey: String
        get() = _selectedTerminalKey!!
        set(value) {
            _selectedTerminalKey = value
            prefs.edit().putString(SELECTED_TERMINAL_KEY, value).commit()
            SampleApplication.initSdk(context, selectedTerminal)
        }

    val selectedTerminal: SessionParams
        get() = requireTerminal(selectedTerminalKey)

    private val gson = Gson()

    fun init(context: Context) = apply {
        this.context = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)

        val terminalsJson = prefs.getString(TERMINALS_KEY, null)
        terminals = terminalsJson?.let {
            Gson().fromJson<List<SessionParams>>(it,
                TypeToken.getParameterized(List::class.java, SessionParams::class.java).type)
        }.takeIf { !it.isNullOrEmpty() } ?: SessionParams.getDefaultTerminals()

        val selectedTerminalKeyJson = prefs.getString(SELECTED_TERMINAL_KEY, terminals.first().terminalKey)!!
        selectedTerminalKey = selectedTerminalKeyJson
    }

    fun reset() {
        terminals = SessionParams.getDefaultTerminals()
        selectedTerminalKey = terminals.first().terminalKey
    }

    fun findTerminal(terminalKey: String): SessionParams? {
        val terminal = terminals.find { it.terminalKey == terminalKey }
        return terminal
    }

    fun requireTerminal(terminalKey: String) = findTerminal(terminalKey)!!
}