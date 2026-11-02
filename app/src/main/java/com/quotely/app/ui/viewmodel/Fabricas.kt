package com.quotely.app.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.quotely.app.AppContainer

@Composable
fun authViewModel(container: AppContainer): AuthViewModel = viewModel(
    factory = viewModelFactory {
        initializer { AuthViewModel(container.authRepository) }
    }
)

@Composable
fun inicioViewModel(container: AppContainer): InicioViewModel = viewModel(
    key = "inicio",
    factory = viewModelFactory {
        initializer {
            InicioViewModel(container.cotizacionRepository, container.pedidoRepository)
        }
    }
)

@Composable
fun cotizacionesViewModel(container: AppContainer): CotizacionesViewModel = viewModel(
    key = "cotizaciones",
    factory = viewModelFactory {
        initializer { CotizacionesViewModel(container.cotizacionRepository) }
    }
)

@Composable
fun cotizacionFormViewModel(container: AppContainer, cotizacionId: Long?): CotizacionFormViewModel =
    viewModel(
        key = "cotizacion_form_${cotizacionId ?: 0}",
        factory = viewModelFactory {
            initializer {
                CotizacionFormViewModel(
                    container.cotizacionRepository,
                    container.productoRepository,
                    cotizacionId
                )
            }
        }
    )

@Composable
fun cotizacionDetalleViewModel(container: AppContainer, cotizacionId: Long): CotizacionDetalleViewModel =
    viewModel(
        key = "cotizacion_detalle_$cotizacionId",
        factory = viewModelFactory {
            initializer {
                CotizacionDetalleViewModel(
                    container.cotizacionRepository,
                    container.authRepository,
                    cotizacionId
                )
            }
        }
    )

@Composable
fun pedidosViewModel(container: AppContainer): PedidosViewModel = viewModel(
    key = "pedidos",
    factory = viewModelFactory {
        initializer { PedidosViewModel(container.pedidoRepository) }
    }
)

@Composable
fun pedidoFormViewModel(container: AppContainer): PedidoFormViewModel = viewModel(
    key = "pedido_form",
    factory = viewModelFactory {
        initializer {
            PedidoFormViewModel(
                container.pedidoRepository,
                container.productoRepository
            )
        }
    }
)

@Composable
fun pedidoDetalleViewModel(container: AppContainer, pedidoId: Long): PedidoDetalleViewModel =
    viewModel(
        key = "pedido_detalle_$pedidoId",
        factory = viewModelFactory {
            initializer {
                PedidoDetalleViewModel(
                    container.pedidoRepository,
                    container.cotizacionRepository,
                    container.authRepository,
                    pedidoId
                )
            }
        }
    )

@Composable
fun productosViewModel(container: AppContainer): ProductosViewModel = viewModel(
    key = "productos",
    factory = viewModelFactory {
        initializer { ProductosViewModel(container.productoRepository) }
    }
)

@Composable
fun productoFormViewModel(container: AppContainer, productoId: Long?): ProductoFormViewModel =
    viewModel(
        key = "producto_form_${productoId ?: 0}",
        factory = viewModelFactory {
            initializer { ProductoFormViewModel(container.productoRepository, productoId) }
        }
    )

@Composable
fun responsablesViewModel(container: AppContainer): ResponsablesViewModel = viewModel(
    key = "responsables",
    factory = viewModelFactory {
        initializer { ResponsablesViewModel(container.authRepository) }
    }
)

@Composable
fun responsableFormViewModel(container: AppContainer, responsableId: Long?): ResponsableFormViewModel =
    viewModel(
        key = "responsable_form_${responsableId ?: 0}",
        factory = viewModelFactory {
            initializer { ResponsableFormViewModel(container.authRepository, responsableId) }
        }
    )
