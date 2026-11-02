package com.quotely.app.data.repository

import androidx.room.withTransaction
import com.quotely.app.data.local.CotizacionConItems
import com.quotely.app.data.local.CotizacionEntity
import com.quotely.app.data.local.CotizacionItemEntity
import com.quotely.app.data.local.QuotelyDatabase
import com.quotely.app.domain.Calculadora
import com.quotely.app.domain.EstadoCotizacion
import com.quotely.app.domain.EstadoPedido
import com.quotely.app.domain.LineaEntrada
import com.quotely.app.domain.TipoDescuento
import com.quotely.app.domain.estadoEfectivoCotizacion
import com.quotely.app.domain.puedeConvertirCotizacion
import com.quotely.app.data.local.PedidoEntity
import com.quotely.app.data.local.PedidoItemEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

internal object Folios {
    fun prefijo(tipo: String, fecha: LocalDate = LocalDate.now()): String =
        "$tipo-${fecha.year}-"

    fun siguiente(ultimo: String?, prefijo: String): String {
        val numero = ultimo?.substringAfterLast('-')?.toIntOrNull()?.plus(1) ?: 1
        return prefijo + numero.toString().padStart(4, '0')
    }
}

data class BorradorCotizacion(
    val id: Long = 0,
    val cliente: String,
    val contacto: String,
    val emision: LocalDate,
    val caducidad: LocalDate,
    val lineas: List<LineaEntrada>,
    val tipoDescuento: TipoDescuento,
    val valorDescuento: Double,
    val notas: String,
    val estado: EstadoCotizacion
)

data class GuardadoCotizacion(val id: Long, val folio: String, val estado: EstadoCotizacion)

class CotizacionRepository(private val db: QuotelyDatabase) {
    private val dao = db.cotizacionDao()
    private val pedidoDao = db.pedidoDao()

    fun observarTodas(): Flow<List<CotizacionEntity>> = dao.observarTodas()

    fun observarDetalle(id: Long): Flow<CotizacionConItems?> = dao.observarDetalle(id)

    fun observarPedidoDeCotizacion(cotizacionId: Long): Flow<PedidoEntity?> =
        pedidoDao.observarPorCotizacion(cotizacionId)

    suspend fun obtenerDetalle(id: Long): CotizacionConItems? = dao.obtenerDetalle(id)

    suspend fun guardar(borrador: BorradorCotizacion, responsableId: Long): GuardadoCotizacion {
        validar(borrador)
        val totales = Calculadora.calcular(
            entradas = borrador.lineas,
            tipoDescuento = borrador.tipoDescuento,
            valorDescuento = borrador.valorDescuento
        )
        val estado = when (borrador.estado) {
            EstadoCotizacion.EMITIDA -> EstadoCotizacion.EMITIDA
            else -> EstadoCotizacion.BORRADOR
        }

        return db.withTransaction {
            val ahora = System.currentTimeMillis()
            val existente = if (borrador.id != 0L) dao.obtenerDetalle(borrador.id) else null
            if (borrador.id != 0L && existente == null) {
                throw IllegalArgumentException("La cotización ya no existe.")
            }
            if (existente != null && EstadoCotizacion.desde(existente.cotizacion.estado) != EstadoCotizacion.BORRADOR) {
                throw IllegalStateException("Solo se puede editar una cotización en borrador.")
            }

            val folio = existente?.cotizacion?.folio ?: run {
                val prefijo = Folios.prefijo("COT", borrador.emision)
                Folios.siguiente(dao.ultimoFolio(prefijo), prefijo)
            }
            val id = if (existente == null) {
                dao.insertar(
                    CotizacionEntity(
                        folio = folio,
                        cliente = borrador.cliente.trim(),
                        clienteContacto = borrador.contacto.trim(),
                        fechaEmision = borrador.emision.toEpochDay(),
                        fechaCaducidad = borrador.caducidad.toEpochDay(),
                        responsableId = responsableId,
                        estado = estado.name,
                        subtotal = totales.subtotal,
                        descuentoVolumen = totales.descuentoVolumen,
                        descuentoGlobal = totales.descuentoGlobal,
                        tipoDescuentoGlobal = borrador.tipoDescuento.name,
                        valorDescuentoGlobal = borrador.valorDescuento,
                        iva = totales.iva,
                        total = totales.total,
                        notas = borrador.notas.trim(),
                        creadoEn = ahora,
                        actualizadoEn = ahora
                    )
                )
            } else {
                dao.actualizar(
                    existente.cotizacion.copy(
                        cliente = borrador.cliente.trim(),
                        clienteContacto = borrador.contacto.trim(),
                        fechaEmision = borrador.emision.toEpochDay(),
                        fechaCaducidad = borrador.caducidad.toEpochDay(),
                        estado = estado.name,
                        subtotal = totales.subtotal,
                        descuentoVolumen = totales.descuentoVolumen,
                        descuentoGlobal = totales.descuentoGlobal,
                        tipoDescuentoGlobal = borrador.tipoDescuento.name,
                        valorDescuentoGlobal = borrador.valorDescuento,
                        iva = totales.iva,
                        total = totales.total,
                        notas = borrador.notas.trim(),
                        actualizadoEn = ahora
                    )
                )
                dao.borrarItems(existente.cotizacion.id)
                existente.cotizacion.id
            }

            dao.insertarItems(
                totales.lineas.mapIndexed { indice, linea ->
                    CotizacionItemEntity(
                        cotizacionId = id,
                        productoId = linea.productoId,
                        codigo = linea.codigo,
                        descripcion = linea.descripcion,
                        unidad = linea.unidad,
                        precioUnitario = linea.precioUnitario,
                        cantidad = linea.cantidad,
                        descuentoVolumenPct = linea.descuentoVolumenPct,
                        descuentoVolumen = linea.descuentoVolumen,
                        importe = linea.importe,
                        orden = indice
                    )
                }
            )

            GuardadoCotizacion(id, folio, estado)
        }
    }

    suspend fun emitir(id: Long) {
        val detalle = dao.obtenerDetalle(id) ?: throw IllegalArgumentException("La cotización no existe.")
        if (EstadoCotizacion.desde(detalle.cotizacion.estado) != EstadoCotizacion.BORRADOR) {
            throw IllegalStateException("Solo un borrador se puede emitir.")
        }
        if (detalle.items.isEmpty()) {
            throw IllegalStateException("Agrega al menos un producto antes de emitir.")
        }
        dao.actualizarEstado(id, EstadoCotizacion.EMITIDA.name, System.currentTimeMillis())
    }

    suspend fun anular(id: Long) {
        val detalle = dao.obtenerDetalle(id) ?: throw IllegalArgumentException("La cotización no existe.")
        val estado = EstadoCotizacion.desde(detalle.cotizacion.estado)
        if (estado == EstadoCotizacion.CONVERTIDA) {
            throw IllegalStateException("No se puede anular una cotización que ya generó un pedido.")
        }
        if (estado == EstadoCotizacion.ANULADA) {
            throw IllegalStateException("La cotización ya está anulada.")
        }
        dao.actualizarEstado(id, EstadoCotizacion.ANULADA.name, System.currentTimeMillis())
    }

    suspend fun eliminarBorrador(id: Long) {
        val filas = dao.eliminarBorrador(id)
        if (filas == 0) throw IllegalStateException("Solo se puede eliminar un borrador.")
    }

    suspend fun convertirAPedido(cotizacionId: Long, responsableId: Long): Long =
        db.withTransaction {
            val detalle = dao.obtenerDetalle(cotizacionId)
                ?: throw IllegalArgumentException("La cotización no existe.")
            val estado = EstadoCotizacion.desde(detalle.cotizacion.estado)
            val caducidad = LocalDate.ofEpochDay(detalle.cotizacion.fechaCaducidad)

            if (estado == EstadoCotizacion.CONVERTIDA || pedidoDao.buscarPorCotizacion(cotizacionId) != null) {
                throw IllegalStateException("Esta cotización ya tiene un pedido asociado.")
            }
            if (estado != EstadoCotizacion.EMITIDA) {
                throw IllegalStateException("Solo una cotización emitida se puede convertir en pedido.")
            }
            if (!puedeConvertirCotizacion(estado, caducidad)) {
                throw IllegalStateException("La cotización está vencida. Crea un pedido directo con precios base.")
            }

            val ahora = System.currentTimeMillis()
            val prefijo = Folios.prefijo("PED")
            val folio = Folios.siguiente(pedidoDao.ultimoFolio(prefijo), prefijo)
            val pedidoId = pedidoDao.insertar(
                PedidoEntity(
                    folio = folio,
                    cliente = detalle.cotizacion.cliente,
                    fecha = LocalDate.now().toEpochDay(),
                    responsableId = responsableId,
                    cotizacionId = cotizacionId,
                    origen = "COTIZACION",
                    estado = EstadoPedido.PENDIENTE.name,
                    subtotal = detalle.cotizacion.subtotal,
                    descuentoVolumen = detalle.cotizacion.descuentoVolumen,
                    descuentoGlobal = detalle.cotizacion.descuentoGlobal,
                    tipoDescuentoGlobal = detalle.cotizacion.tipoDescuentoGlobal,
                    valorDescuentoGlobal = detalle.cotizacion.valorDescuentoGlobal,
                    iva = detalle.cotizacion.iva,
                    total = detalle.cotizacion.total,
                    notas = detalle.cotizacion.notas,
                    creadoEn = ahora,
                    actualizadoEn = ahora
                )
            )
            pedidoDao.insertarItems(
                detalle.itemsOrdenados.mapIndexed { indice, item ->
                    PedidoItemEntity(
                        pedidoId = pedidoId,
                        productoId = item.productoId,
                        codigo = item.codigo,
                        descripcion = item.descripcion,
                        unidad = item.unidad,
                        precioUnitario = item.precioUnitario,
                        cantidad = item.cantidad,
                        descuentoVolumenPct = item.descuentoVolumenPct,
                        descuentoVolumen = item.descuentoVolumen,
                        importe = item.importe,
                        orden = indice
                    )
                }
            )
            dao.actualizarEstado(
                cotizacionId,
                EstadoCotizacion.CONVERTIDA.name,
                ahora
            )
            pedidoId
        }

    private fun validar(borrador: BorradorCotizacion) {
        if (borrador.cliente.trim().isBlank()) {
            throw IllegalArgumentException("Escribe el nombre del cliente.")
        }
        if (borrador.lineas.isEmpty()) {
            throw IllegalArgumentException("Agrega al menos un producto.")
        }
        if (borrador.caducidad.isBefore(borrador.emision)) {
            throw IllegalArgumentException("La fecha de caducidad no puede ser anterior a la emisión.")
        }
        if (borrador.lineas.any { it.cantidad <= 0 }) {
            throw IllegalArgumentException("Cada producto debe tener una cantidad mayor a cero.")
        }
        if (borrador.tipoDescuento == TipoDescuento.PORCENTAJE &&
            borrador.valorDescuento !in 0.0..100.0
        ) {
            throw IllegalArgumentException("El descuento porcentual debe estar entre 0 y 100.")
        }
        if (borrador.tipoDescuento == TipoDescuento.MONTO && borrador.valorDescuento < 0) {
            throw IllegalArgumentException("El descuento no puede ser negativo.")
        }
    }
}
