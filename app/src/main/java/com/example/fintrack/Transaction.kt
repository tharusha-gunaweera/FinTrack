package com.example.fintrack

data class Transaction(
    val title: String,
    val amount: String,
    val category: String,
    val color: String,
    val dateTime: String = "" // Default empty string for backward compatibility
) 