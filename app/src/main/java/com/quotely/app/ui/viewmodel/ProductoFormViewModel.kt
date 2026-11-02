package com.quotely.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotely.app.data.local.ProductoEntity
import com.quotely.app.data.repository.ProductoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductoFormState(
    val cargando: Boolean = true,
    val id: Long = 0,
    val codigo: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val precio: String = "",
    val unidad: String = "pieza",
    val activo: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null
) {
    val esEdicion: Boolean get() = id > 0
    val precioNumero: Double get() = precio.replace(',', '.').toDoubleOrNull() ?: -1.0
}

class ProductoFormViewModel(
    private val repo: ProductoRepository,
    private val productoId: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(ProductoFormState(cargando = productoId != null))
    val state = _state.asStateFlow()

    init {
        if (productoId != null) {
            viewModelScope.launch {
                val producto = repo.buscarPorId(productoId)
                if (producto == null) {
                    _state.update { it.copy(cargando = false, error = "El producto ya no existe.") }
                } else {
                    _state.update {
                        it.copy(
                            cargando = false,
                            id = producto.id,
                            codigo = producto.codigo,
                            nombre = producto.nombre,
                            descripcion = producto.descripcion,
                            precio = producto.precioBase.toBigDecimal().stripTrailingZeros().toPlainString(),
                            unidad = producto.unidad,
                            activo = producto.activo
                        )
                    }
                }
            }
        }
    }

    fun actualizarCodigo(valor: String) = _state.update {
        it.copy(codigo = valor.uppercase(), error = null)
    }

    fun actualizarNombre(valor: String) = _state.update { it.copy(nombre = valor, error = null) }

    fun actualizarDescripcion(valor: String) = _state.update { it.copy(descripcion = valor) }

    fun actualizarPrecio(valor: String) {
        val normalizado = valor.replace(',', '.')
        if (normalizado.isEmpty() || normalizado.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) {
            _state.update { it.copy(precio = normalizado, error = null) }
        }
    }

    fun actualizarUnidad(valor: String) = _state.update { it.copy(unidad = valor) }

    fun actualizarActivo(valor: Boolean) = _state.update { it.copy(activo = valor) }

    fun guardar(onExito: (Long) -> Unit) {
        val actual = _state.value
        if (actual.guardando) return
        if (actual.precioNumero < 0) {
            _state.update { it.copy(error = "Escribe un precio válido.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(guardando = true, error = null) }
            runCatching {
                repo.guardar(
                    ProductoEntity(
                        id = actual.id,
                        codigo = actual.codigo,
                        nombre = actual.nombre,
                        descripcion = actual.descripcion,
                        precioBase = actual.precioNumero,
                        unidad = actual.unidad,
                        activo = actual.activo
                    )
                )
            }.onSuccess { id ->
                _state.update { it.copy(guardando = false, id = id) }
                onExito(id)
            }.onFailure { fallo ->
                _state.update {
                    it.copy(
                        guardando = false,
                        error = fallo.message ?: "No se pudo guardar el producto."
                    )
                }
            }
        }
    }

    fun limpiarError() = _state.update { it.copy(error = null) }
}
