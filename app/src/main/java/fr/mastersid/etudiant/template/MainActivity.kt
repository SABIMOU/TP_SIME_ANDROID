package fr.mastersid.etudiant.template

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import fr.mastersid.etudiant.template.presentation.items.QuestionsScreen
import fr.mastersid.etudiant.template.presentation.theme.AppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                QuestionsScreen()
            }
        }
    }
}