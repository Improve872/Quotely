package com.quotely.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quotely.app.domain.EstadoCotizacion
import com.quotely.app.domain.EstadoPedido
import com.quotely.app.ui.theme.EstiloMonto
import com.quotely.app.ui.theme.PlexMono
import com.quotely.app.util.Formato
import com.quotely.app.util.aLocalDateUtc
import com.quotely.app.util.aMillisUtc
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.launch

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("No hay SnackbarHostState en este árbol de composición")
}

val LocalMostrarMensaje = staticCompositionLocalOf<(String) -> Unit> {
    error("No hay un mostrador de mensajes en este árbol de composición")
}

@Composable
fun ProporcionarSnackbar(host: SnackbarHostState, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val mostrar: (String) -> Unit = remember(host, scope) {
        { texto ->
            scope.launch { host.showSnackbar(texto) }
            Unit
        }
    }
    CompositionLocalProvider(
        LocalSnackbarHostState provides host,
        LocalMostrarMensaje provides mostrar,
        content = content
    )
}

@Composable
fun Etiqueta(texto: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = texto.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1
    )
}

@Composable
fun Monto(
    valor: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = LocalContentColor.current
) {
    Text(
        text = Formato.moneda(valor),
        modifier = modifier,
        style = style.merge(EstiloMonto),
        color = color,
        maxLines = 1
    )
}

data class ColoresEstado(val contenedor: Color, val contenido: Color, val lomo: Color)

@Composable
fun coloresCotizacion(estado: EstadoCotizacion): ColoresEstado = when (estado) {
    EstadoCotizacion.BORRADOR -> ColoresEstado(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.colorScheme.outline
    )
    EstadoCotizacion.EMITIDA -> ColoresEstado(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.primary
    )
    EstadoCotizacion.VENCIDA -> ColoresEstado(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.secondary
    )
    EstadoCotizacion.CONVERTIDA -> ColoresEstado(
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
        MaterialTheme.colorScheme.tertiary
    )
    EstadoCotizacion.ANULADA -> ColoresEstado(
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.onErrorContainer,
        MaterialTheme.colorScheme.error
    )
}

@Composable
fun coloresPedido(estado: EstadoPedido): ColoresEstado = when (estado) {
    EstadoPedido.PENDIENTE -> ColoresEstado(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.secondary
    )
    EstadoPedido.CONFIRMADO -> ColoresEstado(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.primary
    )
    EstadoPedido.ENTREGADO -> ColoresEstado(
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
        MaterialTheme.colorScheme.tertiary
    )
    EstadoPedido.CANCELADO -> ColoresEstado(
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.onErrorContainer,
        MaterialTheme.colorScheme.error
    )
}

@Composable
fun ChipEstado(texto: String, colores: ColoresEstado, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = colores.contenedor,
        contentColor = colores.contenido,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colores.lomo)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ChipCotizacion(estado: EstadoCotizacion, modifier: Modifier = Modifier) {
    ChipEstado(estado.etiqueta, coloresCotizacion(estado), modifier)
}

@Composable
fun ChipPedido(estado: EstadoPedido, modifier: Modifier = Modifier) {
    ChipEstado(estado.etiqueta, coloresPedido(estado), modifier)
}

@Composable
fun Barcode(
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current
) {
    val barras = androidx.compose.runtime.remember(texto) {
        val random = Random(texto.hashCode())
        List(34) { random.nextFloat() }
    }
    Canvas(modifier) {
        val anchoTotal = size.width
        val alto = size.height
        val unidad = anchoTotal / (barras.size * 2f)
        var x = 0f
        barras.forEachIndexed { indice, valor ->
            val ancho = unidad * (0.6f + valor * 1.4f)
            if (indice % 2 == 0) {
                drawRect(
                    color = color,
                    topLeft = Offset(x, 0f),
                    size = Size(ancho.coerceAtLeast(0.8f), alto)
                )
            }
            x += ancho
        }
    }
}

@Composable
fun DivisorPunteado(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    grosor: Dp = 1.dp
) {
    Canvas(modifier.fillMaxWidth().height(grosor)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f))
        )
    }
}

@Composable
fun TicketCard(
    modifier: Modifier = Modifier,
    notchFraction: Float = 0.34f,
    muescas: Boolean = true,
    contenido: @Composable () -> Unit
) {
    val fondo = MaterialTheme.colorScheme.surface
    val exterior = MaterialTheme.colorScheme.background
    val radio = with(LocalDensity.current) { 9.dp.toPx() }
    Box(modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = fondo,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            contenido()
        }
        if (muescas) {
            Canvas(Modifier.matchParentSize()) {
                val y = size.height * notchFraction
                drawCircle(exterior, radio, Offset(0f, y))
                drawCircle(exterior, radio, Offset(size.width, y))
            }
        }
    }
}

@Composable
fun TarjetaDocumento(
    tipo: String,
    folio: String,
    cliente: String,
    detalle: String,
    total: Double,
    colores: ColoresEstado,
    estado: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    extra: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .drawBehind {
                    drawRect(
                        color = colores.lomo,
                        size = Size(4.dp.toPx(), size.height)
                    )
                }
                .padding(start = 18.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Etiqueta(tipo)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = folio,
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlexMono),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = cliente,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (extra != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = extra,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                ChipEstado(estado, colores)
                Spacer(Modifier.height(8.dp))
                Monto(
                    valor = total,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun Encabezado(
    titulo: String,
    modifier: Modifier = Modifier,
    sobretitulo: String? = null,
    subtitulo: String? = null,
    onAtras: (() -> Unit)? = null,
    acciones: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onAtras != null) {
            IconButton(onClick = onAtras, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Regresar"
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            if (sobretitulo != null) {
                Etiqueta(sobretitulo)
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = titulo,
                style = if (onAtras == null) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (subtitulo != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (acciones != null) {
            Row(verticalAlignment = Alignment.CenterVertically, content = acciones)
        }
    }
}

@Composable
fun TituloSeccion(
    texto: String,
    modifier: Modifier = Modifier,
    contador: Int? = null,
    accion: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Etiqueta(texto)
        if (contador != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = contador.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.weight(1f))
        accion?.invoke()
    }
}

@Composable
fun CampoTexto(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    icono: ImageVector? = null,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    minLines: Int = 1,
    readOnly: Boolean = false,
    sufijo: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(etiqueta) },
        placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        leadingIcon = icono?.let {
            { Icon(imageVector = it, contentDescription = null) }
        },
        suffix = sufijo?.let { { Text(it, style = MaterialTheme.typography.labelMedium) } },
        isError = error != null,
        supportingText = error?.let {
            { Text(it, style = MaterialTheme.typography.bodySmall) }
        },
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        minLines = minLines,
        readOnly = readOnly,
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            errorContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun Buscador(
    valor: String,
    onValorChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (valor.isNotBlank()) {
            {
                IconButton(onClick = { onValorChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChipsFiltro(
    opciones: List<String>,
    seleccionada: String,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(opciones) { opcion ->
            val activo = opcion == seleccionada
            FilterChip(
                selected = activo,
                onClick = { onSeleccion(opcion) },
                label = { Text(opcion, style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = activo,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun CampoFecha(
    etiqueta: String,
    fecha: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Formato.fecha(fecha),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlexMono),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoFecha(
    visible: Boolean,
    inicial: LocalDate,
    onDismiss: () -> Unit,
    onSeleccion: (LocalDate) -> Unit
) {
    if (!visible) return
    val estado = rememberDatePickerState(initialSelectedDateMillis = inicial.aMillisUtc())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    estado.selectedDateMillis?.let { onSeleccion(it.aLocalDateUtc()) }
                    onDismiss()
                }
            ) { Text("Elegir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        DatePicker(state = estado)
    }
}

@Composable
fun StepperCantidad(
    cantidad: Int,
    onCambio: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minimo: Int = 1,
    maximo: Int = 999
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onCambio((cantidad - 1).coerceAtLeast(minimo)) },
                enabled = cantidad > minimo,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Quitar una unidad")
            }
            Text(
                text = cantidad.toString(),
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = PlexMono),
                modifier = Modifier.width(28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(
                onClick = { onCambio((cantidad + 1).coerceAtMost(maximo)) },
                enabled = cantidad < maximo,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Agregar una unidad")
            }
        }
    }
}

@Composable
fun EmptyState(
    icono: ImageVector,
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icono, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (accion != null) {
            Spacer(Modifier.height(18.dp))
            accion()
        }
    }
}

@Composable
fun AvatarIniciales(nombre: String, modifier: Modifier = Modifier, tamano: Dp = 42.dp) {
    Surface(
        modifier = modifier.size(tamano),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = Formato.iniciales(nombre),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun FilaTotal(
    etiqueta: String,
    valor: Double?,
    modifier: Modifier = Modifier,
    enfasis: Boolean = false,
    negativo: Boolean = false,
    textoValor: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            style = if (enfasis) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (enfasis) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f)
        )
        Text(
            text = textoValor ?: (valor?.let { Formato.moneda(it) } ?: ""),
            style = (if (enfasis) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium)
                .merge(EstiloMonto),
            color = when {
                negativo -> MaterialTheme.colorScheme.secondary
                enfasis -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = habilitado,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun MensajeError(texto: String?, modifier: Modifier = Modifier) {
    if (texto.isNullOrBlank()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
