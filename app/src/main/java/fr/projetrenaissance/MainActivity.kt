package fr.projetrenaissance

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.projetrenaissance.presentation.AppViewModel
import fr.projetrenaissance.presentation.RenaissanceApp
import fr.projetrenaissance.presentation.RenaissanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // L'interface est en thème clair : on force des barres système claires
        // (icônes sombres), sinon sur un téléphone en mode sombre l'heure et les
        // icônes d'état restent blanches et deviennent illisibles sur le fond clair.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            RenaissanceTheme {
                val application = application as RenaissanceApplication
                val appViewModel: AppViewModel = viewModel(factory = AppViewModel.factory(application.container))
                RenaissanceApp(appViewModel)
            }
        }
    }
}

