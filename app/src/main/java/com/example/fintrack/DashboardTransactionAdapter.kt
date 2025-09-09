package com.example.fintrack

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DashboardTransactionAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<DashboardTransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTransactionTitle)
        val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvTransactionDateTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_dashboard, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.tvTitle.text = transaction.title
        holder.tvAmount.text = transaction.amount
        holder.tvAmount.setTextColor(Color.parseColor(transaction.color))
        holder.tvDateTime.text = transaction.dateTime
    }

    override fun getItemCount() = transactions.size
} 