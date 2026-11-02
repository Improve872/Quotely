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
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotely.app.AppContainer
import com.quotely.app.ui.components.Buscador
import com.quotely.app.ui.components.ChipsFiltro
import com.quotely.app.ui.components.EmptyState
import com.quotely.app.ui.components.Encabezado
import com.quotely.app.ui.components.TarjetaDocumento
import com.quotely.app.ui.components.coloresPedido
import com.quotely.app.ui.viewmodel.FiltroPedido
import com.quotely.app.ui.viewmodel.PedidoListaItem
import com.quotely.app.ui.viewmodel.pedidosViewModel
import com.quotely.app.util.Formato
import java.time.LocalDate

@Composable
fun PedidosScreen(
    container: AppContainer,
    onAbrirPedido: (Long) -> Unit,
    onNuevoPedido: () -> Unit,
    onIrCotizaciones: () -> Unit
) {
    val vm = pedidosViewModel(container)
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
                        titulo = "Pedidos",
                        sobretitulo = "OPERACIÓN",
                        subtitulo = "${estado.total} pedidos · ${
                            (estado.conteos[FiltroPedido.PENDIENTES] ?: 0) +
                                (estado.conteos[FiltroPedido.CONFIRMADOS] ?: 0)
                        } abiertos"
                    )
                    Spacer(Modifier.height(16.dp))
                    Buscador(
                        valor = busqueda,
                        onValorChange = vm::cambiarBusqueda,
                        placeholder = "Buscar por folio o cliente"
                    )
                    Spacer(Modifier.height(10.dp))
                    ChipsFiltro(
                        opciones = FiltroPedido.entries.map { it.etiqueta },
                        seleccionada = filtro.etiqueta,
                        onSeleccion = { etiqueta ->
                            FiltroPedido.entries.firstOrNull { it.etiqueta == etiqueta }
                                ?.let(vm::cambiarFiltro)
                        }
                    )
                }
            }

            if (estado.items.isEmpty() && !estado.cargando) {
                item {
                    EmptyState(
                        icono = Icons.Outlined.ShoppingBag,
                        titulo = if (estado.total == 0) "Todavía no hay pedidos" else "Sin resultados",
                        mensaje = if (estado.total == 0) {
                            "Registra un pedido directo con precios base o convierte una cotización vigente."
                        } else {
                            "Prueba con otro texto o cambia el filtro de estado."
                        },
                        modifier = Modifier.padding(horizontal = 20.dp),
                        accion = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onNuevoPedido,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Nuevo pedido", style = MaterialTheme.typography.labelLarge)
                                }
                                OutlinedButton(
                                    onClick = onIrCotizaciones,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Convertir una cotización", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    )
                }
            } else {
                items(estado.items, key = { it.pedido.id }) { item ->
                    TarjetaPedido(
                        item = item,
                        onClick = { onAbrirPedido(item.pedido.id) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNuevoPedido,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text("Nuevo", style = MaterialTheme.typography.labelLarge) }
        )
    }
}

@Composable
private fun TarjetaPedido(
    item: PedidoListaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pedido = item.pedido
    TarjetaDocumento(
        tipo = "PEDIDO",
        folio = pedido.folio,
        cliente = pedido.cliente,
        detalle = "${Formato.fecha(LocalDate.ofEpochDay(pedido.fecha))} · ${
            if (pedido.origen == "COTIZACION") "desde cotización" else "directo"
        }",
        total = pedido.total,
        colores = coloresPedido(item.estado),
        estado = item.estado.etiqueta,
        onClick = onClick,
        modifier = modifier,
        extra = if (pedido.origen == "COTIZACION") {
            "Precios de la cotización conservados"
        } else {
            "Precio base del catálogo"
        }
    )
}
