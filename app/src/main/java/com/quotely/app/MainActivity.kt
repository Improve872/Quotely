package com.quotely.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotely.app.ui.navigation.AppNavigation
import com.quotely.app.ui.screens.LoginScreen
import com.quotely.app.ui.screens.PantallaCarga
import com.quotely.app.ui.theme.QuotelyTheme
import com.quotely.app.ui.viewmodel.EstadoSesion
import com.quotely.app.ui.viewmodel.authViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val locale = Locale("es", "MX")
        Locale.setDefault(locale)
        val configuracion = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuracion))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as QuotelyApp).container
        setContent {
            QuotelyTheme {
                QuotelyRoot(container)
            }
        }
    }
}

@Composable
fun QuotelyRoot(container: AppContainer) {
    val vm = authViewModel(container)
    val estado by vm.estado.collectAsStateWithLifecycle()
    val inicializada by container.inicializada.collectAsStateWithLifecycle()

    if (!inicializada) {
        PantallaCarga()
        return
    }

    when (val sesion = estado) {
        EstadoSesion.Cargando -> PantallaCarga()
        EstadoSesion.SinSesion -> LoginScreen(vm)
        is EstadoSesion.Autenticado -> AppNavigation(
            container = container,
            usuario = sesion.usuario,
            onCerrarSesion = { vm.cerrarSesion() }
        )
    }
}
