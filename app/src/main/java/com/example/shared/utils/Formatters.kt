package com.example.shared.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    private val percentFormat = NumberFormat.getPercentInstance(Locale("es", "MX")).apply {
        maximumFractionDigits = 1
    }

    fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun formatPercent(value: Double): String {
        return percentFormat.format(value)
    }

    fun formatDateIso(timestampMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    fun formatDateDisplay(timestampMs: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    fun formatDateTimeDisplay(timestampMs: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }
}
