package com.quotely.app.data.repository

import androidx.room.withTransaction
import com.quotely.app.data.local.PedidoConItems
import com.quotely.app.data.local.PedidoEntity
import com.quotely.app.data.local.PedidoItemEntity
import com.quotely.app.data.local.QuotelyDatabase
import com.quotely.app.domain.Calculadora
import com.quotely.app.domain.EstadoPedido
import com.quotely.app.domain.LineaEntrada
import com.quotely.app.domain.TipoDescuento
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class BorradorPedido(
    val cliente: String,
    val fecha: LocalDate,
    val lineas: List<LineaEntrada>,
    val notas: String
)

data class GuardadoPedido(val id: Long, val folio: String)

class PedidoRepository(private val db: QuotelyDatabase) {
    private val dao = db.pedidoDao()

    fun observarTodos(): Flow<List<PedidoEntity>> = dao.observarTodos()

    fun observarDetalle(id: Long): Flow<PedidoConItems?> = dao.observarDetalle(id)

    suspend fun obtenerDetalle(id: Long): PedidoConItems? = dao.obtenerDetalle(id)

    suspend fun crearDirecto(borrador: BorradorPedido, responsableId: Long): GuardadoPedido {
        if (borrador.cliente.trim().isBlank()) {
            throw IllegalArgumentException("Escribe el nombre del cliente.")
        }
        if (borrador.lineas.isEmpty()) {
            throw IllegalArgumentException("Agrega al menos un producto.")
        }
        if (borrador.lineas.any { it.cantidad <= 0 }) {
            throw IllegalArgumentException("Cada producto debe tener una cantidad mayor a cero.")
        }

        val totales = Calculadora.calcular(
            entradas = borrador.lineas,
            tipoDescuento = TipoDescuento.NINGUNO,
            aplicarDescuentos = false
        )

        return db.withTransaction {
            val ahora = System.currentTimeMillis()
            val prefijo = Folios.prefijo("PED", borrador.fecha)
            val folio = Folios.siguiente(dao.ultimoFolio(prefijo), prefijo)
            val id = dao.insertar(
                PedidoEntity(
                    folio = folio,
                    cliente = borrador.cliente.trim(),
                    fecha = borrador.fecha.toEpochDay(),
                    responsableId = responsableId,
                    cotizacionId = null,
                    origen = "DIRECTO",
                    estado = EstadoPedido.PENDIENTE.name,
                    subtotal = totales.subtotal,
                    descuentoVolumen = 0.0,
                    descuentoGlobal = 0.0,
                    tipoDescuentoGlobal = TipoDescuento.NINGUNO.name,
                    valorDescuentoGlobal = 0.0,
                    iva = totales.iva,
                    total = totales.total,
                    notas = borrador.notas.trim(),
                    creadoEn = ahora,
                    actualizadoEn = ahora
                )
            )
            dao.insertarItems(
                totales.lineas.mapIndexed { indice, linea ->
                    PedidoItemEntity(
                        pedidoId = id,
                        productoId = linea.productoId,
                        codigo = linea.codigo,
                        descripcion = linea.descripcion,
                        unidad = linea.unidad,
                        precioUnitario = linea.precioUnitario,
                        cantidad = linea.cantidad,
                        descuentoVolumenPct = 0.0,
                        descuentoVolumen = 0.0,
                        importe = linea.importe,
                        orden = indice
                    )
                }
            )
            GuardadoPedido(id, folio)
        }
    }

    suspend fun cambiarEstado(id: Long, nuevo: EstadoPedido) {
        val pedido = dao.obtenerDetalle(id) ?: throw IllegalArgumentException("El pedido no existe.")
        val actual = EstadoPedido.desde(pedido.pedido.estado)
        val permitidos = when (actual) {
            EstadoPedido.PENDIENTE -> setOf(EstadoPedido.CONFIRMADO, EstadoPedido.CANCELADO)
            EstadoPedido.CONFIRMADO -> setOf(EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO)
            EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO -> emptySet()
        }
        if (nuevo !in permitidos) {
            throw IllegalStateException("No se puede pasar de ${actual.etiqueta} a ${nuevo.etiqueta}.")
        }
        dao.actualizarEstado(id, nuevo.name, System.currentTimeMillis())
    }
}
