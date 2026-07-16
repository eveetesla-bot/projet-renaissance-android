package fr.projetrenaissance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.projetrenaissance.presentation.AppViewModel
import fr.projetrenaissance.presentation.RenaissanceApp
import fr.projetrenaissance.presentation.RenaissanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RenaissanceTheme {
                val application = application as RenaissanceApplication
                val appViewModel: AppViewModel = viewModel(factory = AppViewModel.factory(application.container))
                RenaissanceApp(appViewModel)
            }
        }
    }
}

