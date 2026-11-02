package com.quotely.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotely.app.data.local.ProductoEntity
import com.quotely.app.data.repository.BorradorCotizacion
import com.quotely.app.data.repository.CotizacionRepository
import com.quotely.app.data.repository.GuardadoCotizacion
import com.quotely.app.data.repository.ProductoRepository
import com.quotely.app.domain.Calculadora
import com.quotely.app.domain.EstadoCotizacion
import com.quotely.app.domain.LineaEntrada
import com.quotely.app.domain.PricingConfig
import com.quotely.app.domain.TipoDescuento
import com.quotely.app.domain.Totales
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LineaFormulario(
    val producto: ProductoEntity,
    val cantidad: Int
) {
    val clave: String get() = producto.codigo
    val entrada: LineaEntrada
        get() = LineaEntrada(
            productoId = producto.id.takeIf { it > 0 },
            codigo = producto.codigo,
            descripcion = producto.nombre,
            unidad = producto.unidad,
            precioUnitario = producto.precioBase,
            cantidad = cantidad
        )
}

data class CotizacionFormState(
    val cargando: Boolean = true,
    val id: Long = 0,
    val folio: String = "",
    val cliente: String = "",
    val contacto: String = "",
    val emision: LocalDate = LocalDate.now(),
    val caducidad: LocalDate = LocalDate.now().plusDays(PricingConfig.DIAS_VIGENCIA_POR_DEFECTO.toLong()),
    val notas: String = "",
    val tipoDescuento: TipoDescuento = TipoDescuento.NINGUNO,
    val valorDescuento: String = "",
    val lineas: List<LineaFormulario> = emptyList(),
    val productos: List<ProductoEntity> = emptyList(),
    val guardando: Boolean = false,
    val error: String? = null
) {
    val valorDescuentoNumero: Double
        get() = valorDescuento.replace(',', '.').toDoubleOrNull() ?: 0.0

    val totales: Totales
        get() = Calculadora.calcular(
            entradas = lineas.map { it.entrada },
            tipoDescuento = tipoDescuento,
            valorDescuento = valorDescuentoNumero
        )

    val esEdicion: Boolean get() = id > 0
}

class CotizacionFormViewModel(
    private val repo: CotizacionRepository,
    private val productoRepo: ProductoRepository,
    private val cotizacionId: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(CotizacionFormState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            productoRepo.observarActivos().collect { productos ->
                _state.update { actual ->
                    actual.copy(
                        productos = productos,
                        cargando = if (cotizacionId == null) false else actual.cargando
                    )
                }
            }
        }
        if (cotizacionId != null) {
            viewModelScope.launch { cargar(cotizacionId) }
        }
    }

    fun actualizarCliente(valor: String) = _state.update { it.copy(cliente = valor, error = null) }

    fun actualizarContacto(valor: String) = _state.update { it.copy(contacto = valor) }

    fun actualizarNotas(valor: String) = _state.update { it.copy(notas = valor) }

    fun cambiarEmision(fecha: LocalDate) = _state.update { actual ->
        val nuevaCaducidad = if (actual.caducidad.isBefore(fecha)) {
            fecha.plusDays(PricingConfig.DIAS_VIGENCIA_POR_DEFECTO.toLong())
        } else {
            actual.caducidad
        }
        actual.copy(emision = fecha, caducidad = nuevaCaducidad, error = null)
    }

    fun cambiarCaducidad(fecha: LocalDate) = _state.update { it.copy(caducidad = fecha, error = null) }

    fun cambiarTipoDescuento(tipo: TipoDescuento) = _state.update {
        it.copy(
            tipoDescuento = tipo,
            valorDescuento = if (tipo == TipoDescuento.NINGUNO) "" else it.valorDescuento,
            error = null
        )
    }

    fun actualizarValorDescuento(valor: String) {
        val normalizado = valor.replace(',', '.')
        if (normalizado.isEmpty() || normalizado.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) {
            _state.update { it.copy(valorDescuento = normalizado, error = null) }
        }
    }

    fun agregarProducto(producto: ProductoEntity) {
        _state.update { actual ->
            val existente = actual.lineas.firstOrNull { it.clave == producto.codigo }
            val lineas = if (existente == null) {
                actual.lineas + LineaFormulario(producto, 1)
            } else {
                actual.lineas.map {
                    if (it.clave == producto.codigo) it.copy(cantidad = (it.cantidad + 1).coerceAtMost(999)) else it
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

    fun guardar(
        emitir: Boolean,
        responsableId: Long,
        onExito: (GuardadoCotizacion) -> Unit
    ) {
        val actual = _state.value
        if (actual.guardando) return
        val error = validar(actual)
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(guardando = true, error = null) }
            runCatching {
                repo.guardar(
                    BorradorCotizacion(
                        id = actual.id,
                        cliente = actual.cliente,
                        contacto = actual.contacto,
                        emision = actual.emision,
                        caducidad = actual.caducidad,
                        lineas = actual.lineas.map { it.entrada },
                        tipoDescuento = actual.tipoDescuento,
                        valorDescuento = actual.valorDescuentoNumero,
                        notas = actual.notas,
                        estado = if (emitir) EstadoCotizacion.EMITIDA else EstadoCotizacion.BORRADOR
                    ),
                    responsableId = responsableId
                )
            }.onSuccess { guardado ->
                _state.update {
                    it.copy(
                        guardando = false,
                        id = guardado.id,
                        folio = guardado.folio
                    )
                }
                onExito(guardado)
            }.onFailure { fallo ->
                _state.update {
                    it.copy(
                        guardando = false,
                        error = fallo.message ?: "No se pudo guardar la cotización."
                    )
                }
            }
        }
    }

    fun limpiarError() = _state.update { it.copy(error = null) }

    private suspend fun cargar(id: Long) {
        val detalle = repo.obtenerDetalle(id)
        if (detalle == null) {
            _state.update { it.copy(cargando = false, error = "La cotización ya no existe.") }
            return
        }
        if (EstadoCotizacion.desde(detalle.cotizacion.estado) != EstadoCotizacion.BORRADOR) {
            _state.update {
                it.copy(
                    cargando = false,
                    error = "Solo las cotizaciones en borrador se pueden editar."
                )
            }
            return
        }
        val lineas = detalle.itemsOrdenados.map { item ->
            val productoActual = item.productoId?.let { productoRepo.buscarPorId(it) }
            val producto = (productoActual ?: ProductoEntity(
                id = item.productoId ?: 0,
                codigo = item.codigo,
                nombre = item.descripcion,
                precioBase = item.precioUnitario,
                unidad = item.unidad,
                activo = false
            )).copy(
                codigo = item.codigo,
                nombre = item.descripcion,
                unidad = item.unidad,
                precioBase = item.precioUnitario
            )
            LineaFormulario(producto, item.cantidad)
        }
        val cotizacion = detalle.cotizacion
        _state.update {
            it.copy(
                cargando = false,
                id = cotizacion.id,
                folio = cotizacion.folio,
                cliente = cotizacion.cliente,
                contacto = cotizacion.clienteContacto,
                emision = LocalDate.ofEpochDay(cotizacion.fechaEmision),
                caducidad = LocalDate.ofEpochDay(cotizacion.fechaCaducidad),
                notas = cotizacion.notas,
                tipoDescuento = TipoDescuento.desde(cotizacion.tipoDescuentoGlobal),
                valorDescuento = if (cotizacion.valorDescuentoGlobal > 0) {
                    cotizacion.valorDescuentoGlobal
                        .toBigDecimal().stripTrailingZeros().toPlainString()
                } else {
                    ""
                },
                lineas = lineas,
                error = null
            )
        }
    }

    private fun validar(estado: CotizacionFormState): String? {
        if (estado.cliente.trim().isBlank()) return "Escribe el nombre del cliente."
        if (estado.lineas.isEmpty()) return "Agrega al menos un producto."
        if (estado.caducidad.isBefore(estado.emision)) {
            return "La caducidad no puede ser anterior a la emisión."
        }
        if (estado.tipoDescuento == TipoDescuento.PORCENTAJE &&
            estado.valorDescuentoNumero !in 0.0..100.0
        ) {
            return "El porcentaje debe estar entre 0 y 100."
        }
        if (estado.tipoDescuento == TipoDescuento.MONTO && estado.valorDescuentoNumero < 0) {
            return "El monto del descuento no puede ser negativo."
        }
        return null
    }
}
