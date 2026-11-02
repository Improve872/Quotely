package com.quotely.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotely.app.data.local.ProductoEntity
import com.quotely.app.data.repository.ProductoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductosUiState(
    val cargando: Boolean = true,
    val items: List<ProductoEntity> = emptyList(),
    val total: Int = 0,
    val activos: Int = 0
)

class ProductosViewModel(private val repo: ProductoRepository) : ViewModel() {
    val busqueda = MutableStateFlow("")
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje = _mensaje.asStateFlow()

    val uiState = combine(repo.observarTodos(), busqueda) { productos, consulta ->
        val q = consulta.trim()
        val visibles = productos.filter {
            q.isBlank() ||
                it.nombre.contains(q, ignoreCase = true) ||
                it.codigo.contains(q, ignoreCase = true)
        }
        ProductosUiState(
            cargando = false,
            items = visibles,
            total = productos.size,
            activos = productos.count { it.activo }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProductosUiState()
    )

    fun cambiarBusqueda(valor: String) {
        busqueda.value = valor
    }

    fun alternarActivo(producto: ProductoEntity) {
        viewModelScope.launch {
            runCatching { repo.alternarActivo(producto) }
                .onFailure { _mensaje.value = it.message ?: "No se pudo actualizar el producto." }
        }
    }

    fun consumirMensaje() {
        _mensaje.value = null
    }
}
