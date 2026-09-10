package com.example

import com.example.data.service.WhatsAppIntegrationService
import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppIntegrationTest {

    @Test
    fun testFormatTemplateMessage_withValidVariables() {
        val template = "Hola {nombre}, confirmamos tu pago de {monto} para el préstamo #{folio}."
        val variables = mapOf(
            "nombre" to "Maria Torres",
            "monto" to "$120.00",
            "folio" to "PRST-1004"
        )
        val result = WhatsAppIntegrationService.formatTemplateMessage(template, variables)
        assertEquals("Hola Maria Torres, confirmamos tu pago de $120.00 para el préstamo #PRST-1004.", result)
    }

    @Test
    fun testFormatTemplateMessage_withCaseInsensitivity() {
        val template = "Hola {Nombre}, su saldo es {SALDO}."
        val variables = mapOf(
            "nombre" to "Roberto Gómez",
            "saldo" to "$500.00"
        )
        val result = WhatsAppIntegrationService.formatTemplateMessage(template, variables)
        assertEquals("Hola Roberto Gómez, su saldo es $500.00.", result)
    }

    @Test
    fun testFormatTemplateMessage_purgesUnmatchedPlaceholders() {
        val template = "Hola {nombre}, tu cuota de {cuota} vence el {fecha_pago}."
        val variables = mapOf(
            "nombre" to "Roberto Gómez",
            "cuota" to "$200.00"
            // "fecha_pago" is missing
        )
        val result = WhatsAppIntegrationService.formatTemplateMessage(template, variables)
        assertEquals("Hola Roberto Gómez, tu cuota de $200.00 vence el .", result)
    }

    @Test
    fun testFormatTemplateMessage_appendsSignature() {
        val template = "Hola {nombre}, pago registrado."
        val variables = mapOf("nombre" to "Carlos")
        val signature = "Atentamente, RAMA Microfinanzas."
        val result = WhatsAppIntegrationService.formatTemplateMessage(template, variables, signature)
        assertEquals("Hola Carlos, pago registrado.\n\nAtentamente, RAMA Microfinanzas.", result)
    }

    @Test
    fun testBuildVariableMap_returnsCorrectMappings() {
        val variables = WhatsAppIntegrationService.buildVariableMap(
            clientName = "Juan Pérez",
            monto = "$150.00",
            cuota = "$50.00",
            fechaPago = "31/10/2026",
            diasAtraso = 3,
            saldo = "$300.00",
            numeroPrestamo = "PRST-9999",
            nombreCobrador = "Roberto Gómez",
            telefonoEmpresa = "55-1234-5678",
            fechaVencimiento = "15/11/2026",
            mora = "$10.00",
            totalPendiente = "$60.00",
            nombreEmpresa = "RAMA Microfinanzas S.A."
        )

        assertEquals("Juan Pérez", variables["nombre"])
        assertEquals("PRST-9999", variables["folio"])
        assertEquals("$150.00", variables["monto"])
        assertEquals("$300.00", variables["saldo"])
        assertEquals("$50.00", variables["cuota"])
        assertEquals("31/10/2026", variables["fecha_pago"])
        assertEquals("3", variables["dias_atraso"])
        assertEquals("Roberto Gómez", variables["nombre_cobrador"])
        assertEquals("55-1234-5678", variables["telefono_empresa"])
        assertEquals("15/11/2026", variables["fecha_vencimiento"])
        assertEquals("$10.00", variables["mora"])
        assertEquals("$60.00", variables["total_pendiente"])
        assertEquals("RAMA Microfinanzas S.A.", variables["nombre_empresa"])
    }
}
