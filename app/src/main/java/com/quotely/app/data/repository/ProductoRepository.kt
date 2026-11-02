package com.quotely.app.data.repository

import com.quotely.app.data.local.ProductoDao
import com.quotely.app.data.local.ProductoEntity
import kotlinx.coroutines.flow.Flow

class ProductoRepository(private val dao: ProductoDao) {

    fun observarTodos(): Flow<List<ProductoEntity>> = dao.observarTodos()

    fun observarActivos(): Flow<List<ProductoEntity>> = dao.observarActivos()

    suspend fun buscarPorId(id: Long): ProductoEntity? = dao.buscarPorId(id)

    suspend fun guardar(producto: ProductoEntity): Long {
        val codigo = producto.codigo.trim().uppercase()
        val nombre = producto.nombre.trim()
        if (codigo.isBlank()) throw IllegalArgumentException("El código es obligatorio.")
        if (nombre.isBlank()) throw IllegalArgumentException("El nombre es obligatorio.")
        if (producto.precioBase < 0) throw IllegalArgumentException("El precio no puede ser negativo.")
        val existente = dao.buscarPorCodigo(codigo)
        if (existente != null && existente.id != producto.id) {
            throw IllegalArgumentException("Ya existe un producto con el código $codigo.")
        }
        val limpio = producto.copy(codigo = codigo, nombre = nombre, descripcion = producto.descripcion.trim())
        return if (limpio.id == 0L) {
            dao.insertar(limpio)
        } else {
            dao.actualizar(limpio)
            limpio.id
        }
    }

    suspend fun alternarActivo(producto: ProductoEntity) {
        dao.actualizar(producto.copy(activo = !producto.activo))
    }
}
