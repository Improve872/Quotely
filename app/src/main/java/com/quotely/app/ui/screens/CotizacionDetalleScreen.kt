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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotely.app.AppContainer
import com.quotely.app.data.local.CotizacionConItems
import com.quotely.app.data.local.CotizacionEntity
import com.quotely.app.data.local.CotizacionItemEntity
import com.quotely.app.data.local.ResponsableEntity
import com.quotely.app.domain.Calculadora
import com.quotely.app.domain.EstadoCotizacion
import com.quotely.app.domain.TipoDescuento
import com.quotely.app.domain.Totales
import com.quotely.app.domain.estadoEfectivoCotizacion
import com.quotely.app.domain.puedeConvertirCotizacion
import com.quotely.app.ui.components.Barcode
import com.quotely.app.ui.components.BloqueTotales
import com.quotely.app.ui.components.ChipCotizacion
import com.quotely.app.ui.components.DivisorPunteado
import com.quotely.app.ui.components.EmptyState
import com.quotely.app.ui.components.Etiqueta
import com.quotely.app.ui.components.LocalMostrarMensaje
import com.quotely.app.ui.components.Monto
import com.quotely.app.ui.components.TicketCard
import com.quotely.app.ui.theme.EstiloMonto
import com.quotely.app.ui.theme.PlexMono
import com.quotely.app.ui.viewmodel.CotizacionDetalleViewModel
import com.quotely.app.ui.viewmodel.EventoCotizacion
import com.quotely.app.ui.viewmodel.cotizacionDetalleViewModel
import com.quotely.app.util.Formato
import com.quotely.app.util.PdfGenerator
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun CotizacionDetalleScreen(
    container: AppContainer,
    usuario: ResponsableEntity,
    cotizacionId: Long,
    onAtras: () -> Unit,
    onEditar: (Long) -> Unit,
    onVerPedido: (Long) -> Unit
) {
    val vm = cotizacionDetalleViewModel(container, cotizacionId)
    val estado by vm.uiState.collectAsStateWithLifecycle()
    val mostrarMensaje = LocalMostrarMensaje.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var dialogo by remember { mutableStateOf<String?>(null) }
    var generandoPdf by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.eventos.collect { evento ->
            when (evento) {
                is EventoCotizacion.Mensaje -> mostrarMensaje(evento.texto)
                is EventoCotizacion.PedidoCreado -> {
                    mostrarMensaje("Pedido creado con los precios de la cotización.")
                    onVerPedido(evento.pedidoId)
                }
                EventoCotizacion.Eliminada -> {
                    mostrarMensaje("Borrador eliminado.")
                    onAtras()
                }
            }
        }
    }

    fun compartirPdf(detalle: CotizacionConItems) {
        if (generandoPdf) return
        scope.launch {
            generandoPdf = true
            runCatching {
                PdfGenerator.generarCotizacion(
                    context = context,
                    detalle = detalle,
                    responsable = estado.responsable?.nombre ?: "Responsable"
                )
            }.onSuccess { uri ->
                PdfGenerator.compartir(context, uri, "Cotización ${detalle.cotizacion.folio}")
            }.onFailure {
                mostrarMensaje("No se pudo generar el PDF. Intenta de nuevo.")
            }
            generandoPdf = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val detalle = estado.detalle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAtras, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Regresar")
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Etiqueta("COTIZACIÓN")
                Spacer(Modifier.height(2.dp))
                Text(
                    text = detalle?.cotizacion?.folio ?: "Detalle",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            if (detalle != null) {
                val efectivo = estadoEfectivoCotizacion(
                    EstadoCotizacion.desde(detalle.cotizacion.estado),
                    LocalDate.ofEpochDay(detalle.cotizacion.fechaCaducidad)
                )
                ChipCotizacion(efectivo)
            }
            IconButton(
                onClick = { detalle?.let { compartirPdf(it) } },
                enabled = detalle != null && !generandoPdf
            ) {
                if (generandoPdf) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(Icons.Outlined.Share, contentDescription = "Compartir PDF")
                }
            }
        }

        when {
            estado.cargando -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            detalle == null -> EmptyState(
                icono = Icons.Outlined.ReceiptLong,
                titulo = "Cotización no encontrada",
                mensaje = "El documento pudo ser eliminado o no existe.",
                modifier = Modifier.padding(20.dp),
                accion = { OutlinedButton(onClick = onAtras) { Text("Regresar") } }
            )

            else -> {
                val cotizacion = detalle.cotizacion
                val efectivo = estadoEfectivoCotizacion(
                    EstadoCotizacion.desde(cotizacion.estado),
                    LocalDate.ofEpochDay(cotizacion.fechaCaducidad)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TicketCotizacion(
                            detalle = detalle,
                            estado = efectivo,
                            responsable = estado.responsable
                        )
                    }

                    if (efectivo == EstadoCotizacion.VENCIDA) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Esta cotización venció el ${
                                        Formato.fecha(LocalDate.ofEpochDay(cotizacion.fechaCaducidad))
                                    }. Ya no se puede convertir; registra un pedido directo con precios base.",
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    item {
                        AccionesCotizacion(
                            estado = efectivo,
                            pedidoId = estado.pedido?.id,
                            trabajando = estado.trabajando,
                            onEditar = { onEditar(cotizacion.id) },
                            onEmitir = vm::emitir,
                            onConvertir = { dialogo = "convertir" },
                            onAnular = { dialogo = "anular" },
                            onEliminar = { dialogo = "eliminar" },
                            onVerPedido = { estado.pedido?.let { onVerPedido(it.id) } },
                            onPdf = { compartirPdf(detalle) }
                        )
                    }
                }
            }
        }
    }

    when (dialogo) {
        "convertir" -> AlertDialog(
            onDismissRequest = { dialogo = null },
            title = { Text("Convertir a pedido") },
            text = {
                Text(
                    "Se creará un pedido con los precios, cantidades y descuentos de esta cotización. " +
                        "La cotización quedará marcada como convertida."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogo = null
                        vm.convertirAPedido(usuario.id)
                    }
                ) { Text("Convertir") }
            },
            dismissButton = {
                TextButton(onClick = { dialogo = null }) { Text("Cancelar") }
            }
        )

        "anular" -> AlertDialog(
            onDismissRequest = { dialogo = null },
            title = { Text("Anular cotización") },
            text = { Text("La cotización conservará su folio, pero ya no podrá convertirse en pedido.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogo = null
                        vm.anular()
                    }
                ) { Text("Anular") }
            },
            dismissButton = {
                TextButton(onClick = { dialogo = null }) { Text("Cancelar") }
            }
        )

        "eliminar" -> AlertDialog(
            onDismissRequest = { dialogo = null },
            title = { Text("Eliminar borrador") },
            text = { Text("Se borrarán la cotización y sus productos. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogo = null
                        vm.eliminarBorrador()
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { dialogo = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TicketCotizacion(
    detalle: CotizacionConItems,
    estado: EstadoCotizacion,
    responsable: ResponsableEntity?
) {
    val cotizacion = detalle.cotizacion
    TicketCard(notchFraction = 0.44f) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Etiqueta("FOLIO")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = cotizacion.folio,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono)
                    )
                }
                ChipCotizacion(estado)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = cotizacion.cliente,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (cotizacion.clienteContacto.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = cotizacion.clienteContacto,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                FechaBloque("EMISIÓN", LocalDate.ofEpochDay(cotizacion.fechaEmision))
                FechaBloque("CADUCIDAD", LocalDate.ofEpochDay(cotizacion.fechaCaducidad))
            }
            Spacer(Modifier.height(14.dp))
            DivisorPunteado()
            Spacer(Modifier.height(12.dp))
            Etiqueta("CONCEPTOS")
            Spacer(Modifier.height(4.dp))
            detalle.itemsOrdenados.forEach { item ->
                ItemCotizacion(item)
            }
            Spacer(Modifier.height(10.dp))
            DivisorPunteado()
            Spacer(Modifier.height(12.dp))
            BloqueTotales(
                totales = totalesDe(cotizacion),
                tipoDescuento = TipoDescuento.desde(cotizacion.tipoDescuentoGlobal),
                valorDescuento = cotizacion.valorDescuentoGlobal
            )
            if (cotizacion.notas.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Etiqueta("NOTAS")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = cotizacion.notas,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            DivisorPunteado()
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Etiqueta("RESPONSABLE")
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = responsable?.nombre ?: "Responsable",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = Formato.fechaHora(cotizacion.creadoEn),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Barcode(
                    texto = cotizacion.folio,
                    modifier = Modifier
                        .width(110.dp)
                        .height(34.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "QUOTELY · DOCUMENTO LOCAL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FechaBloque(etiqueta: String, fecha: LocalDate) {
    Column {
        Etiqueta(etiqueta)
        Spacer(Modifier.height(3.dp))
        Text(
            text = Formato.fecha(fecha),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PlexMono),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ItemCotizacion(item: CotizacionItemEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.descripcion,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${item.codigo} · ${item.unidad}",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.descuentoVolumenPct > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Volumen ${Formato.porcentaje(item.descuentoVolumenPct)} · -${Formato.moneda(item.descuentoVolumen)}",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Monto(
                valor = item.importe,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${item.cantidad} × ${Formato.moneda(item.precioUnitario)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccionesCotizacion(
    estado: EstadoCotizacion,
    pedidoId: Long?,
    trabajando: Boolean,
    onEditar: () -> Unit,
    onEmitir: () -> Unit,
    onConvertir: () -> Unit,
    onAnular: () -> Unit,
    onEliminar: () -> Unit,
    onVerPedido: () -> Unit,
    onPdf: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (estado) {
            EstadoCotizacion.BORRADOR -> {
                Button(
                    onClick = onEditar,
                    enabled = !trabajando,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Editar borrador", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onEmitir,
                    enabled = !trabajando,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (trabajando) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Emitir cotización", style = MaterialTheme.typography.labelLarge)
                    }
                }
                TextButton(
                    onClick = onEliminar,
                    enabled = !trabajando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar borrador")
                }
            }

            EstadoCotizacion.EMITIDA -> {
                Button(
                    onClick = onConvertir,
                    enabled = !trabajando,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (trabajando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Convertir a pedido", style = MaterialTheme.typography.labelLarge)
                    }
                }
                OutlinedButton(
                    onClick = onPdf,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir PDF", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onAnular, enabled = !trabajando) {
                    Text("Anular cotización")
                }
            }

            EstadoCotizacion.VENCIDA -> {
                OutlinedButton(
                    onClick = onPdf,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir PDF", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onAnular, enabled = !trabajando) {
                    Text("Anular cotización")
                }
            }

            EstadoCotizacion.CONVERTIDA -> {
                if (pedidoId != null) {
                    Button(
                        onClick = onVerPedido,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ver pedido", style = MaterialTheme.typography.labelLarge)
                    }
                }
                OutlinedButton(
                    onClick = onPdf,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir PDF", style = MaterialTheme.typography.labelLarge)
                }
            }

            EstadoCotizacion.ANULADA -> {
                OutlinedButton(
                    onClick = onPdf,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir PDF", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun totalesDe(cotizacion: CotizacionEntity): Totales {
    val baseGravable = Calculadora.redondear(
        cotizacion.subtotal - cotizacion.descuentoVolumen - cotizacion.descuentoGlobal
    )
    return Totales(
        lineas = emptyList(),
        subtotal = cotizacion.subtotal,
        descuentoVolumen = cotizacion.descuentoVolumen,
        descuentoGlobal = cotizacion.descuentoGlobal,
        baseGravable = baseGravable,
        iva = cotizacion.iva,
        total = cotizacion.total
    )
}
