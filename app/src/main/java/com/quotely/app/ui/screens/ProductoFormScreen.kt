package com.quotely.app.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotely.app.AppContainer
import com.quotely.app.ui.components.CampoTexto
import com.quotely.app.ui.components.ChipsFiltro
import com.quotely.app.ui.components.Etiqueta
import com.quotely.app.ui.components.LocalMostrarMensaje
import com.quotely.app.ui.components.MensajeError
import com.quotely.app.ui.components.TituloSeccion
import com.quotely.app.ui.theme.PlexCondensed
import com.quotely.app.ui.viewmodel.productoFormViewModel
import kotlinx.coroutines.launch

@Composable
fun ProductoFormScreen(
    container: AppContainer,
    productoId: Long?,
    onAtras: () -> Unit,
    onGuardado: () -> Unit
) {
    val vm = productoFormViewModel(container, productoId)
    val estado by vm.state.collectAsStateWithLifecycle()
    val mostrarMensaje = LocalMostrarMensaje.current
    val unidades = (listOf("pieza", "caja", "paquete", "licencia", "servicio", "hora") +
        estado.unidad).distinct()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAtras, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Regresar")
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Etiqueta(if (estado.esEdicion) "EDITAR PRODUCTO" else "NUEVO PRODUCTO")
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (estado.esEdicion) "Producto" else "Alta de producto",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PlexCondensed)
                )
            }
            Icon(
                Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        if (estado.cargando) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TituloSeccion("IDENTIFICACIÓN")
                    Spacer(Modifier.height(6.dp))
                    CampoTexto(
                        valor = estado.codigo,
                        onValorChange = vm::actualizarCodigo,
                        etiqueta = "Código",
                        placeholder = "Ej. LAP-001"
                    )
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(
                        valor = estado.nombre,
                        onValorChange = vm::actualizarNombre,
                        etiqueta = "Nombre del producto",
                        placeholder = "Ej. Laptop Lenovo ThinkPad E14"
                    )
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(
                        valor = estado.descripcion,
                        onValorChange = vm::actualizarDescripcion,
                        etiqueta = "Descripción (opcional)",
                        singleLine = false,
                        minLines = 2
                    )
                }

                item {
                    TituloSeccion("PRECIO Y UNIDAD")
                    Spacer(Modifier.height(6.dp))
                    CampoTexto(
                        valor = estado.precio,
                        onValorChange = vm::actualizarPrecio,
                        etiqueta = "Precio base",
                        sufijo = "$",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Unidad de venta",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    ChipsFiltro(
                        opciones = unidades,
                        seleccionada = estado.unidad,
                        onSeleccion = vm::actualizarUnidad
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "El precio base se usa en pedidos directos. En cotizaciones se congela al emitir.",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Producto activo", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Los productos inactivos no aparecen al crear documentos.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = estado.activo,
                                onCheckedChange = vm::actualizarActivo
                            )
                        }
                    }
                }

                if (estado.error != null) {
                    item { MensajeError(estado.error) }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        if (!estado.cargando) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Button(
                    onClick = {
                        vm.guardar {
                            mostrarMensaje("Producto guardado.")
                            onGuardado()
                        }
                    },
                    enabled = !estado.guardando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (estado.guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Guardar producto", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
