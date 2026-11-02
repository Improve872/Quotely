package com.quotely.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotely.app.data.local.ResponsableEntity
import com.quotely.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface EstadoSesion {
    data object Cargando : EstadoSesion
    data object SinSesion : EstadoSesion
    data class Autenticado(val usuario: ResponsableEntity) : EstadoSesion
}

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {
    private val _estado = MutableStateFlow<EstadoSesion>(EstadoSesion.Cargando)
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    init {
        viewModelScope.launch {
            repo.sesionId.collectLatest { id ->
                if (id == null) {
                    _estado.value = EstadoSesion.SinSesion
                } else {
                    repo.observarPorId(id).collect { usuario ->
                        if (usuario == null || !usuario.activo) {
                            repo.cerrarSesion()
                            _estado.value = EstadoSesion.SinSesion
                        } else {
                            _estado.value = EstadoSesion.Autenticado(usuario)
                        }
                    }
                }
            }
        }
    }

    fun iniciarSesion(usuario: String, password: String) {
        if (_cargando.value) return
        viewModelScope.launch {
            _error.value = null
            _cargando.value = true
            runCatching { repo.iniciarSesion(usuario, password) }
                .onFailure { _error.value = it.message ?: "No se pudo iniciar sesión." }
            _cargando.value = false
        }
    }

    fun limpiarError() {
        _error.value = null
    }

    fun cerrarSesion() {
        viewModelScope.launch { repo.cerrarSesion() }
    }
}
