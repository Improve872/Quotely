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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotely.app.AppContainer
import com.quotely.app.data.local.ResponsableEntity
import com.quotely.app.data.repository.Rol
import com.quotely.app.ui.components.AvatarIniciales
import com.quotely.app.ui.components.EmptyState
import com.quotely.app.ui.components.Encabezado
import com.quotely.app.ui.components.Etiqueta
import com.quotely.app.ui.viewmodel.responsablesViewModel

@Composable
fun ResponsablesScreen(
    container: AppContainer,
    usuarioActualId: Long,
    onAtras: () -> Unit,
    onAbrirResponsable: (Long) -> Unit,
    onNuevoResponsable: () -> Unit
) {
    val vm = responsablesViewModel(container)
    val estado by vm.uiState.collectAsStateWithLifecycle()
    val activos = estado.items.count { it.activo }
    val administradores = estado.items.count { it.rol == Rol.ADMIN }
    val leyendaAdmins = if (administradores == 1) "administrador" else "administradores"

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
                Encabezado(
                    titulo = "Responsables",
                    sobretitulo = "ACCESOS",
                    subtitulo = "$activos activos · $administradores $leyendaAdmins",
                    onAtras = onAtras,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }

            if (estado.items.isEmpty() && !estado.cargando) {
                item {
                    EmptyState(
                        icono = Icons.Outlined.People,
                        titulo = "Sin responsables",
                        mensaje = "Agrega a las personas que pueden registrar cotizaciones y pedidos.",
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                items(estado.items, key = { it.id }) { responsable ->
                    TarjetaResponsable(
                        responsable = responsable,
                        esUsuarioActual = responsable.id == usuarioActualId,
                        onClick = { onAbrirResponsable(responsable.id) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNuevoResponsable,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text("Nuevo", style = MaterialTheme.typography.labelLarge) }
        )
    }
}

@Composable
private fun TarjetaResponsable(
    responsable: ResponsableEntity,
    esUsuarioActual: Boolean,
    onClick: () -> Unit,
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarIniciales(responsable.nombre, tamano = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = responsable.nombre,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (esUsuarioActual) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "TÚ",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "@${responsable.usuario} · ${Rol.etiqueta(responsable.rol)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!responsable.activo) {
                    Spacer(Modifier.height(2.dp))
                    Etiqueta("INACTIVO", color = MaterialTheme.colorScheme.error)
                }
            }
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Editar responsable",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
