package com.quotely.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** Estados persistidos de una cotización. VENCIDA se calcula, nunca se guarda. */
enum class EstadoCotizacion(val etiqueta: String) {
    BORRADOR("Borrador"),
    EMITIDA("Emitida"),
    CONVERTIDA("Convertida"),
    ANULADA("Anulada"),
    VENCIDA("Vencida");

    companion object {
        fun desde(valor: String?): EstadoCotizacion =
            entries.firstOrNull { it.name == valor } ?: BORRADOR
    }
}

enum class EstadoPedido(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    CONFIRMADO("Confirmado"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    companion object {
        fun desde(valor: String?): EstadoPedido =
            entries.firstOrNull { it.name == valor } ?: PENDIENTE
    }
}

enum class TipoDescuento(val etiqueta: String) {
    NINGUNO("Sin descuento"),
    PORCENTAJE("Porcentaje"),
    MONTO("Monto fijo");

    companion object {
        fun desde(valor: String?): TipoDescuento =
            entries.firstOrNull { it.name == valor } ?: NINGUNO
    }
}

data class TramoVolumen(val cantidadMinima: Int, val porcentaje: Double)

object PricingConfig {
    const val IVA = 0.16
    const val DIAS_VIGENCIA_POR_DEFECTO = 15
    const val DIAS_AVISO_VENCIMIENTO = 7

    val tramosVolumen = listOf(
        TramoVolumen(10, 5.0),
        TramoVolumen(25, 10.0),
        TramoVolumen(50, 15.0)
    )

    fun descuentoPorVolumen(cantidad: Int): Double =
        tramosVolumen.lastOrNull { cantidad >= it.cantidadMinima }?.porcentaje ?: 0.0
}

data class LineaEntrada(
    val productoId: Long?,
    val codigo: String,
    val descripcion: String,
    val unidad: String,
    val precioUnitario: Double,
    val cantidad: Int
)

data class LineaCalculada(
    val productoId: Long?,
    val codigo: String,
    val descripcion: String,
    val unidad: String,
    val precioUnitario: Double,
    val cantidad: Int,
    val base: Double,
    val descuentoVolumenPct: Double,
    val descuentoVolumen: Double,
    val importe: Double
)

data class Totales(
    val lineas: List<LineaCalculada>,
    val subtotal: Double,
    val descuentoVolumen: Double,
    val descuentoGlobal: Double,
    val baseGravable: Double,
    val iva: Double,
    val total: Double
)

object Calculadora {

    fun redondear(valor: Double): Double =
        BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).toDouble()

    fun calcularLinea(entrada: LineaEntrada, aplicarVolumen: Boolean = true): LineaCalculada {
        val cantidad = entrada.cantidad.coerceAtLeast(0)
        val base = redondear(entrada.precioUnitario * cantidad)
        val porcentaje = if (aplicarVolumen) PricingConfig.descuentoPorVolumen(cantidad) else 0.0
        val descuento = redondear(base * porcentaje / 100.0)
        return LineaCalculada(
            productoId = entrada.productoId,
            codigo = entrada.codigo,
            descripcion = entrada.descripcion,
            unidad = entrada.unidad,
            precioUnitario = entrada.precioUnitario,
            cantidad = cantidad,
            base = base,
            descuentoVolumenPct = porcentaje,
            descuentoVolumen = descuento,
            importe = redondear(base - descuento)
        )
    }

    fun calcular(
        entradas: List<LineaEntrada>,
        tipoDescuento: TipoDescuento = TipoDescuento.NINGUNO,
        valorDescuento: Double = 0.0,
        aplicarDescuentos: Boolean = true
    ): Totales {
        val lineas = entradas.map { calcularLinea(it, aplicarDescuentos) }
        val subtotal = redondear(lineas.sumOf { it.base })
        val descuentoVolumen = redondear(lineas.sumOf { it.descuentoVolumen })
        val despuesDeVolumen = redondear(subtotal - descuentoVolumen)

        val descuentoGlobal = if (!aplicarDescuentos) {
            0.0
        } else when (tipoDescuento) {
            TipoDescuento.NINGUNO -> 0.0
            TipoDescuento.PORCENTAJE ->
                redondear(despuesDeVolumen * valorDescuento.coerceIn(0.0, 100.0) / 100.0)
            TipoDescuento.MONTO ->
                redondear(valorDescuento.coerceIn(0.0, despuesDeVolumen))
        }

        val baseGravable = redondear((despuesDeVolumen - descuentoGlobal).coerceAtLeast(0.0))
        val iva = redondear(baseGravable * PricingConfig.IVA)
        return Totales(
            lineas = lineas,
            subtotal = subtotal,
            descuentoVolumen = descuentoVolumen,
            descuentoGlobal = descuentoGlobal,
            baseGravable = baseGravable,
            iva = iva,
            total = redondear(baseGravable + iva)
        )
    }
}

fun estadoEfectivoCotizacion(
    estado: EstadoCotizacion,
    fechaCaducidad: LocalDate,
    hoy: LocalDate = LocalDate.now()
): EstadoCotizacion =
    if (estado == EstadoCotizacion.EMITIDA && fechaCaducidad.isBefore(hoy)) {
        EstadoCotizacion.VENCIDA
    } else {
        estado
    }

fun puedeConvertirCotizacion(
    estado: EstadoCotizacion,
    fechaCaducidad: LocalDate,
    hoy: LocalDate = LocalDate.now()
): Boolean =
    estado == EstadoCotizacion.EMITIDA && !fechaCaducidad.isBefore(hoy)

fun diasParaVencer(fechaCaducidad: LocalDate, hoy: LocalDate = LocalDate.now()): Long =
    java.time.temporal.ChronoUnit.DAYS.between(hoy, fechaCaducidad)
