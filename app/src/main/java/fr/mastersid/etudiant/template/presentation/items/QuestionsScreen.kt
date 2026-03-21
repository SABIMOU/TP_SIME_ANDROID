package fr.mastersid.etudiant.template.presentation.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.mastersid.etudiant.template.R
import fr.mastersid.etudiant.template.domain.model.Question
import fr.mastersid.etudiant.template.presentation.theme.AppTheme

// ── Stateful ──────────────────────────────────────────────────────────────
@Composable
fun QuestionsScreen(viewModel: QuestionsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.observeAsState(QuestionsUiState())
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    QuestionsContent(
        uiState           = uiState,
        snackbarHostState = snackbarHostState,
        onUpdateClick     = viewModel::updateQuestions,
        onFilterChange    = viewModel::toggleFilter
    )
}

// ── Stateless ─────────────────────────────────────────────────────────────
@Composable
fun QuestionsContent(
    uiState           : QuestionsUiState,
    snackbarHostState : SnackbarHostState = remember { SnackbarHostState() },
    onUpdateClick     : () -> Unit = {},
    onFilterChange    : (Boolean) -> Unit = {}
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onUpdateClick) {
                Icon(
                    painter            = painterResource(R.drawable.ic_download),
                    contentDescription = "Mettre à jour"
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    action = {
                        TextButton(onClick = { data.dismiss() }) { Text("OK") }
                    }
                ) { Text(data.visuals.message) }
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(items = uiState.displayedQuestions, key = { it.id }) { question ->
                        QuestionItem(question = question)
                        HorizontalDivider()
                    }
                }

                HorizontalDivider()
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked         = uiState.onlyNotAnswered,
                        onCheckedChange = onFilterChange
                    )
                    Text(text = "Uniquement sans réponse", fontSize = 16.sp)
                }
            }

            if (uiState.isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

// ── QuestionItem ──────────────────────────────────────────────────────────
@Composable
fun QuestionItem(question: Question) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = question.title,
                style    = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (question.body.isNotEmpty()) {
                Text(
                    text     = question.body,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Text(
            text     = question.answerCount.toString(),
            style    = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────
@Preview(showBackground = true)
@Composable
private fun QuestionItemPreview() {
    AppTheme {
        QuestionItem(
            question = Question(
                id          = 1,
                title       = "Pourquoi Kotlin ne compile pas ?",
                answerCount = 0,
                body        = "Voici mon problème détaillé..."
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun QuestionsContentPreview() {
    AppTheme {
        QuestionsContent(
            uiState = QuestionsUiState(
                questions = listOf(
                    Question(1, "Question Kotlin", 3),
                    Question(2, "Question sans réponse", 0),
                    Question(3, "Question courte", 7)
                )
            )
        )
    }
}