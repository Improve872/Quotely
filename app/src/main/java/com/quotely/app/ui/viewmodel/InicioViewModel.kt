package com.quotely.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotely.app.data.repository.CotizacionRepository
import com.quotely.app.data.repository.PedidoRepository
import com.quotely.app.domain.EstadoCotizacion
import com.quotely.app.domain.EstadoPedido
import com.quotely.app.domain.diasParaVencer
import com.quotely.app.domain.estadoEfectivoCotizacion
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class TipoMovimiento { COTIZACION, PEDIDO }

data class MovimientoReciente(
    val tipo: TipoMovimiento,
    val id: Long,
    val folio: String,
    val cliente: String,
    val fecha: LocalDate,
    val total: Double,
    val creadoEn: Long,
    val estadoCotizacion: EstadoCotizacion? = null,
    val estadoPedido: EstadoPedido? = null
)

data class InicioUiState(
    val cargando: Boolean = true,
    val cotizacionesVigentes: Int = 0,
    val porVencer: Int = 0,
    val pedidosAbiertos: Int = 0,
    val totalMes: Double = 0.0,
    val recientes: List<MovimientoReciente> = emptyList()
)

class InicioViewModel(
    cotizacionRepository: CotizacionRepository,
    pedidoRepository: PedidoRepository
) : ViewModel() {

    val uiState = combine(
        cotizacionRepository.observarTodas(),
        pedidoRepository.observarTodos()
    ) { cotizaciones, pedidos ->
        val hoy = LocalDate.now()
        val efectivas = cotizaciones.map { cotizacion ->
            cotizacion to estadoEfectivoCotizacion(
                EstadoCotizacion.desde(cotizacion.estado),
                LocalDate.ofEpochDay(cotizacion.fechaCaducidad),
                hoy
            )
        }
        val vigentes = efectivas.count { it.second == EstadoCotizacion.EMITIDA }
        val porVencer = efectivas.count {
            it.second == EstadoCotizacion.EMITIDA &&
                diasParaVencer(LocalDate.ofEpochDay(it.first.fechaCaducidad), hoy) in 0..7
        }
        val totalMes = efectivas
            .filter { (cotizacion, estado) ->
                val fecha = LocalDate.ofEpochDay(cotizacion.fechaEmision)
                estado != EstadoCotizacion.BORRADOR &&
                    estado != EstadoCotizacion.ANULADA &&
                    fecha.year == hoy.year && fecha.month == hoy.month
            }
            .sumOf { it.first.total }
        val pedidosAbiertos = pedidos.count {
            EstadoPedido.desde(it.estado) in listOf(EstadoPedido.PENDIENTE, EstadoPedido.CONFIRMADO)
        }

        val movimientos = buildList {
            efectivas.forEach { (cotizacion, estado) ->
                add(
                    MovimientoReciente(
                        tipo = TipoMovimiento.COTIZACION,
                        id = cotizacion.id,
                        folio = cotizacion.folio,
                        cliente = cotizacion.cliente,
                        fecha = LocalDate.ofEpochDay(cotizacion.fechaEmision),
                        total = cotizacion.total,
                        creadoEn = cotizacion.creadoEn,
                        estadoCotizacion = estado
                    )
                )
            }
            pedidos.forEach { pedido ->
                add(
                    MovimientoReciente(
                        tipo = TipoMovimiento.PEDIDO,
                        id = pedido.id,
                        folio = pedido.folio,
                        cliente = pedido.cliente,
                        fecha = LocalDate.ofEpochDay(pedido.fecha),
                        total = pedido.total,
                        creadoEn = pedido.creadoEn,
                        estadoPedido = EstadoPedido.desde(pedido.estado)
                    )
                )
            }
        }.sortedByDescending { it.creadoEn }.take(6)

        InicioUiState(
            cargando = false,
            cotizacionesVigentes = vigentes,
            porVencer = porVencer,
            pedidosAbiertos = pedidosAbiertos,
            totalMes = totalMes,
            recientes = movimientos
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InicioUiState()
    )
}
