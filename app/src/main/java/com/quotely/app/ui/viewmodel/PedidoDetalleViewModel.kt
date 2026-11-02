package com.quotely.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotely.app.data.local.PedidoConItems
import com.quotely.app.data.local.ResponsableEntity
import com.quotely.app.data.repository.AuthRepository
import com.quotely.app.data.repository.CotizacionRepository
import com.quotely.app.data.repository.PedidoRepository
import com.quotely.app.domain.EstadoPedido
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EventoPedido {
    data class Mensaje(val texto: String) : EventoPedido
}

data class DetallePedidoState(
    val cargando: Boolean = true,
    val detalle: PedidoConItems? = null,
    val responsable: ResponsableEntity? = null,
    val folioCotizacion: String? = null,
    val trabajando: Boolean = false
)

class PedidoDetalleViewModel(
    private val repo: PedidoRepository,
    private val cotizacionRepo: CotizacionRepository,
    private val authRepo: AuthRepository,
    private val pedidoId: Long
) : ViewModel() {

    private val _trabajando = MutableStateFlow(false)
    private val _eventos = MutableSharedFlow<EventoPedido>(extraBufferCapacity = 4)
    val eventos = _eventos.asSharedFlow()

    val uiState = combine(
        repo.observarDetalle(pedidoId),
        _trabajando
    ) { detalle, trabajando ->
        DetallePedidoState(
            cargando = false,
            detalle = detalle,
            responsable = detalle?.let { authRepo.buscarPorId(it.pedido.responsableId) },
            folioCotizacion = detalle?.pedido?.cotizacionId
                ?.let { cotizacionRepo.obtenerDetalle(it)?.cotizacion?.folio },
            trabajando = trabajando
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetallePedidoState()
    )

    fun cambiarEstado(nuevo: EstadoPedido) {
        if (_trabajando.value) return
        viewModelScope.launch {
            _trabajando.value = true
            runCatching { repo.cambiarEstado(pedidoId, nuevo) }
                .onSuccess {
                    _eventos.emit(EventoPedido.Mensaje("Pedido actualizado a ${nuevo.etiqueta.lowercase()}."))
                }
                .onFailure {
                    _eventos.emit(
                        EventoPedido.Mensaje(it.message ?: "No se pudo cambiar el estado.")
                    )
                }
            _trabajando.value = false
        }
    }
}
