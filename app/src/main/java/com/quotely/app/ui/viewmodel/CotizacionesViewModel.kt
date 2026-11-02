package com.quotely.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotely.app.data.local.CotizacionEntity
import com.quotely.app.data.repository.CotizacionRepository
import com.quotely.app.domain.EstadoCotizacion
import com.quotely.app.domain.diasParaVencer
import com.quotely.app.domain.estadoEfectivoCotizacion
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class FiltroCotizacion(val etiqueta: String) {
    TODAS("Todas"),
    BORRADORES("Borradores"),
    VIGENTES("Vigentes"),
    POR_VENCER("Por vencer"),
    VENCIDAS("Vencidas"),
    CONVERTIDAS("Convertidas"),
    ANULADAS("Anuladas")
}

data class CotizacionListaItem(
    val cotizacion: CotizacionEntity,
    val estado: EstadoCotizacion,
    val diasRestantes: Long
)

data class CotizacionesUiState(
    val cargando: Boolean = true,
    val items: List<CotizacionListaItem> = emptyList(),
    val conteos: Map<FiltroCotizacion, Int> = emptyMap(),
    val total: Int = 0
)

class CotizacionesViewModel(repo: CotizacionRepository) : ViewModel() {
    val busqueda = MutableStateFlow("")
    val filtro = MutableStateFlow(FiltroCotizacion.TODAS)

    val uiState = combine(
        repo.observarTodas(),
        busqueda,
        filtro
    ) { cotizaciones, consulta, seleccionado ->
        val hoy = LocalDate.now()
        val todas = cotizaciones.map { cotizacion ->
            val estado = estadoEfectivoCotizacion(
                EstadoCotizacion.desde(cotizacion.estado),
                LocalDate.ofEpochDay(cotizacion.fechaCaducidad),
                hoy
            )
            CotizacionListaItem(
                cotizacion = cotizacion,
                estado = estado,
                diasRestantes = diasParaVencer(LocalDate.ofEpochDay(cotizacion.fechaCaducidad), hoy)
            )
        }
        val conteos = FiltroCotizacion.entries.associateWith { filtro ->
            todas.count { coincideFiltro(it, filtro) }
        }
        val q = consulta.trim()
        val visibles = todas.filter { item ->
            coincideFiltro(item, seleccionado) &&
                (q.isBlank() ||
                    item.cotizacion.folio.contains(q, ignoreCase = true) ||
                    item.cotizacion.cliente.contains(q, ignoreCase = true))
        }
        CotizacionesUiState(
            cargando = false,
            items = visibles,
            conteos = conteos,
            total = todas.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CotizacionesUiState()
    )

    fun cambiarBusqueda(valor: String) {
        busqueda.value = valor
    }

    fun cambiarFiltro(valor: FiltroCotizacion) {
        filtro.value = valor
    }

    private fun coincideFiltro(item: CotizacionListaItem, filtro: FiltroCotizacion): Boolean =
        when (filtro) {
            FiltroCotizacion.TODAS -> true
            FiltroCotizacion.BORRADORES -> item.estado == EstadoCotizacion.BORRADOR
            FiltroCotizacion.VIGENTES -> item.estado == EstadoCotizacion.EMITIDA
            FiltroCotizacion.POR_VENCER -> item.estado == EstadoCotizacion.EMITIDA &&
                item.diasRestantes in 0..7
            FiltroCotizacion.VENCIDAS -> item.estado == EstadoCotizacion.VENCIDA
            FiltroCotizacion.CONVERTIDAS -> item.estado == EstadoCotizacion.CONVERTIDA
            FiltroCotizacion.ANULADAS -> item.estado == EstadoCotizacion.ANULADA
        }
}
