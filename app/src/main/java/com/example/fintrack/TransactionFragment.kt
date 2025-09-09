package com.example.fintrack

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fintrack.databinding.FragmentTransactionBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.absoluteValue
import java.text.SimpleDateFormat
import java.util.*

class TransactionFragment : Fragment() {

    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()

    private var monthlyBudget = 0.0
    private var totalSpent = 0.0
    private var remainingBudget = 0.0
    private var budgetPercentage = 0.0
    private var totalIncome = 0.0
    private var totalExpense = 0.0
    private var totalBalance = 0.0
    private var budgetCreationMonth = ""
    private var currentUsername = ""

    private val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    private val currentMonth = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {

            val userPrefs = requireContext().getSharedPreferences("UserPrefs", 0)
            val token = userPrefs.getString("token", "") ?: ""
            currentUsername = token.split("_").getOrNull(1) ?: "default"


            setupRecyclerView()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }


        createNotificationChannel()


        loadBudgetData()
        loadTransactions()
        loadBalanceData()


        calculateSpending()
        updateBudgetUI()

            // Set click listeners
            binding.btnEditBudget.setOnClickListener {
                showBudgetDialog()
            }

            binding.fabAddTransaction.setOnClickListener {
                showAddTransactionDialog()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error initializing transactions: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            transactions,
            onTransactionRemoved = { transaction ->
                removeTransaction(transaction)
            },
            onTransactionEdited = { transaction ->
                showEditTransactionDialog(transaction)
            }
        )
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }
    }

    private fun loadBudgetData() {
        val prefs = requireContext().getSharedPreferences("FinTrackPrefs", 0)
        monthlyBudget = prefs.getLong("monthly_budget_${currentUsername}", 0L).toDouble() / 100.0
        budgetCreationMonth = prefs.getString("budget_creation_month_${currentUsername}", "") ?: ""
    }

    private fun loadBalanceData() {
        val prefs = requireContext().getSharedPreferences("FinTrackPrefs", 0)
        
        // Check if this is the first run by looking for a flag
        val isFirstRun = prefs.getBoolean("is_first_run_${currentUsername}", true)
        
        if (isFirstRun) {
            // Set initial balance to $5000
            totalIncome = 5000.0
            totalExpense = 0.0
            totalBalance = totalIncome - totalExpense
            
            // Save the initial values
            prefs.edit()
                .putFloat("income_${currentUsername}", totalIncome.toFloat())
                .putFloat("expense_${currentUsername}", totalExpense.toFloat())
                .putFloat("balance_${currentUsername}", totalBalance.toFloat())
                .putBoolean("is_first_run_${currentUsername}", false)
                .apply()
        } else {
            // Load existing values
            totalIncome = prefs.getFloat("income_${currentUsername}", 0f).toDouble()
            totalExpense = prefs.getFloat("expense_${currentUsername}", 0f).toDouble()
        totalBalance = totalIncome - totalExpense
        }
    }

    private fun loadTransactions() {
        try {
        val prefs = requireContext().getSharedPreferences("FinTrackPrefs", 0)
            val json = prefs.getString("transaction_list_${currentUsername}", "[]")
            val type = object : TypeToken<List<Transaction>>() {}.type
            val allTransactions = Gson().fromJson<List<Transaction>>(json, type) ?: emptyList()
            transactions.clear()
            transactions.addAll(allTransactions)
            if (::transactionAdapter.isInitialized) {
                transactionAdapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error loading transactions: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun calculateSpending() {
        val budgetMonthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val budgetDate = if (budgetCreationMonth.isNotEmpty()) {
            budgetMonthFormat.parse(budgetCreationMonth)
        } else {
            null
        }

        totalSpent = transactions
            .filter { transaction ->
                if (budgetDate == null) return@filter false

                val transactionDate = dateFormat.parse(transaction.dateTime)
                if (transactionDate == null) return@filter false

                // Compare month and year
                val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
                val budgetCalendar = Calendar.getInstance().apply { time = budgetDate }

                transaction.amount.startsWith("-") &&
                transactionCalendar.get(Calendar.MONTH) == budgetCalendar.get(Calendar.MONTH) &&
                transactionCalendar.get(Calendar.YEAR) == budgetCalendar.get(Calendar.YEAR)
            }
            .sumOf { it.amount.replace("$", "").replace(",", "").toDouble().absoluteValue }

        remainingBudget = monthlyBudget - totalSpent
        budgetPercentage = if (monthlyBudget > 0) {
            (totalSpent / monthlyBudget).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Budget Alerts"
            val descriptionText = "Notifications when you exceed your budget"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("BUDGET_ALERTS", name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateBudgetUI() {
        binding.tvBudgetAmount.text = "$${"%.2f".format(monthlyBudget)}"
        binding.tvSpentAmount.text = "$${"%.2f".format(totalSpent)}"
        binding.tvRemainingAmount.text = "$${"%.2f".format(remainingBudget)}"

        binding.progressBudget.progress = (budgetPercentage * 100).toInt()

        when {
            monthlyBudget == 0.0 -> {
                binding.tvBudgetStatus.text = "Not set"
                binding.tvBudgetStatus.setTextColor(Color.parseColor("#9E9E9E"))
            }
            remainingBudget < 0 -> {
                binding.tvBudgetStatus.text = "Over budget!"
                binding.tvBudgetStatus.setTextColor(Color.parseColor("#F44336"))
                // Show notification when budget is exceeded
                if (monthlyBudget > 0 && totalSpent > monthlyBudget) {
                    showBudgetExceededNotification()
                }
            }
            budgetPercentage > 0.8 -> {
                binding.tvBudgetStatus.text = "Almost there"
                binding.tvBudgetStatus.setTextColor(Color.parseColor("#FF9800"))
                // Optional: Show warning notification when close to budget
                if (budgetPercentage > 0.9) {
                    showBudgetWarningNotification()
                }
            }
            else -> {
                binding.tvBudgetStatus.text = "On track"
                binding.tvBudgetStatus.setTextColor(Color.parseColor("#4CAF50"))
            }
        }
    }

    private fun saveBudgetData() {
        val prefs = requireContext().getSharedPreferences("FinTrackPrefs", 0)
        prefs.edit()
            .putLong("monthly_budget_${currentUsername}", (monthlyBudget * 100.0).toLong())
            .putString("budget_creation_month_${currentUsername}", budgetCreationMonth)
            .apply()
    }

    private fun saveBalanceData() {
        val prefs = requireContext().getSharedPreferences("FinTrackPrefs", 0)
        prefs.edit()
            .putFloat("income_${currentUsername}", totalIncome.toFloat())
            .putFloat("expense_${currentUsername}", totalExpense.toFloat())
            .putFloat("balance_${currentUsername}", totalBalance.toFloat())
            .apply()
    }

    private fun showBudgetDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_set_budget, null)
        val etBudget = dialogView.findViewById<EditText>(R.id.etBudgetAmount)
        etBudget.setText(monthlyBudget.toString())

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Set Monthly Budget")
            .setPositiveButton("Save") { _, _ ->
                val newBudget = etBudget.text.toString().toDoubleOrNull() ?: 0.0
                monthlyBudget = newBudget
                budgetCreationMonth = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())
                saveBudgetData()
                calculateSpending()
                updateBudgetUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Initialize category chip group
        val chipGroupCategories = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupCategories)
        var selectedCategory = "Food" // Default category

        // Set up chip selection listener
        chipGroupCategories.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.chipFood -> selectedCategory = "Food"
                R.id.chipTransport -> selectedCategory = "Transport"
                R.id.chipHousing -> selectedCategory = "Housing"
                R.id.chipShopping -> selectedCategory = "Shopping"
                R.id.chipEntertainment -> selectedCategory = "Entertainment"
                R.id.chipOther -> selectedCategory = "Other"
            }
        }

        // Initialize transaction type buttons
        val btnIncome = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnIncome)
        val btnExpense = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExpense)
        var isIncome = false // Default to expense

        // Set up button click listeners
        btnIncome.setOnClickListener {
            isIncome = true
            btnIncome.setBackgroundColor(requireContext().getColor(android.R.color.holo_green_dark))
            btnExpense.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        
        btnExpense.setOnClickListener {
            isIncome = false
            btnExpense.setBackgroundColor(requireContext().getColor(android.R.color.holo_red_dark))
            btnIncome.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        // Initialize date and time text views
        val tvDate = dialogView.findViewById<TextView>(R.id.tvDate)
        val tvTime = dialogView.findViewById<TextView>(R.id.tvTime)
        val calendar = Calendar.getInstance()
        
        // Set current date and time as default
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        tvDate.text = dateFormat.format(calendar.time)
        tvTime.text = timeFormat.format(calendar.time)

        // Date picker
        tvDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                    tvDate.text = dateFormat.format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        // Time picker
        tvTime.setOnClickListener {
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    tvTime.text = timeFormat.format(calendar.time)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePickerDialog.show()
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave).setOnClickListener {
            val title = dialogView.findViewById<EditText>(R.id.etTitle).text.toString()
            val amountText = dialogView.findViewById<EditText>(R.id.etAmount).text.toString()
            val type = if (isIncome) "Income" else "Expense"
            val date = tvDate.text.toString()
            val time = tvTime.text.toString()

            if (title.isNotEmpty() && amountText.isNotEmpty()) {
                val amount = amountText.toDouble()
                val formattedAmount = if (isIncome) {
                    "+$${"%.2f".format(amount)}"
                } else {
                    "-$${"%.2f".format(amount)}"
                }
                val dateTime = "$date $time"
                val color = if (isIncome) "#4CAF50" else "#F44336"

                val newTransaction = Transaction(
                    title,
                    formattedAmount,
                    selectedCategory,
                    color,
                    dateTime
                )

                // Update totals
                if (isIncome) {
                    totalIncome += amount
                } else {
                    totalExpense += amount
                }
                totalBalance = totalIncome - totalExpense
                saveBalanceData()

                // Add transaction
                transactions.add(0, newTransaction)
                saveTransactions()
                
                // Update UI
                calculateSpending()
                updateBudgetUI()
                transactionAdapter.notifyItemInserted(0)

                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Initialize category chip group
        val chipGroupCategories = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupCategories)
        var selectedCategory = transaction.category // Use existing category

        // Set up chip selection listener
        chipGroupCategories.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.chipFood -> selectedCategory = "Food"
                R.id.chipTransport -> selectedCategory = "Transport"
                R.id.chipHousing -> selectedCategory = "Housing"
                R.id.chipShopping -> selectedCategory = "Shopping"
                R.id.chipEntertainment -> selectedCategory = "Entertainment"
                R.id.chipOther -> selectedCategory = "Other"
            }
        }

        // Initialize transaction type buttons
        val btnIncome = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnIncome)
        val btnExpense = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExpense)
        var isIncome = transaction.amount.startsWith("+") // Use existing type

        // Set up button click listeners
        btnIncome.setOnClickListener {
            isIncome = true
            btnIncome.setBackgroundColor(requireContext().getColor(android.R.color.holo_green_dark))
            btnExpense.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        btnExpense.setOnClickListener {
            isIncome = false
            btnExpense.setBackgroundColor(requireContext().getColor(android.R.color.holo_red_dark))
            btnIncome.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        // Set initial button states
        if (isIncome) {
            btnIncome.setBackgroundColor(requireContext().getColor(android.R.color.holo_green_dark))
            btnExpense.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        } else {
            btnExpense.setBackgroundColor(requireContext().getColor(android.R.color.holo_red_dark))
            btnIncome.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        // Initialize date and time text views
        val tvDate = dialogView.findViewById<TextView>(R.id.tvDate)
        val tvTime = dialogView.findViewById<TextView>(R.id.tvTime)
        val calendar = Calendar.getInstance()

        // Parse existing date and time
        val dateTimeParts = transaction.dateTime.split(" ")
        if (dateTimeParts.size == 2) {
            tvDate.text = dateTimeParts[0]
            tvTime.text = dateTimeParts[1]
        } else {
            // Set current date and time as default if parsing fails
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvDate.text = dateFormat.format(calendar.time)
            tvTime.text = timeFormat.format(calendar.time)
        }


        tvDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                    tvDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }


        tvTime.setOnClickListener {
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                R.style.CustomTimePickerTheme, // Apply the custom theme
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )


            timePickerDialog.setOnShowListener {
                timePickerDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                timePickerDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            }

            timePickerDialog.show()
        }


        dialogView.findViewById<EditText>(R.id.etTitle).setText(transaction.title)
        dialogView.findViewById<EditText>(R.id.etAmount).setText(transaction.amount.replace("$", "").replace("+", "").replace("-", ""))


        val categoryChipId = when (transaction.category) {
            "Food" -> R.id.chipFood
            "Transport" -> R.id.chipTransport
            "Housing" -> R.id.chipHousing
            "Entertainment" -> R.id.chipEntertainment
            "Shopping" -> R.id.chipShopping
            else -> R.id.chipOther
        }
        chipGroupCategories.check(categoryChipId)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave).setOnClickListener {
            val title = dialogView.findViewById<EditText>(R.id.etTitle).text.toString()
            val amountText = dialogView.findViewById<EditText>(R.id.etAmount).text.toString()
            val date = tvDate.text.toString()
            val time = tvTime.text.toString()

            if (title.isNotEmpty() && amountText.isNotEmpty()) {
                val amount = amountText.toDouble()
                val formattedAmount = if (isIncome) {
                    "+$${"%.2f".format(amount)}"
                } else {
                    "-$${"%.2f".format(amount)}"
                }
                val dateTime = "$date $time"
                val color = if (isIncome) "#4CAF50" else "#F44336"


                val updatedTransaction = Transaction(
                    title = title,
                    amount = formattedAmount,
                    category = selectedCategory,
                    color = color,
                    dateTime = dateTime
                )


                if (isIncome) {
                    totalIncome += amount
                } else {
                    totalExpense += amount
                }
                totalBalance = totalIncome - totalExpense
                saveBalanceData()


                val index = transactions.indexOf(transaction)
                if (index != -1) {
                    transactions[index] = updatedTransaction
                    transactionAdapter.notifyItemChanged(index)
                    saveTransactions()
                }

                // Update UI
                calculateSpending()
                updateBudgetUI()

                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun removeTransaction(transaction: Transaction) {
        try {
            val position = transactions.indexOf(transaction)
            if (position != -1) {

                val amount = transaction.amount.replace("$", "").replace("+", "").replace("-", "").toDouble()
                if (transaction.amount.startsWith("+")) {
                    totalIncome -= amount
                } else {
                    totalExpense -= amount
                }
                totalBalance = totalIncome - totalExpense
                saveBalanceData()

                // Remove transaction
                transactions.removeAt(position)
                transactionAdapter.notifyItemRemoved(position)
                saveTransactions()


                calculateSpending()
                updateBudgetUI()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error removing transaction: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveTransactions() {
        val prefs = requireContext().getSharedPreferences("FinTrackPrefs", 0)
        prefs.edit().putString("transaction_list_${currentUsername}", Gson().toJson(transactions)).apply()
    }

    fun showBudgetExceededNotification() {
        try {
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(requireContext(), "BUDGET_ALERTS")
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Budget Limit Exceeded!")
                .setContentText("You've spent $${String.format("%.2f", totalSpent)} out of your $${String.format("%.2f", monthlyBudget)} budget")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.RED)
            .setLights(Color.RED, 1000, 1000)
            .setVibrate(longArrayOf(0, 500, 250, 500))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(1, notification)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to show notification: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun showBudgetWarningNotification() {
        try {
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(requireContext(), "BUDGET_ALERTS")
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Budget Warning")
                .setContentText("You've used ${String.format("%.0f", budgetPercentage * 100)}% of your monthly budget")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#FF9800"))
            .setLights(Color.parseColor("#FF9800"), 1000, 1000)
            .setVibrate(longArrayOf(0, 250, 250, 250))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(2, notification)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to show notification: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getCategoryColor(category: String): String {
        return when (category) {
            "Food" -> "#FF5722"
            "Transport" -> "#2196F3"
            "Shopping" -> "#4CAF50"
            "Bills" -> "#F44336"
            "Entertainment" -> "#9C27B0"
            else -> "#000000"
        }
    }
}