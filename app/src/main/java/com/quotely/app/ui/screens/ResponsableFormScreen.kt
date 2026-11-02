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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotely.app.AppContainer
import com.quotely.app.data.repository.Rol
import com.quotely.app.ui.components.CampoTexto
import com.quotely.app.ui.components.ChipsFiltro
import com.quotely.app.ui.components.Etiqueta
import com.quotely.app.ui.components.LocalMostrarMensaje
import com.quotely.app.ui.components.MensajeError
import com.quotely.app.ui.components.TituloSeccion
import com.quotely.app.ui.theme.PlexCondensed
import com.quotely.app.ui.viewmodel.responsableFormViewModel
import kotlinx.coroutines.launch

@Composable
fun ResponsableFormScreen(
    container: AppContainer,
    responsableId: Long?,
    usuarioActualId: Long,
    onAtras: () -> Unit
) {
    val vm = responsableFormViewModel(container, responsableId)
    val estado by vm.state.collectAsStateWithLifecycle()
    val mostrarMensaje = LocalMostrarMensaje.current
    val esUnoMismo = estado.esEdicion && estado.id == usuarioActualId
    val roles = listOf(Rol.ADMIN, Rol.VENDEDOR)

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
                Etiqueta(if (estado.esEdicion) "EDITAR ACCESO" else "NUEVO ACCESO")
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (estado.esEdicion) "Responsable" else "Alta de responsable",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PlexCondensed)
                )
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
                    TituloSeccion("IDENTIDAD")
                    Spacer(Modifier.height(6.dp))
                    CampoTexto(
                        valor = estado.nombre,
                        onValorChange = vm::actualizarNombre,
                        etiqueta = "Nombre completo",
                        icono = Icons.Outlined.Person,
                        placeholder = "Ej. Ana Rivera"
                    )
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(
                        valor = estado.usuario,
                        onValorChange = vm::actualizarUsuario,
                        etiqueta = "Usuario",
                        placeholder = "Ej. ana.rivera"
                    )
                }

                item {
                    TituloSeccion("CREDENCIALES")
                    Spacer(Modifier.height(6.dp))
                    CampoTexto(
                        valor = estado.password,
                        onValorChange = vm::actualizarPassword,
                        etiqueta = if (estado.esEdicion) "Nueva contraseña (opcional)" else "Contraseña",
                        icono = Icons.Outlined.Lock,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (estado.esEdicion) {
                            "Déjala vacía para conservar la contraseña actual. Mínimo 6 caracteres."
                        } else {
                            "Mínimo 6 caracteres. Se guarda hasheada en el dispositivo."
                        },
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    TituloSeccion("PERMISOS")
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Acceso activo", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = if (esUnoMismo) {
                                        "No puedes desactivar tu propio acceso."
                                    } else {
                                        "Un usuario inactivo no puede iniciar sesión."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = estado.activo,
                                onCheckedChange = vm::actualizarActivo,
                                enabled = !esUnoMismo,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    disabledCheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Rol",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    ChipsFiltro(
                        opciones = roles.map { Rol.etiqueta(it) },
                        seleccionada = Rol.etiqueta(estado.rol),
                        onSeleccion = { etiqueta ->
                            roles.firstOrNull { Rol.etiqueta(it) == etiqueta }?.let(vm::actualizarRol)
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Los administradores también pueden gestionar los accesos del equipo.",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Button(
                    onClick = {
                        vm.guardar {
                            mostrarMensaje("Responsable guardado.")
                            onAtras()
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
                        Text("Guardar responsable", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
