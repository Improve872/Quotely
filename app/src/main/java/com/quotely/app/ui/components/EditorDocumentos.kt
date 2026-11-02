package com.quotely.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quotely.app.data.local.ProductoEntity
import com.quotely.app.domain.Calculadora
import com.quotely.app.domain.PricingConfig
import com.quotely.app.domain.TipoDescuento
import com.quotely.app.domain.Totales
import com.quotely.app.ui.theme.PlexMono
import com.quotely.app.ui.viewmodel.LineaFormulario
import com.quotely.app.util.Formato

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorProductosSheet(
    visible: Boolean,
    productos: List<ProductoEntity>,
    onDismiss: () -> Unit,
    onSeleccion: (ProductoEntity) -> Unit
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var consulta by remember { mutableStateOf("") }
    val filtrados = remember(productos, consulta) {
        val q = consulta.trim()
        productos.filter {
            q.isBlank() ||
                it.nombre.contains(q, ignoreCase = true) ||
                it.codigo.contains(q, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Etiqueta("CATÁLOGO ACTIVO")
                    Spacer(Modifier.height(3.dp))
                    Text("Agregar producto", style = MaterialTheme.typography.headlineSmall)
                }
                Text(
                    text = filtrados.size.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            Buscador(
                valor = consulta,
                onValorChange = { consulta = it },
                placeholder = "Buscar por nombre o código"
            )
            Spacer(Modifier.height(12.dp))
            if (filtrados.isEmpty()) {
                Text(
                    text = "No hay productos que coincidan. Créalos desde la sección Productos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtrados, key = { it.id }) { producto ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onSeleccion(producto)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = producto.nombre,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${producto.codigo} · ${producto.unidad}",
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Monto(
                                        valor = producto.precioBase,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun LineaEditor(
    linea: LineaFormulario,
    onCantidad: (Int) -> Unit,
    onQuitar: () -> Unit,
    modifier: Modifier = Modifier,
    aplicarDescuentoVolumen: Boolean = true
) {
    val calculada = Calculadora.calcularLinea(linea.entrada, aplicarDescuentoVolumen)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = linea.producto.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${linea.producto.codigo} · ${linea.producto.unidad}",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onQuitar, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Quitar ${linea.producto.nombre}",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Formato.moneda(calculada.precioUnitario) + " / " + linea.producto.unidad,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                StepperCantidad(cantidad = linea.cantidad, onCambio = onCantidad)
                Spacer(Modifier.width(12.dp))
                Monto(
                    valor = calculada.importe,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (calculada.descuentoVolumenPct > 0.0) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = "Volumen ${Formato.porcentaje(calculada.descuentoVolumenPct)}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "-${Formato.moneda(calculada.descuentoVolumen)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Base ${Formato.moneda(calculada.base)}",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BloqueTotales(
    totales: Totales,
    modifier: Modifier = Modifier,
    tipoDescuento: TipoDescuento = TipoDescuento.NINGUNO,
    valorDescuento: Double = 0.0,
    aplicarDescuentos: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FilaTotal(etiqueta = "Subtotal", valor = totales.subtotal)
        if (totales.descuentoVolumen > 0) {
            FilaTotal(
                etiqueta = "Descuento por volumen",
                valor = -totales.descuentoVolumen,
                negativo = true
            )
        }
        if (totales.descuentoGlobal > 0) {
            FilaTotal(
                etiqueta = if (tipoDescuento == TipoDescuento.PORCENTAJE) {
                    "Descuento global (${Formato.porcentaje(valorDescuento)})"
                } else {
                    "Descuento global"
                },
                valor = -totales.descuentoGlobal,
                negativo = true
            )
        }
        FilaTotal(etiqueta = "IVA 16%", valor = totales.iva)
        Spacer(Modifier.height(10.dp))
        DivisorPunteado()
        Spacer(Modifier.height(10.dp))
        FilaTotal(etiqueta = "Total", valor = totales.total, enfasis = true)
        if (!aplicarDescuentos) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Precio base del catálogo · sin descuentos",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
