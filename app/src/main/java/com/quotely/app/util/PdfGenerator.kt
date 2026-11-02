package com.quotely.app.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.quotely.app.data.local.CotizacionConItems
import com.quotely.app.data.local.PedidoConItems
import com.quotely.app.domain.EstadoCotizacion
import com.quotely.app.domain.EstadoPedido
import com.quotely.app.domain.TipoDescuento
import com.quotely.app.domain.estadoEfectivoCotizacion
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.random.Random

private const val ANCHO_PAGINA = 595
private const val ALTO_PAGINA = 842
private const val MARGEN = 40f
private const val CORTE_TABLA = 620f

private val TINTA = AndroidColor.rgb(18, 26, 43)
private val TINTA_SUAVE = AndroidColor.rgb(86, 96, 114)
private val ACERO = AndroidColor.rgb(107, 114, 128)
private val COBALTO = AndroidColor.rgb(36, 71, 216)
private val SENAL = AndroidColor.rgb(232, 89, 12)
private val EXITO = AndroidColor.rgb(31, 138, 91)
private val LINEA = AndroidColor.rgb(214, 219, 210)
private val PAPEL = AndroidColor.rgb(250, 251, 248)

private val SANS = Typeface.create("sans-serif", Typeface.NORMAL)
private val SANS_MEDIA = Typeface.create("sans-serif-medium", Typeface.NORMAL)
private val CONDENSADA = Typeface.create("sans-serif-condensed", Typeface.BOLD)
private val MONO = Typeface.MONOSPACE

private data class LineaPdf(
    val descripcion: String,
    val codigo: String,
    val unidad: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val descuentoPct: Double,
    val descuento: Double,
    val importe: Double
)

private data class DocumentoPdfInfo(
    val tipo: String,
    val folio: String,
    val estado: String,
    val colorEstado: Int,
    val cliente: String,
    val contacto: String,
    val datosFecha: List<Pair<String, String>>,
    val lineas: List<LineaPdf>,
    val subtotal: Double,
    val descuentoVolumen: Double,
    val descuentoGlobal: Double,
    val etiquetaDescuentoGlobal: String?,
    val iva: Double,
    val total: Double,
    val notas: String,
    val responsable: String,
    val notaOrigen: String?
)

object PdfGenerator {

    suspend fun generarCotizacion(
        context: Context,
        detalle: CotizacionConItems,
        responsable: String
    ): Uri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val cotizacion = detalle.cotizacion
        val caducidad = LocalDate.ofEpochDay(cotizacion.fechaCaducidad)
        val emision = LocalDate.ofEpochDay(cotizacion.fechaEmision)
        val estado = estadoEfectivoCotizacion(
            EstadoCotizacion.desde(cotizacion.estado),
            caducidad
        )
        val info = DocumentoPdfInfo(
            tipo = "COTIZACIÓN",
            folio = cotizacion.folio,
            estado = estado.etiqueta,
            colorEstado = when (estado) {
                EstadoCotizacion.EMITIDA -> COBALTO
                EstadoCotizacion.VENCIDA -> SENAL
                EstadoCotizacion.CONVERTIDA -> EXITO
                EstadoCotizacion.ANULADA -> AndroidColor.rgb(180, 35, 24)
                EstadoCotizacion.BORRADOR -> ACERO
            },
            cliente = cotizacion.cliente,
            contacto = cotizacion.clienteContacto,
            datosFecha = listOf(
                "EMISIÓN" to Formato.fecha(emision),
                "CADUCIDAD" to Formato.fecha(caducidad)
            ),
            lineas = detalle.itemsOrdenados.map { item ->
                LineaPdf(
                    descripcion = item.descripcion,
                    codigo = item.codigo,
                    unidad = item.unidad,
                    cantidad = item.cantidad,
                    precioUnitario = item.precioUnitario,
                    descuentoPct = item.descuentoVolumenPct,
                    descuento = item.descuentoVolumen,
                    importe = item.importe
                )
            },
            subtotal = cotizacion.subtotal,
            descuentoVolumen = cotizacion.descuentoVolumen,
            descuentoGlobal = cotizacion.descuentoGlobal,
            etiquetaDescuentoGlobal = etiquetaDescuento(
                TipoDescuento.desde(cotizacion.tipoDescuentoGlobal),
                cotizacion.valorDescuentoGlobal
            ),
            iva = cotizacion.iva,
            total = cotizacion.total,
            notas = cotizacion.notas,
            responsable = responsable,
            notaOrigen = null
        )
        escribirPdf(context, info)
    }

    suspend fun generarPedido(
        context: Context,
        detalle: PedidoConItems,
        responsable: String,
        folioCotizacion: String?
    ): Uri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val pedido = detalle.pedido
        val fecha = LocalDate.ofEpochDay(pedido.fecha)
        val estado = EstadoPedido.desde(pedido.estado)
        val info = DocumentoPdfInfo(
            tipo = "PEDIDO",
            folio = pedido.folio,
            estado = estado.etiqueta,
            colorEstado = when (estado) {
                EstadoPedido.PENDIENTE -> SENAL
                EstadoPedido.CONFIRMADO -> COBALTO
                EstadoPedido.ENTREGADO -> EXITO
                EstadoPedido.CANCELADO -> AndroidColor.rgb(180, 35, 24)
            },
            cliente = pedido.cliente,
            contacto = "",
            datosFecha = listOf("FECHA" to Formato.fecha(fecha)),
            lineas = detalle.itemsOrdenados.map { item ->
                LineaPdf(
                    descripcion = item.descripcion,
                    codigo = item.codigo,
                    unidad = item.unidad,
                    cantidad = item.cantidad,
                    precioUnitario = item.precioUnitario,
                    descuentoPct = item.descuentoVolumenPct,
                    descuento = item.descuentoVolumen,
                    importe = item.importe
                )
            },
            subtotal = pedido.subtotal,
            descuentoVolumen = pedido.descuentoVolumen,
            descuentoGlobal = pedido.descuentoGlobal,
            etiquetaDescuentoGlobal = etiquetaDescuento(
                TipoDescuento.desde(pedido.tipoDescuentoGlobal),
                pedido.valorDescuentoGlobal
            ),
            iva = pedido.iva,
            total = pedido.total,
            notas = pedido.notas,
            responsable = responsable,
            notaOrigen = when {
                folioCotizacion != null ->
                    "Pedido generado desde $folioCotizacion · precios y descuentos conservados."
                else -> "Precios base del catálogo · pedido directo."
            }
        )
        escribirPdf(context, info)
    }

    private fun etiquetaDescuento(tipo: TipoDescuento, valor: Double): String? = when {
        tipo == TipoDescuento.PORCENTAJE && valor > 0 -> "Descuento global (${Formato.porcentaje(valor)})"
        tipo == TipoDescuento.MONTO && valor > 0 -> "Descuento global"
        else -> null
    }

    private fun escribirPdf(context: Context, info: DocumentoPdfInfo): Uri {
        val documento = PdfDocument()
        val pintor = PintorPdf(documento)
        pintor.nuevaPagina()
        dibujarCabecera(pintor, info, primera = true)
        dibujarTabla(pintor, info)
        dibujarTotales(pintor, info)
        dibujarNotas(pintor, info)
        pintor.cerrarPagina()

        val carpeta = File(context.cacheDir, "documentos").apply { mkdirs() }
        val nombre = "${info.folio.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf"
        val archivo = File(carpeta, nombre)
        FileOutputStream(archivo).use { documento.writeTo(it) }
        documento.close()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )
    }

    fun compartir(context: Context, uri: Uri, titulo: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            putExtra(Intent.EXTRA_TEXT, "Adjunto $titulo generado con Quotely.")
            clipData = ClipData.newRawUri("PDF Quotely", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Compartir $titulo"))
        }
    }

    private fun dibujarCabecera(pintor: PintorPdf, info: DocumentoPdfInfo, primera: Boolean) {
        val canvas = pintor.canvas ?: return
        if (primera) {
            texto(canvas, "QUOTELY", MARGEN, 62f, 26f, TINTA, CONDENSADA)
            texto(canvas, "COTIZACIONES Y PEDIDOS", MARGEN, 76f, 7.5f, ACERO, MONO)
            codigoBarras(canvas, info.folio, MARGEN, 82f, 58f, 7f, ACERO)
            texto(canvas, info.tipo, ANCHO_PAGINA - MARGEN, 48f, 8f, COBALTO, MONO, Paint.Align.RIGHT)
            texto(canvas, info.folio, ANCHO_PAGINA - MARGEN, 65f, 12f, TINTA, MONO, Paint.Align.RIGHT)
            texto(canvas, info.estado.uppercase(), ANCHO_PAGINA - MARGEN, 79f, 8f, info.colorEstado, MONO, Paint.Align.RIGHT)
            canvas.drawRect(MARGEN, 96f, ANCHO_PAGINA - MARGEN, 98.5f, relleno(COBALTO))

            texto(canvas, "CLIENTE", MARGEN, 122f, 7f, ACERO, MONO)
            val altoNombre = parrafo(canvas, info.cliente, MARGEN, 134f, 300f, 13f, TINTA, CONDENSADA, 2)
            var y = 134f + altoNombre
            if (info.contacto.isNotBlank()) {
                texto(canvas, info.contacto, MARGEN, y + 4f, 8f, TINTA_SUAVE, SANS)
                y += 15f
            }
            if (info.notaOrigen != null) {
                texto(canvas, info.notaOrigen, MARGEN, y + 4f, 7.5f, COBALTO, SANS)
            }

            var fechaY = 122f
            info.datosFecha.forEach { (etiqueta, valor) ->
                texto(canvas, etiqueta, ANCHO_PAGINA - MARGEN, fechaY, 7f, ACERO, MONO, Paint.Align.RIGHT)
                texto(canvas, valor, ANCHO_PAGINA - MARGEN, fechaY + 11f, 8.5f, TINTA, MONO, Paint.Align.RIGHT)
                fechaY += 26f
            }
            pintor.y = max(180f, y + 28f)
            lineaPunteada(canvas, MARGEN, pintor.y, ANCHO_PAGINA - MARGEN, LINEA)
            pintor.y += 20f
        } else {
            texto(canvas, "QUOTELY · ${info.folio}", MARGEN, 54f, 9f, TINTA, MONO)
            texto(canvas, "continuación", ANCHO_PAGINA - MARGEN, 54f, 8f, ACERO, SANS, Paint.Align.RIGHT)
            canvas.drawRect(MARGEN, 64f, ANCHO_PAGINA - MARGEN, 65.5f, relleno(LINEA))
            pintor.y = 92f
        }
        dibujarCabeceraTabla(pintor)
    }

    private fun dibujarCabeceraTabla(pintor: PintorPdf) {
        val canvas = pintor.canvas ?: return
        val y = pintor.y
        texto(canvas, "CONCEPTO", MARGEN, y, 7f, ACERO, MONO)
        texto(canvas, "CANT.", 395f, y, 7f, ACERO, MONO, Paint.Align.RIGHT)
        texto(canvas, "P. UNIT.", 482f, y, 7f, ACERO, MONO, Paint.Align.RIGHT)
        texto(canvas, "IMPORTE", ANCHO_PAGINA - MARGEN, y, 7f, ACERO, MONO, Paint.Align.RIGHT)
        canvas.drawRect(MARGEN, y + 6f, ANCHO_PAGINA - MARGEN, y + 7f, relleno(LINEA))
        pintor.y = y + 20f
    }

    private fun dibujarTabla(pintor: PintorPdf, info: DocumentoPdfInfo) {
        info.lineas.forEach { linea ->
            val canvas = pintor.canvas ?: return
            val altoDescripcion = medirParrafo(linea.descripcion, 300f, 9f, SANS_MEDIA, 2)
            val alto = max(44f, altoDescripcion + if (linea.descuento > 0.0) 38f else 30f)
            if (pintor.y + alto > CORTE_TABLA) {
                pintor.nuevaPagina()
                dibujarCabecera(pintor, info, primera = false)
            }
            val actual = pintor.canvas ?: return
            val yBase = pintor.y + 10f
            parrafo(actual, linea.descripcion, MARGEN, yBase, 300f, 9f, TINTA, SANS_MEDIA, 2)
            texto(
                actual,
                "${linea.codigo} · ${linea.unidad}",
                MARGEN,
                yBase + altoDescripcion + 7f,
                7f,
                ACERO,
                MONO
            )
            texto(actual, linea.cantidad.toString(), 395f, yBase + 1f, 8.5f, TINTA, MONO, Paint.Align.RIGHT)
            texto(actual, Formato.moneda(linea.precioUnitario), 482f, yBase + 1f, 8.5f, TINTA, MONO, Paint.Align.RIGHT)
            texto(
                actual,
                Formato.moneda(linea.importe),
                ANCHO_PAGINA - MARGEN,
                yBase + 1f,
                8.5f,
                TINTA,
                MONO,
                Paint.Align.RIGHT
            )
            if (linea.descuento > 0.0) {
                texto(
                    actual,
                    "Desc. volumen ${Formato.porcentaje(linea.descuentoPct)}",
                    MARGEN,
                    yBase + altoDescripcion + 18f,
                    7f,
                    SENAL,
                    SANS
                )
                texto(
                    actual,
                    "-${Formato.moneda(linea.descuento)}",
                    ANCHO_PAGINA - MARGEN,
                    yBase + altoDescripcion + 18f,
                    7f,
                    SENAL,
                    MONO,
                    Paint.Align.RIGHT
                )
            }
            pintor.y += alto
            actual.drawRect(MARGEN, pintor.y, ANCHO_PAGINA - MARGEN, pintor.y + 0.6f, relleno(LINEA))
        }
    }

    private fun dibujarTotales(pintor: PintorPdf, info: DocumentoPdfInfo) {
        if (pintor.y + 130f > ALTO_PAGINA - 100f) {
            pintor.nuevaPagina()
            dibujarCabecera(pintor, info, primera = false)
        }
        val canvas = pintor.canvas ?: return
        pintor.y += 14f
        lineaPunteada(canvas, 300f, pintor.y, ANCHO_PAGINA - MARGEN, LINEA)
        pintor.y += 18f

        filaTotal(canvas, pintor, "Subtotal", info.subtotal, false)
        if (info.descuentoVolumen > 0.0) {
            filaTotal(canvas, pintor, "Descuento por volumen", -info.descuentoVolumen, false)
        }
        if (info.descuentoGlobal > 0.0) {
            filaTotal(canvas, pintor, info.etiquetaDescuentoGlobal ?: "Descuento global", -info.descuentoGlobal, false)
        }
        filaTotal(canvas, pintor, "IVA 16%", info.iva, false)
        pintor.y += 3f
        canvas.drawRect(390f, pintor.y, ANCHO_PAGINA - MARGEN, pintor.y + 0.8f, relleno(TINTA))
        pintor.y += 20f
        texto(canvas, "TOTAL", 390f, pintor.y, 11f, TINTA, CONDENSADA)
        texto(
            canvas,
            Formato.moneda(info.total),
            ANCHO_PAGINA - MARGEN,
            pintor.y,
            14f,
            TINTA,
            MONO,
            Paint.Align.RIGHT
        )
        pintor.y += 24f
    }

    private fun filaTotal(canvas: Canvas, pintor: PintorPdf, etiqueta: String, monto: Double, negativo: Boolean) {
        texto(canvas, etiqueta, 390f, pintor.y, 8.5f, TINTA_SUAVE, SANS)
        texto(
            canvas,
            Formato.moneda(monto),
            ANCHO_PAGINA - MARGEN,
            pintor.y,
            8.5f,
            if (negativo) SENAL else TINTA,
            MONO,
            Paint.Align.RIGHT
        )
        pintor.y += 16f
    }

    private fun dibujarNotas(pintor: PintorPdf, info: DocumentoPdfInfo) {
        if (info.notas.isBlank()) return
        val alto = medirParrafo(info.notas, ANCHO_PAGINA - MARGEN * 2, 8f, SANS, 6)
        if (pintor.y + alto + 34f > ALTO_PAGINA - 110f) {
            pintor.nuevaPagina()
            dibujarCabecera(pintor, info, primera = false)
        }
        val canvas = pintor.canvas ?: return
        pintor.y += 6f
        texto(canvas, "NOTAS", MARGEN, pintor.y, 7f, ACERO, MONO)
        pintor.y += 11f
        parrafo(canvas, info.notas, MARGEN, pintor.y, ANCHO_PAGINA - MARGEN * 2, 8f, TINTA_SUAVE, SANS, 6)
        pintor.y += alto + 8f
        texto(
            canvas,
            "Registrado por ${info.responsable}",
            MARGEN,
            pintor.y,
            7.5f,
            ACERO,
            SANS
        )
    }

    private class PintorPdf(private val documento: PdfDocument) {
        var pagina: PdfDocument.Page? = null
            private set
        var canvas: Canvas? = null
            private set
        var y: Float = 0f
        private var numero = 0

        fun nuevaPagina() {
            cerrarPagina()
            numero += 1
            val page = documento.startPage(
                PdfDocument.PageInfo.Builder(ANCHO_PAGINA, ALTO_PAGINA, numero).create()
            )
            pagina = page
            canvas = page.canvas
            page.canvas.drawRect(
                0f,
                0f,
                ANCHO_PAGINA.toFloat(),
                ALTO_PAGINA.toFloat(),
                relleno(PAPEL)
            )
            y = MARGEN
        }

        fun cerrarPagina() {
            val page = pagina ?: return
            val c = page.canvas
            val ahora = LocalDate.now().format(
                DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "MX"))
            )
            c.drawRect(MARGEN, 786f, ANCHO_PAGINA - MARGEN, 787f, relleno(LINEA))
            texto(c, "QUOTELY · documento generado el $ahora", MARGEN, 800f, 7f, ACERO, SANS)
            texto(c, "Página $numero", ANCHO_PAGINA - MARGEN, 800f, 7f, ACERO, MONO, Paint.Align.RIGHT)
            documento.finishPage(page)
            pagina = null
            canvas = null
        }
    }

    private fun texto(
        canvas: Canvas,
        valor: String,
        x: Float,
        y: Float,
        tamano: Float,
        color: Int,
        typeface: Typeface,
        align: Paint.Align = Paint.Align.LEFT
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = tamano
            this.typeface = typeface
            textAlign = align
        }
        canvas.drawText(valor, x, y, paint)
    }

    private fun parrafo(
        canvas: Canvas,
        valor: String,
        x: Float,
        y: Float,
        ancho: Float,
        tamano: Float,
        color: Int,
        typeface: Typeface,
        maxLineas: Int
    ): Float {
        if (valor.isBlank()) return 0f
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = tamano
            this.typeface = typeface
        }
        val layout = StaticLayout.Builder.obtain(valor, 0, valor.length, paint, ancho.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(maxLineas)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(1.8f, 1f)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    private fun medirParrafo(
        valor: String,
        ancho: Float,
        tamano: Float,
        typeface: Typeface,
        maxLineas: Int
    ): Float {
        if (valor.isBlank()) return 0f
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = tamano
            this.typeface = typeface
        }
        return StaticLayout.Builder.obtain(valor, 0, valor.length, paint, ancho.toInt())
            .setMaxLines(maxLineas)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(1.8f, 1f)
            .build().height.toFloat()
    }

    private fun relleno(color: Int): Paint = Paint().apply {
        this.color = color
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private fun lineaPunteada(canvas: Canvas, x1: Float, y: Float, x2: Float, color: Int) {
        val paint = Paint().apply {
            this.color = color
            strokeWidth = 1f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 4f), 0f)
        }
        canvas.drawLine(x1, y, x2, y, paint)
    }

    private fun codigoBarras(
        canvas: Canvas,
        semilla: String,
        x: Float,
        y: Float,
        ancho: Float,
        alto: Float,
        color: Int
    ) {
        val random = Random(semilla.hashCode())
        val paint = relleno(color)
        var actual = x
        while (actual < x + ancho) {
            val barra = 0.8f + random.nextFloat() * 2.2f
            val espacio = 0.8f + random.nextFloat() * 2.4f
            canvas.drawRect(actual, y, actual + barra, y + alto, paint)
            actual += barra + espacio
        }
    }
}
