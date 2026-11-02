package com.quotely.app.data.seed

import androidx.room.withTransaction
import com.quotely.app.data.local.CotizacionEntity
import com.quotely.app.data.local.CotizacionItemEntity
import com.quotely.app.data.local.PedidoEntity
import com.quotely.app.data.local.PedidoItemEntity
import com.quotely.app.data.local.ProductoEntity
import com.quotely.app.data.local.QuotelyDatabase
import com.quotely.app.data.local.ResponsableEntity
import com.quotely.app.data.repository.Folios
import com.quotely.app.domain.Calculadora
import com.quotely.app.domain.EstadoCotizacion
import com.quotely.app.domain.EstadoPedido
import com.quotely.app.domain.LineaEntrada
import com.quotely.app.domain.TipoDescuento
import com.quotely.app.domain.Totales
import com.quotely.app.util.PasswordHasher
import java.time.LocalDate

class DatabaseSeeder(private val db: QuotelyDatabase) {

    suspend fun seedIfNeeded() = db.withTransaction {
        if (db.responsableDao().contar() > 0 || db.productoDao().contar() > 0) {
            return@withTransaction
        }

        val adminId = db.responsableDao().insertar(
            usuario(
                nombre = "Ana Rivera",
                usuario = "admin",
                password = "admin123",
                rol = "ADMIN"
            )
        )
        val vendedorId = db.responsableDao().insertar(
            usuario(
                nombre = "Diego Salas",
                usuario = "vendedor",
                password = "vendedor123",
                rol = "VENDEDOR"
            )
        )

        val productos = listOf(
            ProductoEntity(
                codigo = "LAP-001",
                nombre = "Laptop Lenovo ThinkPad E14",
                descripcion = "Intel Core i5 · 16 GB RAM · 512 GB SSD · 14\"",
                precioBase = 18_990.00,
                unidad = "pieza"
            ),
            ProductoEntity(
                codigo = "MON-001",
                nombre = "Monitor Dell 24\" Full HD IPS",
                descripcion = "1920 × 1080 · HDMI y DisplayPort · base ajustable",
                precioBase = 3_450.00,
                unidad = "pieza"
            ),
            ProductoEntity(
                codigo = "TEC-001",
                nombre = "Teclado Logitech MX Keys",
                descripcion = "Inalámbrico · retroiluminado · multi-dispositivo",
                precioBase = 1_290.00,
                unidad = "pieza"
            ),
            ProductoEntity(
                codigo = "MOU-001",
                nombre = "Mouse Logitech MX Master 3S",
                descripcion = "Inalámbrico · 8 000 DPI · silencioso",
                precioBase = 899.00,
                unidad = "pieza"
            ),
            ProductoEntity(
                codigo = "IMP-001",
                nombre = "Impresora HP LaserJet MFP",
                descripcion = "Multifuncional láser · dúplex · Wi-Fi",
                precioBase = 4_750.00,
                unidad = "pieza"
            ),
            ProductoEntity(
                codigo = "SSD-001",
                nombre = "SSD Kingston NV2 1 TB",
                descripcion = "NVMe PCIe 4.0 · lectura 3 500 MB/s",
                precioBase = 1_349.00,
                unidad = "pieza"
            )
        )
        val ids = productos.associate { producto ->
            producto.codigo to db.productoDao().insertar(producto)
        }
        val catalogo = productos.mapIndexed { indice, producto ->
            producto to ids.getValue(producto.codigo)
        }.toMap()

        fun linea(codigo: String, cantidad: Int): LineaEntrada {
            val (producto, id) = catalogo.entries.first { it.key.codigo == codigo }.let {
                it.key to it.value
            }
            return LineaEntrada(
                productoId = id,
                codigo = producto.codigo,
                descripcion = producto.nombre,
                unidad = producto.unidad,
                precioUnitario = producto.precioBase,
                cantidad = cantidad
            )
        }

        val hoy = LocalDate.now()
        val ahora = System.currentTimeMillis()

        val andares = crearCotizacion(
            cliente = "Corporativo Andares",
            contacto = "Luis Mendoza · compras@andares.mx",
            emision = hoy,
            caducidad = hoy.plusDays(13),
            lineas = listOf(linea("LAP-001", 2), linea("MON-001", 12), linea("TEC-001", 2)),
            tipo = TipoDescuento.PORCENTAJE,
            valor = 3.0,
            estado = EstadoCotizacion.EMITIDA,
            responsableId = adminId,
            notas = "Entrega en un solo envío. Precios sujetos a existencias.",
            creadoEn = ahora - 3_600_000L
        )

        crearCotizacion(
            cliente = "Innovatech Solutions",
            contacto = "Karla Ruiz · karla@innovatech.mx",
            emision = hoy.minusDays(30),
            caducidad = hoy.minusDays(5),
            lineas = listOf(linea("IMP-001", 1), linea("SSD-001", 2), linea("TEC-001", 1)),
            tipo = TipoDescuento.NINGUNO,
            valor = 0.0,
            estado = EstadoCotizacion.EMITIDA,
            responsableId = vendedorId,
            notas = "Cotización de reposición trimestral.",
            creadoEn = ahora - 30 * 86_400_000L
        )

        val meridian = crearCotizacion(
            cliente = "Estudios Meridian",
            contacto = "Sofía Herrera · sofia@meridian.mx",
            emision = hoy.minusDays(10),
            caducidad = hoy.plusDays(5),
            lineas = listOf(linea("LAP-001", 3), linea("MON-001", 6)),
            tipo = TipoDescuento.PORCENTAJE,
            valor = 2.0,
            estado = EstadoCotizacion.CONVERTIDA,
            responsableId = vendedorId,
            notas = "Proyecto de renovación de estaciones de trabajo.",
            creadoEn = ahora - 10 * 86_400_000L
        )
        crearPedidoDesdeCotizacion(
            cotizacionId = meridian,
            fecha = hoy.minusDays(8),
            estado = EstadoPedido.ENTREGADO,
            responsableId = vendedorId,
            creadoEn = ahora - 8 * 86_400_000L
        )

        val pedidoDirecto = Calculadora.calcular(
            listOf(linea("MOU-001", 5), linea("TEC-001", 2)),
            tipoDescuento = TipoDescuento.NINGUNO,
            aplicarDescuentos = false
        )
        val prefijo = Folios.prefijo("PED", hoy.minusDays(1))
        val folio = Folios.siguiente(db.pedidoDao().ultimoFolio(prefijo), prefijo)
        val pedidoId = db.pedidoDao().insertar(
            PedidoEntity(
                folio = folio,
                cliente = "Distribuidora del Norte",
                fecha = hoy.minusDays(1).toEpochDay(),
                responsableId = adminId,
                cotizacionId = null,
                origen = "DIRECTO",
                estado = EstadoPedido.CONFIRMADO.name,
                subtotal = pedidoDirecto.subtotal,
                descuentoVolumen = 0.0,
                descuentoGlobal = 0.0,
                tipoDescuentoGlobal = TipoDescuento.NINGUNO.name,
                valorDescuentoGlobal = 0.0,
                iva = pedidoDirecto.iva,
                total = pedidoDirecto.total,
                notas = "Pedido de mostrador. Precios base del catálogo.",
                creadoEn = ahora - 86_400_000L,
                actualizadoEn = ahora - 86_400_000L
            )
        )
        db.pedidoDao().insertarItems(
            pedidoDirecto.lineas.mapIndexed { indice, item ->
                PedidoItemEntity(
                    pedidoId = pedidoId,
                    productoId = item.productoId,
                    codigo = item.codigo,
                    descripcion = item.descripcion,
                    unidad = item.unidad,
                    precioUnitario = item.precioUnitario,
                    cantidad = item.cantidad,
                    descuentoVolumenPct = 0.0,
                    descuentoVolumen = 0.0,
                    importe = item.importe,
                    orden = indice
                )
            }
        )

        // La cotización de Andares queda vigente; el pedido directo se registra aparte.
        check(andares > 0)
    }

    private fun usuario(nombre: String, usuario: String, password: String, rol: String): ResponsableEntity {
        val salt = PasswordHasher.nuevoSalt()
        return ResponsableEntity(
            nombre = nombre,
            usuario = usuario,
            passwordHash = PasswordHasher.hash(password, salt),
            salt = salt,
            rol = rol
        )
    }

    private suspend fun crearCotizacion(
        cliente: String,
        contacto: String,
        emision: LocalDate,
        caducidad: LocalDate,
        lineas: List<LineaEntrada>,
        tipo: TipoDescuento,
        valor: Double,
        estado: EstadoCotizacion,
        responsableId: Long,
        notas: String,
        creadoEn: Long
    ): Long {
        val totales: Totales = Calculadora.calcular(lineas, tipo, valor)
        val prefijo = Folios.prefijo("COT", emision)
        val folio = Folios.siguiente(db.cotizacionDao().ultimoFolio(prefijo), prefijo)
        val id = db.cotizacionDao().insertar(
            CotizacionEntity(
                folio = folio,
                cliente = cliente,
                clienteContacto = contacto,
                fechaEmision = emision.toEpochDay(),
                fechaCaducidad = caducidad.toEpochDay(),
                responsableId = responsableId,
                estado = estado.name,
                subtotal = totales.subtotal,
                descuentoVolumen = totales.descuentoVolumen,
                descuentoGlobal = totales.descuentoGlobal,
                tipoDescuentoGlobal = tipo.name,
                valorDescuentoGlobal = valor,
                iva = totales.iva,
                total = totales.total,
                notas = notas,
                creadoEn = creadoEn,
                actualizadoEn = creadoEn
            )
        )
        db.cotizacionDao().insertarItems(
            totales.lineas.mapIndexed { indice, item ->
                CotizacionItemEntity(
                    cotizacionId = id,
                    productoId = item.productoId,
                    codigo = item.codigo,
                    descripcion = item.descripcion,
                    unidad = item.unidad,
                    precioUnitario = item.precioUnitario,
                    cantidad = item.cantidad,
                    descuentoVolumenPct = item.descuentoVolumenPct,
                    descuentoVolumen = item.descuentoVolumen,
                    importe = item.importe,
                    orden = indice
                )
            }
        )
        return id
    }

    private suspend fun crearPedidoDesdeCotizacion(
        cotizacionId: Long,
        fecha: LocalDate,
        estado: EstadoPedido,
        responsableId: Long,
        creadoEn: Long
    ) {
        val detalle = db.cotizacionDao().obtenerDetalle(cotizacionId) ?: return
        val prefijo = Folios.prefijo("PED", fecha)
        val folio = Folios.siguiente(db.pedidoDao().ultimoFolio(prefijo), prefijo)
        val pedidoId = db.pedidoDao().insertar(
            PedidoEntity(
                folio = folio,
                cliente = detalle.cotizacion.cliente,
                fecha = fecha.toEpochDay(),
                responsableId = responsableId,
                cotizacionId = cotizacionId,
                origen = "COTIZACION",
                estado = estado.name,
                subtotal = detalle.cotizacion.subtotal,
                descuentoVolumen = detalle.cotizacion.descuentoVolumen,
                descuentoGlobal = detalle.cotizacion.descuentoGlobal,
                tipoDescuentoGlobal = detalle.cotizacion.tipoDescuentoGlobal,
                valorDescuentoGlobal = detalle.cotizacion.valorDescuentoGlobal,
                iva = detalle.cotizacion.iva,
                total = detalle.cotizacion.total,
                notas = "Precios y descuentos conservados de ${detalle.cotizacion.folio}.",
                creadoEn = creadoEn,
                actualizadoEn = creadoEn
            )
        )
        db.pedidoDao().insertarItems(
            detalle.itemsOrdenados.mapIndexed { indice, item ->
                PedidoItemEntity(
                    pedidoId = pedidoId,
                    productoId = item.productoId,
                    codigo = item.codigo,
                    descripcion = item.descripcion,
                    unidad = item.unidad,
                    precioUnitario = item.precioUnitario,
                    cantidad = item.cantidad,
                    descuentoVolumenPct = item.descuentoVolumenPct,
                    descuentoVolumen = item.descuentoVolumen,
                    importe = item.importe,
                    orden = indice
                )
            }
        )
    }
}
