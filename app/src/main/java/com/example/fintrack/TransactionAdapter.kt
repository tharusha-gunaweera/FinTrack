package com.example.fintrack

// File: TransactionAdapter.kt
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class TransactionAdapter(
    private val transactions: MutableList<Transaction>,
    private val onTransactionRemoved: (Transaction) -> Unit,
    private val onTransactionEdited: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {


    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTransactionTitle)
        val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvTransactionDateTime)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemoveTransaction)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditTransaction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.tvTitle.text = transaction.title
        holder.tvAmount.text = transaction.amount
        holder.tvAmount.setTextColor(Color.parseColor(transaction.color))
        holder.tvDateTime.text = transaction.dateTime

        holder.btnRemove.setOnClickListener {
            onTransactionRemoved(transaction)
        }

        holder.btnEdit.setOnClickListener {
            onTransactionEdited(transaction)
        }
    }

    override fun getItemCount() = transactions.size
}