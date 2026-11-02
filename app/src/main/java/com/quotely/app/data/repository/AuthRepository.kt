package com.quotely.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quotely.app.data.local.ResponsableDao
import com.quotely.app.data.local.ResponsableEntity
import com.quotely.app.util.PasswordHasher
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.sesionDataStore by preferencesDataStore(name = "sesion_quotely")

object Rol {
    const val ADMIN = "ADMIN"
    const val VENDEDOR = "VENDEDOR"

    fun etiqueta(valor: String): String = when (valor) {
        ADMIN -> "Administrador"
        else -> "Vendedor"
    }
}

class AuthRepository(
    private val context: Context,
    private val dao: ResponsableDao
) {
    private val claveSesion = longPreferencesKey("responsable_id")

    val sesionId: Flow<Long?> = context.sesionDataStore.data
        .catch { error ->
            if (error is IOException) {
                Log.w("QuotelyAuth", "No se pudo leer la sesión", error)
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferencias -> preferencias[claveSesion] }

    fun observarResponsables(): Flow<List<ResponsableEntity>> = dao.observarTodos()

    fun observarPorId(id: Long): Flow<ResponsableEntity?> = dao.observarPorId(id)

    suspend fun buscarPorId(id: Long): ResponsableEntity? = dao.buscarPorId(id)

    suspend fun iniciarSesion(usuario: String, password: String): ResponsableEntity {
        val limpio = usuario.trim()
        if (limpio.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("Escribe tu usuario y contraseña.")
        }
        val responsable = dao.buscarPorUsuario(limpio)
            ?: throw IllegalArgumentException("No existe un responsable con ese usuario.")
        if (!responsable.activo) {
            throw IllegalArgumentException("Ese usuario está desactivado. Habla con el administrador.")
        }
        if (!PasswordHasher.verificar(password, responsable.salt, responsable.passwordHash)) {
            throw IllegalArgumentException("La contraseña no es correcta.")
        }
        context.sesionDataStore.edit { it[claveSesion] = responsable.id }
        return responsable
    }

    suspend fun cerrarSesion() {
        context.sesionDataStore.edit { it.remove(claveSesion) }
    }

    suspend fun crearResponsable(
        nombre: String,
        usuario: String,
        password: String,
        rol: String
    ): Long {
        val nombreLimpio = nombre.trim()
        val usuarioLimpio = usuario.trim()
        if (nombreLimpio.isBlank()) throw IllegalArgumentException("El nombre es obligatorio.")
        if (usuarioLimpio.length < 3) throw IllegalArgumentException("El usuario debe tener al menos 3 caracteres.")
        if (password.length < 6) throw IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.")
        if (dao.buscarPorUsuario(usuarioLimpio) != null) {
            throw IllegalArgumentException("Ese usuario ya está registrado.")
        }
        val salt = PasswordHasher.nuevoSalt()
        return dao.insertar(
            ResponsableEntity(
                nombre = nombreLimpio,
                usuario = usuarioLimpio,
                passwordHash = PasswordHasher.hash(password, salt),
                salt = salt,
                rol = rol
            )
        )
    }

    suspend fun actualizarResponsable(
        id: Long,
        nombre: String,
        usuario: String,
        rol: String,
        activo: Boolean,
        passwordNueva: String?
    ) {
        val actual = dao.buscarPorId(id)
            ?: throw IllegalArgumentException("El responsable ya no existe.")
        val nombreLimpio = nombre.trim()
        val usuarioLimpio = usuario.trim()
        if (nombreLimpio.isBlank()) throw IllegalArgumentException("El nombre es obligatorio.")
        if (usuarioLimpio.length < 3) throw IllegalArgumentException("El usuario debe tener al menos 3 caracteres.")
        val duplicado = dao.buscarPorUsuario(usuarioLimpio)
        if (duplicado != null && duplicado.id != id) {
            throw IllegalArgumentException("Ese usuario ya está registrado.")
        }
        val salt = if (passwordNueva.isNullOrBlank()) actual.salt else PasswordHasher.nuevoSalt()
        val hash = if (passwordNueva.isNullOrBlank()) {
            actual.passwordHash
        } else {
            if (passwordNueva.length < 6) {
                throw IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.")
            }
            PasswordHasher.hash(passwordNueva, salt)
        }
        dao.actualizar(
            actual.copy(
                nombre = nombreLimpio,
                usuario = usuarioLimpio,
                rol = rol,
                activo = activo,
                salt = salt,
                passwordHash = hash
            )
        )
    }
}
