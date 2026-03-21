package fr.mastersid.etudiant.template.domain.repository

import fr.mastersid.etudiant.template.domain.model.Question

sealed interface QuestionsResponse {
    data class Idle(val questions: List<Question>) : QuestionsResponse
    data object Pending : QuestionsResponse
    data class Error(val message: String) : QuestionsResponse
}