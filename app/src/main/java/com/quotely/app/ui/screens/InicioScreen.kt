package com.quotely.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotely.app.AppContainer
import com.quotely.app.data.local.ResponsableEntity
import com.quotely.app.domain.EstadoCotizacion
import com.quotely.app.domain.EstadoPedido
import com.quotely.app.domain.TipoDescuento
import com.quotely.app.ui.components.Barcode
import com.quotely.app.ui.components.DivisorPunteado
import com.quotely.app.ui.components.EmptyState
import com.quotely.app.ui.components.Etiqueta
import com.quotely.app.ui.components.TarjetaDocumento
import com.quotely.app.ui.components.TicketCard
import com.quotely.app.ui.components.TituloSeccion
import com.quotely.app.ui.components.coloresCotizacion
import com.quotely.app.ui.components.coloresPedido
import com.quotely.app.ui.theme.EstiloMonto
import com.quotely.app.ui.theme.PlexMono
import com.quotely.app.ui.viewmodel.InicioUiState
import com.quotely.app.ui.viewmodel.MovimientoReciente
import com.quotely.app.ui.viewmodel.TipoMovimiento
import com.quotely.app.ui.viewmodel.inicioViewModel
import com.quotely.app.util.Formato
import java.time.LocalDate

@Composable
fun InicioScreen(
    container: AppContainer,
    usuario: ResponsableEntity,
    onNuevaCotizacion: () -> Unit,
    onNuevoPedido: () -> Unit,
    onAbrirCotizacion: (Long) -> Unit,
    onAbrirPedido: (Long) -> Unit,
    onAbrirAjustes: () -> Unit
) {
    val vm = inicioViewModel(container)
    val estado by vm.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Etiqueta("TURNO · ${Formato.fecha(LocalDate.now())}")
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${Formato.saludo()}, ${usuario.nombre.substringBefore(' ')}",
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onAbrirAjustes),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = Formato.iniciales(usuario.nombre),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        TicketCard(
            modifier = Modifier.padding(horizontal = 20.dp),
            notchFraction = 0.7f,
            muescas = false
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Etiqueta("COTIZADO ESTE MES", color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "MXN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = Formato.moneda(estado.totalMes),
                    style = MaterialTheme.typography.displaySmall.merge(EstiloMonto),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                DivisorPunteado()
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Estadistica(
                        valor = estado.cotizacionesVigentes,
                        etiqueta = "Vigentes",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Estadistica(
                        valor = estado.porVencer,
                        etiqueta = "Por vencer",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    Estadistica(
                        valor = estado.pedidosAbiertos,
                        etiqueta = "Pedidos",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Barcode(
                    texto = "QTL-${LocalDate.now().year}-${LocalDate.now().monthValue}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onNuevaCotizacion,
                modifier = Modifier
                    .weight(1.25f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cotizar", style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
            OutlinedButton(
                onClick = onNuevoPedido,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pedido", style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }

        Spacer(Modifier.height(28.dp))
        TituloSeccion(
            texto = "ÚLTIMOS MOVIMIENTOS",
            contador = estado.recientes.size,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(10.dp))
        if (estado.recientes.isEmpty() && !estado.cargando) {
            EmptyState(
                icono = Icons.Outlined.ReceiptLong,
                titulo = "Todavía no hay movimientos",
                mensaje = "Crea tu primera cotización o registra un pedido directo.",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        } else {
            estado.recientes.forEach { movimiento ->
                TarjetaMovimiento(
                    movimiento = movimiento,
                    onClick = {
                        if (movimiento.tipo == TipoMovimiento.COTIZACION) {
                            onAbrirCotizacion(movimiento.id)
                        } else {
                            onAbrirPedido(movimiento.id)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun Estadistica(
    valor: Int,
    etiqueta: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = valor.toString().padStart(2, '0'),
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = PlexMono),
            color = color
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TarjetaMovimiento(
    movimiento: MovimientoReciente,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colores = when (movimiento.tipo) {
        TipoMovimiento.COTIZACION -> coloresCotizacion(
            movimiento.estadoCotizacion ?: EstadoCotizacion.BORRADOR
        )
        TipoMovimiento.PEDIDO -> coloresPedido(
            movimiento.estadoPedido ?: EstadoPedido.PENDIENTE
        )
    }
    val etiquetaEstado = when (movimiento.tipo) {
        TipoMovimiento.COTIZACION -> (movimiento.estadoCotizacion ?: EstadoCotizacion.BORRADOR).etiqueta
        TipoMovimiento.PEDIDO -> (movimiento.estadoPedido ?: EstadoPedido.PENDIENTE).etiqueta
    }
    TarjetaDocumento(
        tipo = if (movimiento.tipo == TipoMovimiento.COTIZACION) "COTIZACIÓN" else "PEDIDO",
        folio = movimiento.folio,
        cliente = movimiento.cliente,
        detalle = Formato.fecha(movimiento.fecha),
        total = movimiento.total,
        colores = colores,
        estado = etiquetaEstado,
        onClick = onClick,
        modifier = modifier
    )
}
