package ru.tinkoff.acquiring.sample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_edit_terminal.*
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.utils.SessionParams
import ru.tinkoff.acquiring.sample.utils.TerminalsManager
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.utils.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

class EditTerminalFragment : Fragment() {

    var oldTerminalKey: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_terminal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        oldTerminalKey = arguments?.getString(TERMINAL_KEY)

        oldTerminalKey?.let { TerminalsManager.requireTerminal(it) }?.run {
            terminal_key_input.setText(terminalKey)
            description_input.setText(description)
            public_key_input.setText(publicKey)
            password_input.setText(password)
            customer_key_input.setText(customerKey)
            customer_email_input.setText(customerEmail)
        }

        save.setOnClickListener {
            val terminal = SessionParams(
                terminal_key_input.text.toString().trim(),
                password_input.text.toString().trim().takeUnless { it.isBlank() },
                public_key_input.text.toString().trim(),
                customer_key_input.text.toString().trim(),
                customer_email_input.text.toString().trim(),
                description_input.text.toString().trim().takeUnless { it.isBlank() },
            )

            try {
                terminal.validate()
            } catch (e: Throwable) {
                toast(e.message!!)
                return@setOnClickListener
            }

            saveTerminal(terminal)
            close()
        }

        cancel.setOnClickListener {
            close()
        }
    }

    fun close() {
        (requireActivity() as TerminalsActivity).updateList()
        requireActivity().onBackPressed()
    }

    fun saveTerminal(terminal: SessionParams) {
        val oldTerminalKey = oldTerminalKey
        val oldSelectedTerminalKey = TerminalsManager.selectedTerminalKey
        val terminals = TerminalsManager.terminals.toMutableList()
        if (oldTerminalKey != null) {
            val oldTerminal = terminals.first { it.terminalKey == oldTerminalKey }
            val oldIndex = terminals.indexOf(oldTerminal)
            terminals.remove(oldTerminal)
            terminals.add(oldIndex, terminal)
        } else {
            terminals.add(terminal)
        }
        TerminalsManager.terminals = terminals

        if (oldSelectedTerminalKey == oldTerminalKey) {
            TerminalsManager.selectedTerminalKey = terminal.terminalKey
        }
    }

    private fun SessionParams.validate() {
        if (oldTerminalKey != terminalKey) {
            check(TerminalsManager.findTerminal(terminalKey) == null) {
                "Terminal with this key already exists"
            }
        }
        check(terminalKey.isNotBlank()) { "Terminal Key can't be empty" }
        check(publicKey.isNotBlank()) { "Public Key can't be empty" }
        check(validatePublicKey(publicKey)) { "Error parsing public key" }
        check(customerKey.isNotBlank()) { "Customer Key can't be empty" }
    }

    private fun validatePublicKey(source: String): Boolean {
        return try {
            val publicBytes = Base64.decode(source, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(publicBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            keyFactory.generatePublic(keySpec)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {

        private const val TERMINAL_KEY = "terminal_key"

        fun newInstance(terminalKey: String?): EditTerminalFragment {
            val args = Bundle()
            args.putString(TERMINAL_KEY, terminalKey)
            val fragment = EditTerminalFragment()
            fragment.arguments = args
            return fragment
        }
    }
}