package fr.mastersid.etudiant.template.domain.repository

import kotlinx.coroutines.flow.Flow

interface QuestionsRepository {
    val questionsResponse: Flow<QuestionsResponse>
    suspend fun updateQuestionsInfo()
}