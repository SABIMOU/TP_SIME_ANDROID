package fr.mastersid.etudiant.template.data.repository

import fr.mastersid.etudiant.template.data.local.dao.AppDao
import fr.mastersid.etudiant.template.data.mapper.toDomain
import fr.mastersid.etudiant.template.data.mapper.toEntity
import fr.mastersid.etudiant.template.data.remote.api.StackOverflowApiService
import fr.mastersid.etudiant.template.domain.repository.QuestionsRepository
import fr.mastersid.etudiant.template.domain.repository.QuestionsResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class RemoteQuestionsRepository @Inject constructor(
    private val apiService: StackOverflowApiService,
    private val dao       : AppDao
) : QuestionsRepository {

    private val _requestState = MutableStateFlow<QuestionsResponse?>(null)

    override val questionsResponse: Flow<QuestionsResponse> =
        combine(dao.getQuestionsFlow(), _requestState) { entities, state ->
            when (state) {
                is QuestionsResponse.Pending -> QuestionsResponse.Pending
                is QuestionsResponse.Error   -> state
                else                         -> QuestionsResponse.Idle(entities.toDomain())
            }
        }

    override suspend fun updateQuestionsInfo() {
        _requestState.value = QuestionsResponse.Pending
        try {
            val questions = apiService
                .getActiveQuestions()
                .items
                .toDomain()
            dao.insertAll(questions.toEntity())
            _requestState.value = null
        } catch (e: IOException)   { _requestState.value = QuestionsResponse.Error("Erreur réseau") }
        catch (e: HttpException)  { _requestState.value = QuestionsResponse.Error("Erreur serveur") }
    }
}