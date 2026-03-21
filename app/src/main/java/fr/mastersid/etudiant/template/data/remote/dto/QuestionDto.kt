package fr.mastersid.etudiant.template.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuestionDto(
    @Json(name = "question_id")        val questionId: Int,
    @Json(name = "title")              val title: String,
    @Json(name = "answer_count")       val answerCount: Int,
    @Json(name = "last_activity_date") val lastActivityDate: Long = 0L,
    @Json(name = "body")               val body: String? = null
)

@JsonClass(generateAdapter = true)
data class QuestionsEnvelopeDto(
    @Json(name = "items") val items: List<QuestionDto>
)