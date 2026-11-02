package com.quotely.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotely.app.data.local.ResponsableEntity
import com.quotely.app.data.repository.AuthRepository
import com.quotely.app.data.repository.Rol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResponsablesUiState(
    val cargando: Boolean = true,
    val items: List<ResponsableEntity> = emptyList()
)

class ResponsablesViewModel(private val repo: AuthRepository) : ViewModel() {
    val uiState = repo.observarResponsables().map {
        ResponsablesUiState(cargando = false, items = it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ResponsablesUiState()
    )
}

data class ResponsableFormState(
    val cargando: Boolean = true,
    val id: Long = 0,
    val nombre: String = "",
    val usuario: String = "",
    val password: String = "",
    val rol: String = Rol.VENDEDOR,
    val activo: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null
) {
    val esEdicion: Boolean get() = id > 0
}

class ResponsableFormViewModel(
    private val repo: AuthRepository,
    private val responsableId: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(ResponsableFormState(cargando = responsableId != null))
    val state = _state.asStateFlow()

    init {
        if (responsableId != null) {
            viewModelScope.launch {
                val responsable = repo.buscarPorId(responsableId)
                if (responsable == null) {
                    _state.update { it.copy(cargando = false, error = "El responsable ya no existe.") }
                } else {
                    _state.update {
                        it.copy(
                            cargando = false,
                            id = responsable.id,
                            nombre = responsable.nombre,
                            usuario = responsable.usuario,
                            rol = responsable.rol,
                            activo = responsable.activo
                        )
                    }
                }
            }
        }
    }

    fun actualizarNombre(valor: String) = _state.update { it.copy(nombre = valor, error = null) }

    fun actualizarUsuario(valor: String) = _state.update { it.copy(usuario = valor, error = null) }

    fun actualizarPassword(valor: String) = _state.update { it.copy(password = valor, error = null) }

    fun actualizarRol(valor: String) = _state.update { it.copy(rol = valor) }

    fun actualizarActivo(valor: Boolean) = _state.update { it.copy(activo = valor) }

    fun guardar(onExito: () -> Unit) {
        val actual = _state.value
        if (actual.guardando) return
        viewModelScope.launch {
            _state.update { it.copy(guardando = true, error = null) }
            runCatching {
                if (actual.esEdicion) {
                    repo.actualizarResponsable(
                        id = actual.id,
                        nombre = actual.nombre,
                        usuario = actual.usuario,
                        rol = actual.rol,
                        activo = actual.activo,
                        passwordNueva = actual.password.takeIf { it.isNotBlank() }
                    )
                } else {
                    repo.crearResponsable(
                        nombre = actual.nombre,
                        usuario = actual.usuario,
                        password = actual.password,
                        rol = actual.rol
                    )
                }
            }.onSuccess {
                _state.update { it.copy(guardando = false) }
                onExito()
            }.onFailure { fallo ->
                _state.update {
                    it.copy(
                        guardando = false,
                        error = fallo.message ?: "No se pudo guardar el responsable."
                    )
                }
            }
        }
    }

    fun limpiarError() = _state.update { it.copy(error = null) }
}
