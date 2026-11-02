package com.quotely.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotely.app.AppContainer
import com.quotely.app.domain.EstadoCotizacion
import com.quotely.app.ui.components.Buscador
import com.quotely.app.ui.components.ChipsFiltro
import com.quotely.app.ui.components.EmptyState
import com.quotely.app.ui.components.Encabezado
import com.quotely.app.ui.components.TarjetaDocumento
import com.quotely.app.ui.components.coloresCotizacion
import com.quotely.app.ui.viewmodel.CotizacionListaItem
import com.quotely.app.ui.viewmodel.FiltroCotizacion
import com.quotely.app.ui.viewmodel.cotizacionesViewModel
import com.quotely.app.util.Formato

@Composable
fun CotizacionesScreen(
    container: AppContainer,
    onAbrirCotizacion: (Long) -> Unit,
    onNuevaCotizacion: () -> Unit
) {
    val vm = cotizacionesViewModel(container)
    val estado by vm.uiState.collectAsStateWithLifecycle()
    val busqueda by vm.busqueda.collectAsStateWithLifecycle()
    val filtro by vm.filtro.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Encabezado(
                        titulo = "Cotizaciones",
                        sobretitulo = "OPERACIÓN",
                        subtitulo = "${estado.total} documentos · ${
                            estado.conteos[FiltroCotizacion.VIGENTES] ?: 0
                        } vigentes"
                    )
                    Spacer(Modifier.height(16.dp))
                    Buscador(
                        valor = busqueda,
                        onValorChange = vm::cambiarBusqueda,
                        placeholder = "Buscar por folio o cliente"
                    )
                    Spacer(Modifier.height(10.dp))
                    ChipsFiltro(
                        opciones = FiltroCotizacion.entries.map { it.etiqueta },
                        seleccionada = filtro.etiqueta,
                        onSeleccion = { etiqueta ->
                            FiltroCotizacion.entries.firstOrNull { it.etiqueta == etiqueta }
                                ?.let(vm::cambiarFiltro)
                        }
                    )
                }
            }

            if (estado.items.isEmpty() && !estado.cargando) {
                item {
                    EmptyState(
                        icono = Icons.Outlined.ReceiptLong,
                        titulo = if (estado.total == 0) "Aún no hay cotizaciones" else "Sin resultados",
                        mensaje = if (estado.total == 0) {
                            "Crea una cotización con productos, descuentos y fecha de vigencia."
                        } else {
                            "Prueba con otro texto o cambia el filtro de estado."
                        },
                        modifier = Modifier.padding(horizontal = 20.dp),
                        accion = if (estado.total == 0) {
                            {
                                Button(
                                    onClick = onNuevaCotizacion,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(" Nueva cotización", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        } else {
                            null
                        }
                    )
                }
            } else {
                items(estado.items, key = { it.cotizacion.id }) { item ->
                    TarjetaCotizacion(
                        item = item,
                        onClick = { onAbrirCotizacion(item.cotizacion.id) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNuevaCotizacion,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text("Nueva", style = MaterialTheme.typography.labelLarge) }
        )
    }
}

@Composable
private fun TarjetaCotizacion(
    item: CotizacionListaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cotizacion = item.cotizacion
    val detalle = when (item.estado) {
        EstadoCotizacion.BORRADOR -> "Borrador · ${Formato.fecha(LocalDateDe(cotizacion.fechaEmision))}"
        EstadoCotizacion.EMITIDA -> "Vence ${Formato.fecha(LocalDateDe(cotizacion.fechaCaducidad))} · ${item.diasRestantes} días"
        EstadoCotizacion.VENCIDA -> "Venció ${Formato.fecha(LocalDateDe(cotizacion.fechaCaducidad))}"
        EstadoCotizacion.CONVERTIDA -> "Pedido generado · ${Formato.fecha(LocalDateDe(cotizacion.fechaEmision))}"
        EstadoCotizacion.ANULADA -> "Anulada · ${Formato.fecha(LocalDateDe(cotizacion.fechaEmision))}"
    }
    TarjetaDocumento(
        tipo = "COTIZACIÓN",
        folio = cotizacion.folio,
        cliente = cotizacion.cliente,
        detalle = detalle,
        total = cotizacion.total,
        colores = coloresCotizacion(item.estado),
        estado = item.estado.etiqueta,
        onClick = onClick,
        modifier = modifier
    )
}

private fun LocalDateDe(epochDay: Long) = java.time.LocalDate.ofEpochDay(epochDay)
