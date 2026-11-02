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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotely.app.AppContainer
import com.quotely.app.data.local.ResponsableEntity
import com.quotely.app.data.repository.GuardadoCotizacion
import com.quotely.app.domain.EstadoCotizacion
import com.quotely.app.domain.PricingConfig
import com.quotely.app.domain.TipoDescuento
import com.quotely.app.ui.components.BloqueTotales
import com.quotely.app.ui.components.CampoFecha
import com.quotely.app.ui.components.CampoTexto
import com.quotely.app.ui.components.ChipsFiltro
import com.quotely.app.ui.components.DialogoFecha
import com.quotely.app.ui.components.Etiqueta
import com.quotely.app.ui.components.LineaEditor
import com.quotely.app.ui.components.LocalMostrarMensaje
import com.quotely.app.ui.components.MensajeError
import com.quotely.app.ui.components.SelectorProductosSheet
import com.quotely.app.ui.components.TituloSeccion
import com.quotely.app.ui.theme.PlexCondensed
import com.quotely.app.ui.theme.PlexMono
import com.quotely.app.ui.viewmodel.CotizacionFormState
import com.quotely.app.ui.viewmodel.cotizacionFormViewModel
import com.quotely.app.util.Formato
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun CotizacionFormScreen(
    container: AppContainer,
    usuario: ResponsableEntity,
    cotizacionId: Long?,
    onAtras: () -> Unit,
    onGuardada: (GuardadoCotizacion) -> Unit
) {
    val vm = cotizacionFormViewModel(container, cotizacionId)
    val estado by vm.state.collectAsStateWithLifecycle()
    val mostrarMensaje = LocalMostrarMensaje.current
    var mostrarSelector by remember { mutableStateOf(false) }
    var dialogoFecha by remember { mutableStateOf<String?>(null) }

    fun guardar(emitir: Boolean) {
        vm.guardar(emitir = emitir, responsableId = usuario.id) { guardado ->
            val mensaje = if (emitir) {
                "Cotización ${guardado.folio} emitida."
            } else {
                "Borrador ${guardado.folio} guardado."
            }
            mostrarMensaje(mensaje)
            onGuardada(guardado)
        }
    }

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
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Regresar"
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Etiqueta(if (estado.esEdicion) "EDITAR BORRADOR" else "NUEVA COTIZACIÓN")
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (estado.esEdicion && estado.folio.isNotBlank()) estado.folio else "Cotización",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PlexCondensed)
                )
            }
            if (estado.folio.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = "BORRADOR",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
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
                    Etiqueta("CLIENTE")
                    Spacer(Modifier.height(6.dp))
                    CampoTexto(
                        valor = estado.cliente,
                        onValorChange = vm::actualizarCliente,
                        etiqueta = "Nombre del cliente",
                        placeholder = "Ej. Corporativo Andares"
                    )
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(
                        valor = estado.contacto,
                        onValorChange = vm::actualizarContacto,
                        etiqueta = "Contacto (opcional)",
                        placeholder = "Nombre · correo o teléfono"
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    TituloSeccion("VIGENCIA")
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CampoFecha(
                            etiqueta = "Emisión",
                            fecha = estado.emision,
                            onClick = { dialogoFecha = "emision" },
                            modifier = Modifier.weight(1f)
                        )
                        CampoFecha(
                            etiqueta = "Caducidad",
                            fecha = estado.caducidad,
                            onClick = { dialogoFecha = "caducidad" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    TituloSeccion("PRODUCTOS", contador = estado.lineas.size) {
                        TextButton(onClick = { mostrarSelector = true }) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (estado.lineas.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (estado.productos.isEmpty()) {
                                        "No hay productos activos. Crea uno desde la sección Productos."
                                    } else {
                                        "Agrega el primer producto de esta cotización."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { mostrarSelector = true },
                                    enabled = estado.productos.isNotEmpty(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Elegir producto", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            estado.lineas.forEach { linea ->
                                LineaEditor(
                                    linea = linea,
                                    onCantidad = { vm.cambiarCantidad(linea.clave, it) },
                                    onQuitar = { vm.quitarLinea(linea.clave) }
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "El descuento por volumen se aplica automáticamente: " +
                                PricingConfig.tramosVolumen.joinToString(" · ") {
                                    "${it.cantidadMinima}+ ${Formato.porcentaje(it.porcentaje)}"
                                },
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    TituloSeccion("DESCUENTOS")
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            ChipsFiltro(
                                opciones = TipoDescuento.entries.map { it.etiqueta },
                                seleccionada = estado.tipoDescuento.etiqueta,
                                onSeleccion = { etiqueta ->
                                    TipoDescuento.entries.firstOrNull { it.etiqueta == etiqueta }
                                        ?.let(vm::cambiarTipoDescuento)
                                }
                            )
                            if (estado.tipoDescuento != TipoDescuento.NINGUNO) {
                                Spacer(Modifier.height(10.dp))
                                CampoTexto(
                                    valor = estado.valorDescuento,
                                    onValorChange = vm::actualizarValorDescuento,
                                    etiqueta = if (estado.tipoDescuento == TipoDescuento.PORCENTAJE) {
                                        "Descuento global"
                                    } else {
                                        "Monto de descuento"
                                    },
                                    sufijo = if (estado.tipoDescuento == TipoDescuento.PORCENTAJE) "%" else "$",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Se aplica después del descuento por volumen. " +
                                        "Descuento calculado: ${Formato.moneda(estado.totales.descuentoGlobal)}",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Sin descuento global. Los descuentos por volumen siguen activos.",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    TituloSeccion("RESUMEN")
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        BloqueTotales(
                            totales = estado.totales,
                            modifier = Modifier.padding(16.dp),
                            tipoDescuento = estado.tipoDescuento,
                            valorDescuento = estado.valorDescuentoNumero
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    TituloSeccion("NOTAS")
                    Spacer(Modifier.height(6.dp))
                    CampoTexto(
                        valor = estado.notas,
                        onValorChange = vm::actualizarNotas,
                        etiqueta = "Condiciones o comentarios (opcional)",
                        singleLine = false,
                        minLines = 3
                    )
                }

                if (estado.error != null) {
                    item {
                        MensajeError(estado.error)
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        if (!estado.cargando) {
            BarraGuardado(
                total = estado.totales.total,
                guardando = estado.guardando,
                onBorrador = { guardar(false) },
                onEmitir = { guardar(true) }
            )
        }
    }

    SelectorProductosSheet(
        visible = mostrarSelector,
        productos = estado.productos,
        onDismiss = { mostrarSelector = false },
        onSeleccion = vm::agregarProducto
    )

    DialogoFecha(
        visible = dialogoFecha == "emision",
        inicial = estado.emision,
        onDismiss = { dialogoFecha = null },
        onSeleccion = { vm.cambiarEmision(it); dialogoFecha = null }
    )
    DialogoFecha(
        visible = dialogoFecha == "caducidad",
        inicial = estado.caducidad,
        onDismiss = { dialogoFecha = null },
        onSeleccion = { vm.cambiarCaducidad(it); dialogoFecha = null }
    )
}

@Composable
private fun BarraGuardado(
    total: Double,
    guardando: Boolean,
    onBorrador: () -> Unit,
    onEmitir: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Etiqueta("TOTAL CON IVA")
                Spacer(Modifier.weight(1f))
                Text(
                    text = Formato.moneda(total),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlexMono,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onBorrador,
                    enabled = !guardando,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Borrador", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = onEmitir,
                    enabled = !guardando,
                    modifier = Modifier.weight(1.35f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Emitir cotización", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
