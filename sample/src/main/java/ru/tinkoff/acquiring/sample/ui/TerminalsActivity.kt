package ru.tinkoff.acquiring.sample.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_terminals.*
import kotlinx.android.synthetic.main.item_terminal.view.*
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.utils.SessionParams
import ru.tinkoff.acquiring.sample.utils.TerminalsManager

class TerminalsActivity : AppCompatActivity() {

    private lateinit var terminals: MutableList<SessionParams>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_terminals)

        updateList()
        recycler.adapter = Adapter()

        add.setOnClickListener {
            openEditFragment(null)
        }

        reset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset all terminal settings to default?")
                .setPositiveButton("Yes") { _, _ ->
                    TerminalsManager.reset()
                    updateList()
                }.setNegativeButton("No") { _, _ -> }
                .show()
        }
    }

    private fun openEditFragment(terminalKey: String?) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, EditTerminalFragment.newInstance(terminalKey))
            .addToBackStack(null)
            .commit()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList() {
        terminals = TerminalsManager.terminals.toMutableList()
        recycler.adapter?.notifyDataSetChanged()
    }

    inner class Adapter : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_terminal, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(terminals.get(position))
        }

        override fun getItemCount(): Int = terminals.size
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(sessionParams: SessionParams) = with(itemView) {
            terminal_key.text = sessionParams.terminalKey

            description.text = sessionParams.description
            description.isGone = sessionParams.description.isNullOrEmpty()

            public_key.text = sessionParams.publicKey
            password.text = sessionParams.password.takeUnless { it.isNullOrBlank() } ?: "<blank>"

            customer_key.text = sessionParams.customerKey
            customer_email.text = sessionParams.customerEmail


            selected_icon.isGone = sessionParams.terminalKey != TerminalsManager.selectedTerminalKey

            delete.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Delete terminal?")
                    .setPositiveButton("Yes") { _, _ ->
                        terminals.remove(sessionParams)
                        TerminalsManager.terminals = terminals
                        updateList()
                    }.setNegativeButton("No") { _, _ -> }
                    .show()
            }

            edit.setOnClickListener {
                openEditFragment(sessionParams.terminalKey)
            }

            setOnClickListener {
                TerminalsManager.selectedTerminalKey = sessionParams.terminalKey
                updateList()
            }
        }
    }
}