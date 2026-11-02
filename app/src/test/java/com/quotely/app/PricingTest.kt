package com.quotely.app

import com.quotely.app.domain.Calculadora
import com.quotely.app.domain.EstadoCotizacion
import com.quotely.app.domain.LineaEntrada
import com.quotely.app.domain.PricingConfig
import com.quotely.app.domain.TipoDescuento
import com.quotely.app.domain.estadoEfectivoCotizacion
import com.quotely.app.domain.puedeConvertirCotizacion
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PricingTest {

    private fun laptop(cantidad: Int) = LineaEntrada(
        productoId = 1,
        codigo = "LAP-001",
        descripcion = "Laptop",
        unidad = "pieza",
        precioUnitario = 18_990.00,
        cantidad = cantidad
    )

    private fun monitor(cantidad: Int) = LineaEntrada(
        productoId = 2,
        codigo = "MON-001",
        descripcion = "Monitor",
        unidad = "pieza",
        precioUnitario = 3_450.00,
        cantidad = cantidad
    )

    private fun mouse(cantidad: Int) = LineaEntrada(
        productoId = 3,
        codigo = "MOU-001",
        descripcion = "Mouse",
        unidad = "pieza",
        precioUnitario = 899.00,
        cantidad = cantidad
    )

    @Test
    fun `los tramos de volumen respetan los limites`() {
        assertEquals(0.0, PricingConfig.descuentoPorVolumen(9), 0.0)
        assertEquals(5.0, PricingConfig.descuentoPorVolumen(10), 0.0)
        assertEquals(5.0, PricingConfig.descuentoPorVolumen(24), 0.0)
        assertEquals(10.0, PricingConfig.descuentoPorVolumen(25), 0.0)
        assertEquals(10.0, PricingConfig.descuentoPorVolumen(49), 0.0)
        assertEquals(15.0, PricingConfig.descuentoPorVolumen(50), 0.0)
        assertEquals(15.0, PricingConfig.descuentoPorVolumen(200), 0.0)
    }

    @Test
    fun `una cotizacion sin descuentos aplica solo IVA`() {
        val totales = Calculadora.calcular(listOf(laptop(2)))
        assertEquals(37_980.00, totales.subtotal, 0.001)
        assertEquals(0.00, totales.descuentoVolumen, 0.001)
        assertEquals(0.00, totales.descuentoGlobal, 0.001)
        assertEquals(6_076.80, totales.iva, 0.001)
        assertEquals(44_056.80, totales.total, 0.001)
    }

    @Test
    fun `combina descuento por volumen y descuento global porcentual`() {
        val totales = Calculadora.calcular(
            entradas = listOf(monitor(10)),
            tipoDescuento = TipoDescuento.PORCENTAJE,
            valorDescuento = 10.0
        )
        assertEquals(34_500.00, totales.subtotal, 0.001)
        assertEquals(1_725.00, totales.descuentoVolumen, 0.001)
        assertEquals(3_277.50, totales.descuentoGlobal, 0.001)
        assertEquals(29_497.50, totales.baseGravable, 0.001)
        assertEquals(4_719.60, totales.iva, 0.001)
        assertEquals(34_217.10, totales.total, 0.001)
    }

    @Test
    fun `el descuento global por monto no supera el subtotal`() {
        val totales = Calculadora.calcular(
            entradas = listOf(laptop(2)),
            tipoDescuento = TipoDescuento.MONTO,
            valorDescuento = 100_000.0
        )
        assertEquals(37_980.00, totales.descuentoGlobal, 0.001)
        assertEquals(0.00, totales.baseGravable, 0.001)
        assertEquals(0.00, totales.iva, 0.001)
        assertEquals(0.00, totales.total, 0.001)
    }

    @Test
    fun `el pedido directo usa precio base y no aplica descuentos`() {
        val totales = Calculadora.calcular(
            entradas = listOf(mouse(5)),
            tipoDescuento = TipoDescuento.PORCENTAJE,
            valorDescuento = 20.0,
            aplicarDescuentos = false
        )
        assertEquals(4_495.00, totales.subtotal, 0.001)
        assertEquals(0.00, totales.descuentoVolumen, 0.001)
        assertEquals(0.00, totales.descuentoGlobal, 0.001)
        assertEquals(719.20, totales.iva, 0.001)
        assertEquals(5_214.20, totales.total, 0.001)
    }

    @Test
    fun `los importes se redondean a dos decimales`() {
        val entrada = LineaEntrada(
            productoId = 9,
            codigo = "TST-001",
            descripcion = "Prueba",
            unidad = "pieza",
            precioUnitario = 10.005,
            cantidad = 3
        )
        val totales = Calculadora.calcular(listOf(entrada))
        assertEquals(30.02, totales.subtotal, 0.0001)
        assertEquals(4.80, totales.iva, 0.0001)
        assertEquals(34.82, totales.total, 0.0001)
    }

    @Test
    fun `una cotizacion emitida vence al dia siguiente de su caducidad`() {
        val hoy = LocalDate.of(2026, 11, 1)
        val ayer = hoy.minusDays(1)
        assertEquals(
            EstadoCotizacion.VENCIDA,
            estadoEfectivoCotizacion(EstadoCotizacion.EMITIDA, ayer, hoy)
        )
        assertEquals(
            EstadoCotizacion.EMITIDA,
            estadoEfectivoCotizacion(EstadoCotizacion.EMITIDA, hoy, hoy)
        )
        assertTrue(puedeConvertirCotizacion(EstadoCotizacion.EMITIDA, hoy, hoy))
        assertFalse(puedeConvertirCotizacion(EstadoCotizacion.EMITIDA, ayer, hoy))
        assertFalse(puedeConvertirCotizacion(EstadoCotizacion.CONVERTIDA, hoy, hoy))
    }
}
