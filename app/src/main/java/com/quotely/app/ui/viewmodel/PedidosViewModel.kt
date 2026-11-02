package com.quotely.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotely.app.data.local.PedidoEntity
import com.quotely.app.data.repository.PedidoRepository
import com.quotely.app.domain.EstadoPedido
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class FiltroPedido(val etiqueta: String) {
    TODOS("Todos"),
    PENDIENTES("Pendientes"),
    CONFIRMADOS("Confirmados"),
    ENTREGADOS("Entregados"),
    CANCELADOS("Cancelados")
}

data class PedidoListaItem(
    val pedido: PedidoEntity,
    val estado: EstadoPedido
)

data class PedidosUiState(
    val cargando: Boolean = true,
    val items: List<PedidoListaItem> = emptyList(),
    val conteos: Map<FiltroPedido, Int> = emptyMap(),
    val total: Int = 0
)

class PedidosViewModel(repo: PedidoRepository) : ViewModel() {
    val busqueda = MutableStateFlow("")
    val filtro = MutableStateFlow(FiltroPedido.TODOS)

    val uiState = combine(
        repo.observarTodos(),
        busqueda,
        filtro
    ) { pedidos, consulta, seleccionado ->
        val todos = pedidos.map { PedidoListaItem(it, EstadoPedido.desde(it.estado)) }
        val conteos = FiltroPedido.entries.associateWith { filtro ->
            todos.count { coincideFiltro(it, filtro) }
        }
        val q = consulta.trim()
        val visibles = todos.filter { item ->
            coincideFiltro(item, seleccionado) &&
                (q.isBlank() ||
                    item.pedido.folio.contains(q, ignoreCase = true) ||
                    item.pedido.cliente.contains(q, ignoreCase = true))
        }
        PedidosUiState(false, visibles, conteos, todos.size)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PedidosUiState()
    )

    fun cambiarBusqueda(valor: String) {
        busqueda.value = valor
    }

    fun cambiarFiltro(valor: FiltroPedido) {
        filtro.value = valor
    }

    private fun coincideFiltro(item: PedidoListaItem, filtro: FiltroPedido): Boolean = when (filtro) {
        FiltroPedido.TODOS -> true
        FiltroPedido.PENDIENTES -> item.estado == EstadoPedido.PENDIENTE
        FiltroPedido.CONFIRMADOS -> item.estado == EstadoPedido.CONFIRMADO
        FiltroPedido.ENTREGADOS -> item.estado == EstadoPedido.ENTREGADO
        FiltroPedido.CANCELADOS -> item.estado == EstadoPedido.CANCELADO
    }
}
