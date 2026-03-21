package fr.mastersid.etudiant.template.presentation.items

import fr.mastersid.etudiant.template.domain.model.Question

data class QuestionsUiState(
    val questions      : List<Question> = emptyList(),
    val isUpdating     : Boolean        = false,
    val onlyNotAnswered: Boolean        = false,
    val errorMessage   : String?        = null
) {
    val displayedQuestions: List<Question>
        get() = if (onlyNotAnswered) questions.filter { it.answerCount == 0 } else questions
}