package com.quotely.app.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formato {
    val localeMx: Locale = Locale("es", "MX")
    private val simbolos = DecimalFormatSymbols(localeMx).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    private val decimales = ThreadLocal.withInitial {
        DecimalFormat("#,##0.00", simbolos)
    }
    private val enteros = ThreadLocal.withInitial {
        DecimalFormat("#,##0", simbolos)
    }
    private val fechaLarga = DateTimeFormatter.ofPattern("dd MMM yyyy", localeMx)
    private val fechaCorta = DateTimeFormatter.ofPattern("dd/MM/yy", localeMx)
    private val fechaHora = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", localeMx)

    private fun formatoDecimales(): DecimalFormat =
        decimales.get() ?: DecimalFormat("#,##0.00", simbolos).also { decimales.set(it) }

    private fun formatoEnteros(): DecimalFormat =
        enteros.get() ?: DecimalFormat("#,##0", simbolos).also { enteros.set(it) }

    fun moneda(valor: Double): String =
        if (valor < 0) "-$" + formatoDecimales().format(-valor) else "$" + formatoDecimales().format(valor)

    fun numero(valor: Double): String = formatoDecimales().format(valor)

    fun entero(valor: Number): String = formatoEnteros().format(valor)

    fun fecha(fecha: LocalDate): String = fechaLarga.format(fecha)

    fun fechaCorta(fecha: LocalDate): String = fechaCorta.format(fecha)

    fun fechaHora(millis: Long): String =
        fechaHora.format(java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()))

    fun porcentaje(valor: Double): String {
        val limpio = if (valor % 1.0 == 0.0) valor.toInt().toString() else numero(valor)
        return "$limpio%"
    }

    fun iniciales(nombre: String): String =
        nombre.trim().split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "Q" }

    fun saludo(): String {
        val hora = java.time.LocalTime.now().hour
        return when {
            hora < 12 -> "Buen día"
            hora < 19 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }
}

fun LocalDate.aMillisUtc(): Long =
    atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.aLocalDateUtc(): LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneOffset.UTC).toLocalDate()
