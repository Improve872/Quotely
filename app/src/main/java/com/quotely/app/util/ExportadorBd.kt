package com.quotely.app.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.quotely.app.data.local.QuotelyDatabase
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExportadorBd {
    suspend fun exportar(context: Context, db: QuotelyDatabase): Uri = withContext(Dispatchers.IO) {
        db.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(FULL)")
            .use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0)
            }
        val origen = context.getDatabasePath("quotely.db")
        require(origen.exists()) { "La base de datos todavía no existe." }
        val marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val destino = File(context.cacheDir, "quotely_$marca.db")
        origen.copyTo(destino, overwrite = true)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            destino
        )
    }

    fun compartir(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/x-sqlite3"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Respaldo de base de datos Quotely")
            putExtra(Intent.EXTRA_TEXT, "Base SQLite de Quotely. Ábrela con DB Browser for SQLite o SQLiteStudio.")
            clipData = ClipData.newRawUri("Base de datos Quotely", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Compartir base de datos"))
        }
    }
}
