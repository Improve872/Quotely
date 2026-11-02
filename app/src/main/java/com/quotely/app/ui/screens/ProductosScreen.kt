package com.quotely.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotely.app.AppContainer
import com.quotely.app.data.local.ProductoEntity
import com.quotely.app.ui.components.Buscador
import com.quotely.app.ui.components.EmptyState
import com.quotely.app.ui.components.Encabezado
import com.quotely.app.ui.components.Etiqueta
import com.quotely.app.ui.components.LocalSnackbarHostState
import com.quotely.app.ui.components.Monto
import com.quotely.app.ui.theme.PlexMono
import com.quotely.app.ui.viewmodel.productosViewModel
import com.quotely.app.util.Formato
import kotlinx.coroutines.launch

@Composable
fun ProductosScreen(
    container: AppContainer,
    onAbrirProducto: (Long) -> Unit,
    onNuevoProducto: () -> Unit
) {
    val vm = productosViewModel(container)
    val estado by vm.uiState.collectAsStateWithLifecycle()
    val busqueda by vm.busqueda.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbar.showSnackbar(it)
            vm.consumirMensaje()
        }
    }

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
                        titulo = "Productos",
                        sobretitulo = "CATÁLOGO",
                        subtitulo = "${estado.activos} activos de ${estado.total} productos"
                    )
                    Spacer(Modifier.height(16.dp))
                    Buscador(
                        valor = busqueda,
                        onValorChange = vm::cambiarBusqueda,
                        placeholder = "Buscar por nombre o código"
                    )
                }
            }

            if (estado.items.isEmpty() && !estado.cargando) {
                item {
                    EmptyState(
                        icono = Icons.Outlined.Inventory2,
                        titulo = if (estado.total == 0) "El catálogo está vacío" else "Sin resultados",
                        mensaje = if (estado.total == 0) {
                            "Agrega productos con su precio base para usarlos en cotizaciones y pedidos."
                        } else {
                            "Prueba con otro nombre o código."
                        },
                        modifier = Modifier.padding(horizontal = 20.dp),
                        accion = {
                            ExtendedFloatingActionButton(
                                onClick = onNuevoProducto,
                                shape = RoundedCornerShape(12.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                                text = { Text("Nuevo producto", style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    )
                }
            } else {
                items(estado.items, key = { it.id }) { producto ->
                    TarjetaProducto(
                        producto = producto,
                        onClick = { onAbrirProducto(producto.id) },
                        onActivo = { vm.alternarActivo(producto) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNuevoProducto,
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
private fun TarjetaProducto(
    producto: ProductoEntity,
    onClick: () -> Unit,
    onActivo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Etiqueta(producto.codigo, color = MaterialTheme.colorScheme.primary)
                    if (!producto.activo) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "INACTIVO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (producto.descripcion.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = producto.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Monto(
                        valor = producto.precioBase,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "/ ${producto.unidad}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Editar producto",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                Switch(
                    checked = producto.activo,
                    onCheckedChange = { onActivo() }
                )
            }
        }
    }
}
