package com.quotely.app

import android.app.Application
import android.content.Context
import com.quotely.app.data.local.QuotelyDatabase
import com.quotely.app.data.repository.AuthRepository
import com.quotely.app.data.repository.CotizacionRepository
import com.quotely.app.data.repository.PedidoRepository
import com.quotely.app.data.repository.ProductoRepository
import com.quotely.app.data.seed.DatabaseSeeder
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: QuotelyDatabase = QuotelyDatabase.obtener(appContext)
    val authRepository = AuthRepository(appContext, database.responsableDao())
    val productoRepository = ProductoRepository(database.productoDao())
    val cotizacionRepository = CotizacionRepository(database)
    val pedidoRepository = PedidoRepository(database)
    private val seeder = DatabaseSeeder(database)

    private val _inicializada = MutableStateFlow(false)
    val inicializada: StateFlow<Boolean> = _inicializada.asStateFlow()

    suspend fun seedIfNeeded() {
        try {
            seeder.seedIfNeeded()
        } finally {
            _inicializada.value = true
        }
    }
}

class QuotelyApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Locale.setDefault(Locale("es", "MX"))
        container = AppContainer(this)
        appScope.launch {
            runCatching { container.seedIfNeeded() }
        }
    }
}
