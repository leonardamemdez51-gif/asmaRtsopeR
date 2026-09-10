package com.example.core.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.core.logger.AppLogger
import java.net.URLEncoder

object WhatsAppHelper {

    fun sendPaymentReminder(
        context: Context,
        clientPhone: String,
        clientName: String,
        amountDue: Double,
        dueDate: String,
        loanId: Long
    ) {
        val cleanPhone = formatPhoneNumber(clientPhone)
        val message = """
            Hola *$clientName*, te saludamos de *RAMA Microfinanzas*. 
            📌 Le recordamos que su cuota del préstamo #$loanId vence el *$dueDate*.
            💰 Monto a pagar: *$${String.format("%.2f", amountDue)} MXN*.
            
            Gracias por su puntualidad para mantener su excelente calificación crediticia.
        """.trimIndent()

        openWhatsAppMessage(context, cleanPhone, message)
    }

    fun sendPaymentReceipt(
        context: Context,
        clientPhone: String,
        clientName: String,
        receiptNo: String,
        amountPaid: Double,
        remainingBalance: Double,
        method: String
    ) {
        val cleanPhone = formatPhoneNumber(clientPhone)
        val message = """
            ✅ *COMPROBANTE DE PAGO - RAMA ERP*
            
            👤 Cliente: *$clientName*
            🧾 Recibo: *$receiptNo*
            💵 Pago Registrado: *$${String.format("%.2f", amountPaid)} MXN* ($method)
            📉 Saldo Restante: *$${String.format("%.2f", remainingBalance)} MXN*
            
            ¡Gracias por su pago!
        """.trimIndent()

        openWhatsAppMessage(context, cleanPhone, message)
    }

    private fun openWhatsAppMessage(context: Context, phone: String, message: String) {
        try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val uriStr = if (phone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$phone&text=$encodedMsg"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMsg"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AppLogger.i("WhatsApp action triggered for phone: $phone", tag = "WhatsAppHelper")
        } catch (e: Exception) {
            AppLogger.e("Could not open WhatsApp intent", e, tag = "WhatsAppHelper")
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        val digitsOnly = phone.replace(Regex("[^0-9]"), "")
        return if (digitsOnly.length == 10) "521$digitsOnly" else digitsOnly
    }
}
