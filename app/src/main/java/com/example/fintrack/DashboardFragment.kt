package com.example.fintrack

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.absoluteValue
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tvBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var rvTransactions: RecyclerView

    private var totalBalance = 0.0
    private var totalIncome = 0.0
    private var totalExpense = 0.0
    private var currentUsername = ""

    private val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())

    private val transactions = mutableListOf<Transaction>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get current username from UserPrefs
        val userPrefs = requireContext().getSharedPreferences("UserPrefs", 0)
        val token = userPrefs.getString("token", "") ?: ""
        currentUsername = token.split("_")[1]

        // Load SharedPreferences
        totalIncome = loadFromPrefs("income_${currentUsername}", 0.0)
        totalExpense = loadFromPrefs("expense_${currentUsername}", 0.0)
        totalBalance = totalIncome - totalExpense

        val savedTransactions = loadTransactionsFromPrefs()
        if (savedTransactions.isNotEmpty()) {
            transactions.clear()
            transactions.addAll(savedTransactions)
        }

        // Init UI
        tvBalance = view.findViewById(R.id.tvBalance)
        tvIncome = view.findViewById(R.id.tvIncome)
        tvExpense = view.findViewById(R.id.tvExpense)
        rvTransactions = view.findViewById(R.id.rvTransactions)

        updateBalance(totalBalance, totalIncome, totalExpense)
        setupTransactions()
    }

    private fun loadTransactionsFromPrefs(): List<Transaction> {
        val prefs = requireContext().getSharedPreferences("FinTrackPrefs", 0)
        val json = prefs.getString("transaction_list_${currentUsername}", null)
        return if (json != null) {
            val type = object : TypeToken<List<Transaction>>() {}.type
            Gson().fromJson(json, type)
        } else {
            listOf()
        }
    }

    private fun loadFromPrefs(key: String, defaultValue: Double): Double {
        val prefs = requireContext().getSharedPreferences("FinTrackPrefs", 0)
        return prefs.getFloat(key, defaultValue.toFloat()).toDouble()
    }

    private fun updateBalance(total: Double, income: Double, expense: Double) {
        tvBalance.text = "$${"%.2f".format(total)}"
        tvIncome.text = "+$${"%.2f".format(income)}"
        tvIncome.setTextColor(Color.parseColor("#4CAF50"))
        tvExpense.text = "-$${"%.2f".format(expense)}"
        tvExpense.setTextColor(Color.parseColor("#F44336"))
    }

    private fun setupTransactions() {
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        rvTransactions.adapter = DashboardTransactionAdapter(transactions)
    }

    private fun saveBalanceData() {
        val prefs = requireContext().getSharedPreferences("FinTrackPrefs", 0)
        prefs.edit()
            .putFloat("income_${currentUsername}", totalIncome.toFloat())
            .putFloat("expense_${currentUsername}", totalExpense.toFloat())
            .putFloat("balance_${currentUsername}", totalBalance.toFloat())
            .apply()
    }

    private fun saveTransactions() {
        val prefs = requireContext().getSharedPreferences("FinTrackPrefs", 0)
        prefs.edit().putString("transaction_list_${currentUsername}", Gson().toJson(transactions)).apply()
    }

    private fun updateDashboardUI() {
        updateBalance(totalBalance, totalIncome, totalExpense)
    }
}

