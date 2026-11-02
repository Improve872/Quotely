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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotely.app.AppContainer
import com.quotely.app.data.local.PedidoConItems
import com.quotely.app.data.local.PedidoEntity
import com.quotely.app.data.local.PedidoItemEntity
import com.quotely.app.data.local.ResponsableEntity
import com.quotely.app.domain.Calculadora
import com.quotely.app.domain.EstadoPedido
import com.quotely.app.domain.Totales
import com.quotely.app.ui.components.Barcode
import com.quotely.app.ui.components.BloqueTotales
import com.quotely.app.ui.components.ChipPedido
import com.quotely.app.ui.components.DivisorPunteado
import com.quotely.app.ui.components.EmptyState
import com.quotely.app.ui.components.Etiqueta
import com.quotely.app.ui.components.LocalMostrarMensaje
import com.quotely.app.ui.components.Monto
import com.quotely.app.ui.components.TicketCard
import com.quotely.app.ui.theme.PlexMono
import com.quotely.app.ui.viewmodel.EventoPedido
import com.quotely.app.ui.viewmodel.pedidoDetalleViewModel
import com.quotely.app.util.Formato
import com.quotely.app.util.PdfGenerator
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun PedidoDetalleScreen(
    container: AppContainer,
    pedidoId: Long,
    onAtras: () -> Unit
) {
    val vm = pedidoDetalleViewModel(container, pedidoId)
    val estado by vm.uiState.collectAsStateWithLifecycle()
    val mostrarMensaje = LocalMostrarMensaje.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var cancelar by remember { mutableStateOf(false) }
    var generandoPdf by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.eventos.collect { evento ->
            when (evento) {
                is EventoPedido.Mensaje -> mostrarMensaje(evento.texto)
            }
        }
    }

    fun compartirPdf(detalle: PedidoConItems) {
        if (generandoPdf) return
        scope.launch {
            generandoPdf = true
            runCatching {
                PdfGenerator.generarPedido(
                    context = context,
                    detalle = detalle,
                    responsable = estado.responsable?.nombre ?: "Responsable",
                    folioCotizacion = estado.folioCotizacion
                )
            }.onSuccess { uri ->
                PdfGenerator.compartir(context, uri, "Pedido ${detalle.pedido.folio}")
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
                Etiqueta("PEDIDO")
                Spacer(Modifier.height(2.dp))
                Text(
                    text = detalle?.pedido?.folio ?: "Detalle",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            if (detalle != null) {
                ChipPedido(EstadoPedido.desde(detalle.pedido.estado))
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
                titulo = "Pedido no encontrado",
                mensaje = "El documento pudo ser eliminado o no existe.",
                modifier = Modifier.padding(20.dp),
                accion = { OutlinedButton(onClick = onAtras) { Text("Regresar") } }
            )

            else -> {
                val pedido = detalle.pedido
                val estadoPedido = EstadoPedido.desde(pedido.estado)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TicketPedido(
                            detalle = detalle,
                            estado = estadoPedido,
                            responsable = estado.responsable,
                            folioCotizacion = estado.folioCotizacion
                        )
                    }
                    item {
                        AccionesPedido(
                            estado = estadoPedido,
                            trabajando = estado.trabajando,
                            onConfirmar = { vm.cambiarEstado(EstadoPedido.CONFIRMADO) },
                            onEntregar = { vm.cambiarEstado(EstadoPedido.ENTREGADO) },
                            onCancelar = { cancelar = true },
                            onPdf = { compartirPdf(detalle) }
                        )
                    }
                }
            }
        }
    }

    if (cancelar) {
        AlertDialog(
            onDismissRequest = { cancelar = false },
            title = { Text("Cancelar pedido") },
            text = { Text("El pedido quedará cancelado y no podrá avanzar a otro estado.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        cancelar = false
                        vm.cambiarEstado(EstadoPedido.CANCELADO)
                    }
                ) { Text("Cancelar pedido") }
            },
            dismissButton = {
                TextButton(onClick = { cancelar = false }) { Text("Volver") }
            }
        )
    }
}

@Composable
private fun TicketPedido(
    detalle: PedidoConItems,
    estado: EstadoPedido,
    responsable: ResponsableEntity?,
    folioCotizacion: String?
) {
    val pedido = detalle.pedido
    TicketCard(notchFraction = 0.44f) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Etiqueta("FOLIO")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = pedido.folio,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono)
                    )
                }
                ChipPedido(estado)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = pedido.cliente,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (pedido.origen == "COTIZACION") {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (pedido.origen == "COTIZACION") {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (pedido.origen == "COTIZACION") Icons.Outlined.ReceiptLong else Icons.Outlined.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (pedido.origen == "COTIZACION") {
                            "Desde $folioCotizacion · precios conservados"
                        } else {
                            "Directo · precio base del catálogo"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Column {
                Etiqueta("FECHA")
                Spacer(Modifier.height(3.dp))
                Text(
                    text = Formato.fecha(LocalDate.ofEpochDay(pedido.fecha)),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PlexMono)
                )
            }
            Spacer(Modifier.height(14.dp))
            DivisorPunteado()
            Spacer(Modifier.height(12.dp))
            Etiqueta("CONCEPTOS")
            Spacer(Modifier.height(4.dp))
            detalle.itemsOrdenados.forEach { item ->
                ItemPedido(item)
            }
            Spacer(Modifier.height(10.dp))
            DivisorPunteado()
            Spacer(Modifier.height(12.dp))
            BloqueTotales(
                totales = totalesDe(pedido),
                tipoDescuento = com.quotely.app.domain.TipoDescuento.desde(pedido.tipoDescuentoGlobal),
                valorDescuento = pedido.valorDescuentoGlobal
            )
            if (pedido.notas.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Etiqueta("NOTAS")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = pedido.notas,
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
                        text = Formato.fechaHora(pedido.creadoEn),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Barcode(
                    texto = pedido.folio,
                    modifier = Modifier.width(110.dp).height(34.dp),
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
private fun ItemPedido(item: PedidoItemEntity) {
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
private fun AccionesPedido(
    estado: EstadoPedido,
    trabajando: Boolean,
    onConfirmar: () -> Unit,
    onEntregar: () -> Unit,
    onCancelar: () -> Unit,
    onPdf: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (estado) {
            EstadoPedido.PENDIENTE -> {
                Button(
                    onClick = onConfirmar,
                    enabled = !trabajando,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (trabajando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Confirmar pedido", style = MaterialTheme.typography.labelLarge)
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
                TextButton(onClick = onCancelar, enabled = !trabajando) {
                    Text("Cancelar pedido")
                }
            }

            EstadoPedido.CONFIRMADO -> {
                Button(
                    onClick = onEntregar,
                    enabled = !trabajando,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (trabajando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Outlined.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Marcar como entregado", style = MaterialTheme.typography.labelLarge)
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
                TextButton(onClick = onCancelar, enabled = !trabajando) {
                    Text("Cancelar pedido")
                }
            }

            EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO -> {
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

private fun totalesDe(pedido: PedidoEntity): Totales {
    val baseGravable = Calculadora.redondear(
        pedido.subtotal - pedido.descuentoVolumen - pedido.descuentoGlobal
    )
    return Totales(
        lineas = emptyList(),
        subtotal = pedido.subtotal,
        descuentoVolumen = pedido.descuentoVolumen,
        descuentoGlobal = pedido.descuentoGlobal,
        baseGravable = baseGravable,
        iva = pedido.iva,
        total = pedido.total
    )
}
