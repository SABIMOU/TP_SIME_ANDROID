package fr.mastersid.etudiant.template.data.repository

import fr.mastersid.etudiant.template.domain.model.Question
import fr.mastersid.etudiant.template.domain.repository.QuestionsRepository
import fr.mastersid.etudiant.template.domain.repository.QuestionsResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

class FakeQuestionsRepository @Inject constructor() : QuestionsRepository {

    private val _flow = MutableSharedFlow<QuestionsResponse>(replay = 1)
    override val questionsResponse: Flow<QuestionsResponse> = _flow

    override suspend fun updateQuestionsInfo() {
        _flow.emit(QuestionsResponse.Pending)
        delay(5_000)
        _flow.emit(QuestionsResponse.Idle(buildFakeQuestions()))
    }

    private fun buildFakeQuestions(): List<Question> = listOf(
        Question(id = 1, title = "Kotlin ne fonctionne pas",       answerCount = (0..20).random()),
        Question(id = 2, title = "Question sans réponse",          answerCount = 0),
        Question(id = 3, title = "Question courte",                answerCount = (0..20).random()),
        Question(id = 4, title = "Titre très très long à tronquer", answerCount = (0..10).random()),
        Question(id = 5, title = "Autre question sans réponse",    answerCount = 0)
    )
}