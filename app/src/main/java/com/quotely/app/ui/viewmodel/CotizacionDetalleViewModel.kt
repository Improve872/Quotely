package com.quotely.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotely.app.data.local.CotizacionConItems
import com.quotely.app.data.local.PedidoEntity
import com.quotely.app.data.local.ResponsableEntity
import com.quotely.app.data.repository.AuthRepository
import com.quotely.app.data.repository.CotizacionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EventoCotizacion {
    data class Mensaje(val texto: String) : EventoCotizacion
    data class PedidoCreado(val pedidoId: Long) : EventoCotizacion
    data object Eliminada : EventoCotizacion
}

data class DetalleCotizacionState(
    val cargando: Boolean = true,
    val detalle: CotizacionConItems? = null,
    val pedido: PedidoEntity? = null,
    val responsable: ResponsableEntity? = null,
    val trabajando: Boolean = false
)

class CotizacionDetalleViewModel(
    private val repo: CotizacionRepository,
    private val authRepo: AuthRepository,
    private val cotizacionId: Long
) : ViewModel() {

    private val _trabajando = MutableStateFlow(false)
    private val _eventos = MutableSharedFlow<EventoCotizacion>(extraBufferCapacity = 4)
    val eventos = _eventos.asSharedFlow()

    val uiState = combine(
        repo.observarDetalle(cotizacionId),
        repo.observarPedidoDeCotizacion(cotizacionId),
        _trabajando
    ) { detalle, pedido, trabajando ->
        DetalleCotizacionState(
            cargando = false,
            detalle = detalle,
            pedido = pedido,
            responsable = detalle?.let { authRepo.buscarPorId(it.cotizacion.responsableId) },
            trabajando = trabajando
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetalleCotizacionState()
    )

    fun emitir() = ejecutar {
        repo.emitir(cotizacionId)
        "Cotización emitida. Ya se puede convertir en pedido."
    }

    fun anular() = ejecutar {
        repo.anular(cotizacionId)
        "Cotización anulada."
    }

    fun eliminarBorrador() {
        if (_trabajando.value) return
        viewModelScope.launch {
            _trabajando.value = true
            runCatching { repo.eliminarBorrador(cotizacionId) }
                .onSuccess { _eventos.emit(EventoCotizacion.Eliminada) }
                .onFailure {
                    _eventos.emit(
                        EventoCotizacion.Mensaje(it.message ?: "No se pudo eliminar el borrador.")
                    )
                }
            _trabajando.value = false
        }
    }

    fun convertirAPedido(responsableId: Long) {
        if (_trabajando.value) return
        viewModelScope.launch {
            _trabajando.value = true
            runCatching { repo.convertirAPedido(cotizacionId, responsableId) }
                .onSuccess { pedidoId ->
                    _eventos.emit(EventoCotizacion.PedidoCreado(pedidoId))
                }
                .onFailure {
                    _eventos.emit(
                        EventoCotizacion.Mensaje(it.message ?: "No se pudo crear el pedido.")
                    )
                }
            _trabajando.value = false
        }
    }

    private fun ejecutar(accion: suspend () -> String) {
        if (_trabajando.value) return
        viewModelScope.launch {
            _trabajando.value = true
            runCatching { accion() }
                .onSuccess { _eventos.emit(EventoCotizacion.Mensaje(it)) }
                .onFailure {
                    _eventos.emit(
                        EventoCotizacion.Mensaje(it.message ?: "No se pudo completar la acción.")
                    )
                }
            _trabajando.value = false
        }
    }
}
