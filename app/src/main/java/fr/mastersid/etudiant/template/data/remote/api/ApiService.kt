package fr.mastersid.etudiant.template.data.remote.api

import fr.mastersid.etudiant.template.data.remote.dto.QuestionsEnvelopeDto
import retrofit2.http.GET

interface StackOverflowApiService {

    @GET("questions?pagesize=20&order=desc&sort=activity&site=stackoverflow&filter=withbody")
    suspend fun getActiveQuestions(): QuestionsEnvelopeDto
}