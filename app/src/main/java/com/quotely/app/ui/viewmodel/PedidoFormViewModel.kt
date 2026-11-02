package com.quotely.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotely.app.data.local.ProductoEntity
import com.quotely.app.data.repository.BorradorPedido
import com.quotely.app.data.repository.GuardadoPedido
import com.quotely.app.data.repository.PedidoRepository
import com.quotely.app.data.repository.ProductoRepository
import com.quotely.app.domain.Calculadora
import com.quotely.app.domain.LineaEntrada
import com.quotely.app.domain.TipoDescuento
import com.quotely.app.domain.Totales
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PedidoFormState(
    val cargando: Boolean = true,
    val cliente: String = "",
    val fecha: LocalDate = LocalDate.now(),
    val notas: String = "",
    val lineas: List<LineaFormulario> = emptyList(),
    val productos: List<ProductoEntity> = emptyList(),
    val guardando: Boolean = false,
    val error: String? = null
) {
    val totales: Totales
        get() = Calculadora.calcular(
            entradas = lineas.map { it.entrada },
            tipoDescuento = TipoDescuento.NINGUNO,
            aplicarDescuentos = false
        )
}

class PedidoFormViewModel(
    private val repo: PedidoRepository,
    productoRepo: ProductoRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PedidoFormState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            productoRepo.observarActivos().collect { productos ->
                _state.update { it.copy(productos = productos, cargando = false) }
            }
        }
    }

    fun actualizarCliente(valor: String) = _state.update { it.copy(cliente = valor, error = null) }

    fun actualizarNotas(valor: String) = _state.update { it.copy(notas = valor) }

    fun cambiarFecha(fecha: LocalDate) = _state.update { it.copy(fecha = fecha) }

    fun agregarProducto(producto: ProductoEntity) {
        _state.update { actual ->
            val existe = actual.lineas.any { it.clave == producto.codigo }
            val lineas = if (!existe) {
                actual.lineas + LineaFormulario(producto, 1)
            } else {
                actual.lineas.map {
                    if (it.clave == producto.codigo) {
                        it.copy(cantidad = (it.cantidad + 1).coerceAtMost(999))
                    } else {
                        it
                    }
                }
            }
            actual.copy(lineas = lineas, error = null)
        }
    }

    fun cambiarCantidad(codigo: String, cantidad: Int) {
        _state.update { actual ->
            actual.copy(
                lineas = actual.lineas.map {
                    if (it.clave == codigo) it.copy(cantidad = cantidad.coerceIn(1, 999)) else it
                },
                error = null
            )
        }
    }

    fun quitarLinea(codigo: String) {
        _state.update { actual ->
            actual.copy(lineas = actual.lineas.filterNot { it.clave == codigo }, error = null)
        }
    }

    fun guardar(responsableId: Long, onExito: (GuardadoPedido) -> Unit) {
        val actual = _state.value
        if (actual.guardando) return
        if (actual.cliente.trim().isBlank()) {
            _state.update { it.copy(error = "Escribe el nombre del cliente.") }
            return
        }
        if (actual.lineas.isEmpty()) {
            _state.update { it.copy(error = "Agrega al menos un producto.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(guardando = true, error = null) }
            runCatching {
                repo.crearDirecto(
                    BorradorPedido(
                        cliente = actual.cliente,
                        fecha = actual.fecha,
                        lineas = actual.lineas.map { it.entrada },
                        notas = actual.notas
                    ),
                    responsableId = responsableId
                )
            }.onSuccess { guardado ->
                _state.update { it.copy(guardando = false) }
                onExito(guardado)
            }.onFailure { fallo ->
                _state.update {
                    it.copy(
                        guardando = false,
                        error = fallo.message ?: "No se pudo registrar el pedido."
                    )
                }
            }
        }
    }

    fun limpiarError() = _state.update { it.copy(error = null) }
}
