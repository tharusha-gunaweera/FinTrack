package com.example.fintrack

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IDataSet
import com.github.mikephil.charting.utils.ViewPortHandler
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.absoluteValue
import java.text.SimpleDateFormat
import java.util.*

class ReportsFragment : Fragment() {

    private lateinit var incomeExpenseChart: BarChart
    private lateinit var expensePieChart: PieChart
    private lateinit var cashFlowChart: LineChart
    private var currentUsername = ""
    private val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    private val transactions = mutableListOf<Transaction>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reports, container, false)

        // Get current username from UserPrefs
        val userPrefs = requireContext().getSharedPreferences("UserPrefs", 0)
        val token = userPrefs.getString("token", "") ?: ""
        currentUsername = token.split("_")[1]

        // Load transactions
        loadTransactions()

        // Initialize charts
        incomeExpenseChart = view.findViewById(R.id.incomeExpenseChart)
        expensePieChart = view.findViewById(R.id.expensePieChart)
        cashFlowChart = view.findViewById(R.id.cashFlowChart)

        setupIncomeExpenseChart()
        setupExpensePieChart()
        setupCashFlowChart()

        return view
    }

    private fun loadTransactions() {
        val prefs = requireContext().getSharedPreferences("FinTrackPrefs", 0)
        val json = prefs.getString("transaction_list_${currentUsername}", null)
        if (json != null) {
            val type = object : TypeToken<List<Transaction>>() {}.type
            val allTransactions = Gson().fromJson<List<Transaction>>(json, type)
            transactions.clear()
            transactions.addAll(allTransactions)
        }
    }

    private fun setupIncomeExpenseChart() {
        // Get last 6 months
        val calendar = Calendar.getInstance()
        val months = mutableListOf<String>()
        val monthlyData = mutableMapOf<String, Pair<Double, Double>>() // Pair of (Income, Expense)

        // Initialize last 6 months with zero values
        repeat(6) {
            val month = monthFormat.format(calendar.time)
            months.add(0, month)
            monthlyData[month] = Pair(0.0, 0.0)
            calendar.add(Calendar.MONTH, -1)
        }

        // Calculate monthly totals
        transactions.forEach { transaction ->
            val transactionDate = dateFormat.parse(transaction.dateTime) ?: return@forEach
            val month = monthFormat.format(transactionDate)
            
            if (monthlyData.containsKey(month)) {
                val amount = transaction.amount.replace("$", "").replace(",", "").toDouble()
                val (currentIncome, currentExpense) = monthlyData[month] ?: Pair(0.0, 0.0)
                
                if (transaction.amount.startsWith("+")) {
                    monthlyData[month] = Pair(currentIncome + amount, currentExpense)
                } else {
                    monthlyData[month] = Pair(currentIncome, currentExpense + amount.absoluteValue)
                }
            }
        }

        // Create chart entries
        val entriesIncome = months.mapIndexed { index, month ->
            BarEntry(index.toFloat(), monthlyData[month]?.first?.toFloat() ?: 0f)
        }

        val entriesExpense = months.mapIndexed { index, month ->
            BarEntry(index.toFloat(), monthlyData[month]?.second?.toFloat() ?: 0f)
        }

        val barDataSetIncome = BarDataSet(entriesIncome, "Income").apply {
            color = Color.parseColor("#4CAF50")
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }

        val barDataSetExpense = BarDataSet(entriesExpense, "Expense").apply {
            color = Color.parseColor("#F44336")
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }

        val data = BarData(barDataSetIncome, barDataSetExpense).apply {
            barWidth = 0.3f
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "$${value.toInt()}"
                }
            })
        }

        incomeExpenseChart.apply {
            this.data = data
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(months)
                textColor = Color.WHITE
            }
            axisLeft.textColor = Color.WHITE
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            animateY(1000, Easing.EaseInOutQuad)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            setNoDataTextColor(Color.WHITE)
            setDrawBorders(false)
            extraBottomOffset = 10f
        }

        incomeExpenseChart.invalidate()
    }

    private fun setupExpensePieChart() {
        // Calculate expense breakdown by category
        val categoryTotals = mutableMapOf<String, Double>()
        
        transactions.forEach { transaction ->
            if (transaction.amount.startsWith("-")) {
                val amount = transaction.amount.replace("$", "").replace(",", "").toDouble().absoluteValue
                categoryTotals[transaction.category] = (categoryTotals[transaction.category] ?: 0.0) + amount
            }
        }

        val entries = categoryTotals.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#FF5722"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#FFC107"),
                Color.parseColor("#607D8B")
            )
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.4f
            valueLineColor = Color.WHITE
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "$${value.toInt()}"
                }
            })
        }

        expensePieChart.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 50f
            transparentCircleRadius = 55f
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.parseColor("#1E1E1E"))
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            centerText = "Expenses"
            setCenterTextColor(Color.WHITE)
            setCenterTextSize(16f)
            setDrawEntryLabels(false)
            setTouchEnabled(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1000, Easing.EaseInOutQuad)
            setNoDataTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
        }

        expensePieChart.invalidate()
    }

    private fun setupCashFlowChart() {
        // Get last 7 days
        val calendar = Calendar.getInstance()
        val days = mutableListOf<String>()
        val dailyData = mutableMapOf<String, Double>()

        // Initialize last 7 days with zero values
        repeat(7) {
            val day = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
            days.add(0, day)
            dailyData[day] = 0.0
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        // Calculate daily cash flow
        transactions.forEach { transaction ->
            val transactionDate = dateFormat.parse(transaction.dateTime) ?: return@forEach
            val day = SimpleDateFormat("EEE", Locale.getDefault()).format(transactionDate)
            
            if (dailyData.containsKey(day)) {
                val amount = transaction.amount.replace("$", "").replace(",", "").toDouble()
                val currentFlow = dailyData[day] ?: 0.0
                dailyData[day] = currentFlow + (if (transaction.amount.startsWith("+")) amount else -amount.absoluteValue)
            }
        }

        val entries = days.mapIndexed { index, day ->
            Entry(index.toFloat(), dailyData[day]?.toFloat() ?: 0f)
        }

        val dataSet = LineDataSet(entries, "Cash Flow").apply {
            color = Color.parseColor("#BB86FC")
            valueTextColor = Color.WHITE
            lineWidth = 2.5f
            circleRadius = 4f
            setCircleColor(Color.parseColor("#BB86FC"))
            setDrawCircleHole(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            fillAlpha = 100
            setDrawFilled(true)
            fillColor = Color.parseColor("#BB86FC")
            setDrawValues(false)
        }

        val data = LineData(dataSet)

        cashFlowChart.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(days)
                textColor = Color.WHITE
            }
            axisLeft.textColor = Color.WHITE
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            setNoDataTextColor(Color.WHITE)
            setDrawBorders(false)
            animateX(1000, Easing.EaseInOutQuad)
        }

        cashFlowChart.invalidate()
    }
}