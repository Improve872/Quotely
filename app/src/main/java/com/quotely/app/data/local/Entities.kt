package com.quotely.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "responsables",
    indices = [Index(value = ["usuario"], unique = true)]
)
data class ResponsableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val usuario: String,
    val passwordHash: String,
    val salt: String,
    val rol: String = "VENDEDOR",
    val activo: Boolean = true,
    val creadoEn: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "productos",
    indices = [Index(value = ["codigo"], unique = true)]
)
data class ProductoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigo: String,
    val nombre: String,
    val descripcion: String = "",
    val precioBase: Double,
    val unidad: String = "pieza",
    val activo: Boolean = true,
    val creadoEn: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cotizaciones",
    indices = [
        Index(value = ["folio"], unique = true),
        Index(value = ["responsableId"]),
        Index(value = ["estado"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ResponsableEntity::class,
            parentColumns = ["id"],
            childColumns = ["responsableId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class CotizacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folio: String,
    val cliente: String,
    val clienteContacto: String = "",
    val fechaEmision: Long,
    val fechaCaducidad: Long,
    val responsableId: Long,
    val estado: String,
    val subtotal: Double,
    val descuentoVolumen: Double,
    val descuentoGlobal: Double,
    val tipoDescuentoGlobal: String,
    val valorDescuentoGlobal: Double,
    val iva: Double,
    val total: Double,
    val notas: String = "",
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cotizacion_items",
    indices = [Index(value = ["cotizacionId"])],
    foreignKeys = [
        ForeignKey(
            entity = CotizacionEntity::class,
            parentColumns = ["id"],
            childColumns = ["cotizacionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CotizacionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cotizacionId: Long,
    val productoId: Long?,
    val codigo: String,
    val descripcion: String,
    val unidad: String,
    val precioUnitario: Double,
    val cantidad: Int,
    val descuentoVolumenPct: Double,
    val descuentoVolumen: Double,
    val importe: Double,
    val orden: Int
)

@Entity(
    tableName = "pedidos",
    indices = [
        Index(value = ["folio"], unique = true),
        Index(value = ["responsableId"]),
        Index(value = ["cotizacionId"], unique = true),
        Index(value = ["estado"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ResponsableEntity::class,
            parentColumns = ["id"],
            childColumns = ["responsableId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CotizacionEntity::class,
            parentColumns = ["id"],
            childColumns = ["cotizacionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class PedidoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folio: String,
    val cliente: String,
    val fecha: Long,
    val responsableId: Long,
    val cotizacionId: Long? = null,
    val origen: String,
    val estado: String,
    val subtotal: Double,
    val descuentoVolumen: Double,
    val descuentoGlobal: Double,
    val tipoDescuentoGlobal: String = "NINGUNO",
    val valorDescuentoGlobal: Double = 0.0,
    val iva: Double,
    val total: Double,
    val notas: String = "",
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "pedido_items",
    indices = [Index(value = ["pedidoId"])],
    foreignKeys = [
        ForeignKey(
            entity = PedidoEntity::class,
            parentColumns = ["id"],
            childColumns = ["pedidoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PedidoItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pedidoId: Long,
    val productoId: Long?,
    val codigo: String,
    val descripcion: String,
    val unidad: String,
    val precioUnitario: Double,
    val cantidad: Int,
    val descuentoVolumenPct: Double,
    val descuentoVolumen: Double,
    val importe: Double,
    val orden: Int
)
