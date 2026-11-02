package com.quotely.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quotely.app.AppContainer
import com.quotely.app.data.local.ResponsableEntity
import com.quotely.app.ui.components.ProporcionarSnackbar
import com.quotely.app.ui.screens.AjustesScreen
import com.quotely.app.ui.screens.CotizacionDetalleScreen
import com.quotely.app.ui.screens.CotizacionFormScreen
import com.quotely.app.ui.screens.CotizacionesScreen
import com.quotely.app.ui.screens.InicioScreen
import com.quotely.app.ui.screens.PedidoDetalleScreen
import com.quotely.app.ui.screens.PedidoFormScreen
import com.quotely.app.ui.screens.PedidosScreen
import com.quotely.app.ui.screens.ProductoFormScreen
import com.quotely.app.ui.screens.ProductosScreen
import com.quotely.app.ui.screens.ResponsableFormScreen
import com.quotely.app.ui.screens.ResponsablesScreen

object Rutas {
    const val INICIO = "inicio"
    const val COTIZACIONES = "cotizaciones"
    const val PEDIDOS = "pedidos"
    const val PRODUCTOS = "productos"
    const val AJUSTES = "ajustes"
    const val RESPONSABLES = "responsables"

    const val COTIZACION_DETALLE = "cotizacion/{cotizacionId}"
    const val COTIZACION_FORM = "cotizacion_form?cotizacionId={cotizacionId}"
    const val PEDIDO_DETALLE = "pedido/{pedidoId}"
    const val PEDIDO_FORM = "pedido_form"
    const val PRODUCTO_FORM = "producto_form?productoId={productoId}"
    const val RESPONSABLE_FORM = "responsable_form?responsableId={responsableId}"

    fun cotizacionDetalle(id: Long) = "cotizacion/$id"
    fun cotizacionForm(id: Long? = null) = "cotizacion_form?cotizacionId=${id ?: -1L}"
    fun pedidoDetalle(id: Long) = "pedido/$id"
    fun productoForm(id: Long? = null) = "producto_form?productoId=${id ?: -1L}"
    fun responsableForm(id: Long? = null) = "responsable_form?responsableId=${id ?: -1L}"
}

private data class ItemBarra(
    val ruta: String,
    val etiqueta: String,
    val iconoActivo: ImageVector,
    val iconoInactivo: ImageVector
)

@Composable
fun AppNavigation(
    container: AppContainer,
    usuario: ResponsableEntity,
    onCerrarSesion: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.route

    val items = listOf(
        ItemBarra(Rutas.INICIO, "Inicio", Icons.Filled.Home, Icons.Outlined.Home),
        ItemBarra(Rutas.COTIZACIONES, "Cotizaciones", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
        ItemBarra(Rutas.PEDIDOS, "Pedidos", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag),
        ItemBarra(Rutas.PRODUCTOS, "Productos", Icons.Filled.Inventory2, Icons.Outlined.Inventory2)
    )
    val mostrarBarra = items.any { it.ruta == rutaActual }

    fun irASeccion(ruta: String) {
        navController.navigate(ruta) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun reemplazarActual(ruta: String) {
        val actual = navController.currentDestination?.route
        navController.navigate(ruta) {
            if (actual != null) {
                popUpTo(actual) { inclusive = true }
            }
            launchSingleTop = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (mostrarBarra) {
                BarraNavegacion(
                    items = items,
                    rutaActual = rutaActual,
                    onSeleccion = ::irASeccion
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        ProporcionarSnackbar(snackbarHostState) {
            NavHost(
                navController = navController,
                startDestination = Rutas.INICIO,
                modifier = Modifier.padding(padding)
            ) {
                composable(Rutas.INICIO) {
                    InicioScreen(
                        container = container,
                        usuario = usuario,
                        onNuevaCotizacion = { navController.navigate(Rutas.cotizacionForm()) },
                        onNuevoPedido = { navController.navigate(Rutas.PEDIDO_FORM) },
                        onAbrirCotizacion = { navController.navigate(Rutas.cotizacionDetalle(it)) },
                        onAbrirPedido = { navController.navigate(Rutas.pedidoDetalle(it)) },
                        onAbrirAjustes = { navController.navigate(Rutas.AJUSTES) }
                    )
                }

                composable(Rutas.COTIZACIONES) {
                    CotizacionesScreen(
                        container = container,
                        onAbrirCotizacion = { navController.navigate(Rutas.cotizacionDetalle(it)) },
                        onNuevaCotizacion = { navController.navigate(Rutas.cotizacionForm()) }
                    )
                }

                composable(
                    route = Rutas.COTIZACION_DETALLE,
                    arguments = listOf(navArgument("cotizacionId") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("cotizacionId") ?: return@composable
                    CotizacionDetalleScreen(
                        container = container,
                        usuario = usuario,
                        cotizacionId = id,
                        onAtras = { navController.popBackStack() },
                        onEditar = { navController.navigate(Rutas.cotizacionForm(it)) },
                        onVerPedido = { navController.navigate(Rutas.pedidoDetalle(it)) }
                    )
                }

                composable(
                    route = Rutas.COTIZACION_FORM,
                    arguments = listOf(
                        navArgument("cotizacionId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    )
                ) { entry ->
                    val id = entry.arguments?.getLong("cotizacionId")?.takeIf { it > 0 }
                    CotizacionFormScreen(
                        container = container,
                        usuario = usuario,
                        cotizacionId = id,
                        onAtras = { navController.popBackStack() },
                        onGuardada = { guardado ->
                            reemplazarActual(Rutas.cotizacionDetalle(guardado.id))
                        }
                    )
                }

                composable(Rutas.PEDIDOS) {
                    PedidosScreen(
                        container = container,
                        onAbrirPedido = { navController.navigate(Rutas.pedidoDetalle(it)) },
                        onNuevoPedido = { navController.navigate(Rutas.PEDIDO_FORM) },
                        onIrCotizaciones = { irASeccion(Rutas.COTIZACIONES) }
                    )
                }

                composable(
                    route = Rutas.PEDIDO_DETALLE,
                    arguments = listOf(navArgument("pedidoId") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("pedidoId") ?: return@composable
                    PedidoDetalleScreen(
                        container = container,
                        pedidoId = id,
                        onAtras = { navController.popBackStack() }
                    )
                }

                composable(Rutas.PEDIDO_FORM) {
                    PedidoFormScreen(
                        container = container,
                        usuario = usuario,
                        onAtras = { navController.popBackStack() },
                        onGuardado = { guardado ->
                            reemplazarActual(Rutas.pedidoDetalle(guardado.id))
                        }
                    )
                }

                composable(Rutas.PRODUCTOS) {
                    ProductosScreen(
                        container = container,
                        onAbrirProducto = { navController.navigate(Rutas.productoForm(it)) },
                        onNuevoProducto = { navController.navigate(Rutas.productoForm()) }
                    )
                }

                composable(
                    route = Rutas.PRODUCTO_FORM,
                    arguments = listOf(
                        navArgument("productoId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    )
                ) { entry ->
                    val id = entry.arguments?.getLong("productoId")?.takeIf { it > 0 }
                    ProductoFormScreen(
                        container = container,
                        productoId = id,
                        onAtras = { navController.popBackStack() },
                        onGuardado = { navController.popBackStack() }
                    )
                }

                composable(Rutas.AJUSTES) {
                    AjustesScreen(
                        container = container,
                        usuario = usuario,
                        onAtras = { navController.popBackStack() },
                        onAbrirResponsables = { navController.navigate(Rutas.RESPONSABLES) },
                        onCerrarSesion = onCerrarSesion
                    )
                }

                composable(Rutas.RESPONSABLES) {
                    ResponsablesScreen(
                        container = container,
                        usuarioActualId = usuario.id,
                        onAtras = { navController.popBackStack() },
                        onAbrirResponsable = { navController.navigate(Rutas.responsableForm(it)) },
                        onNuevoResponsable = { navController.navigate(Rutas.responsableForm()) }
                    )
                }

                composable(
                    route = Rutas.RESPONSABLE_FORM,
                    arguments = listOf(
                        navArgument("responsableId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    )
                ) { entry ->
                    val id = entry.arguments?.getLong("responsableId")?.takeIf { it > 0 }
                    ResponsableFormScreen(
                        container = container,
                        responsableId = id,
                        usuarioActualId = usuario.id,
                        onAtras = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun BarraNavegacion(
    items: List<ItemBarra>,
    rutaActual: String?,
    onSeleccion: (String) -> Unit
) {
    val colorLinea = MaterialTheme.colorScheme.outlineVariant
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.drawBehind {
            drawLine(
                color = colorLinea,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        }
    ) {
        items.forEach { item ->
            val activo = rutaActual == item.ruta
            NavigationBarItem(
                selected = activo,
                onClick = { onSeleccion(item.ruta) },
                icon = {
                    Icon(
                        imageVector = if (activo) item.iconoActivo else item.iconoInactivo,
                        contentDescription = item.etiqueta
                    )
                },
                label = {
                    Text(
                        text = item.etiqueta,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
