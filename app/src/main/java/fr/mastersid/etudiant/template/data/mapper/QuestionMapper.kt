package fr.mastersid.etudiant.template.data.mapper

import fr.mastersid.etudiant.template.data.local.entity.QuestionEntity
import fr.mastersid.etudiant.template.data.remote.dto.QuestionDto
import fr.mastersid.etudiant.template.domain.model.Question
import org.jsoup.Jsoup

// ── DTO → Domain ──────────────────────────────────────────────────────────
fun QuestionDto.toDomain(): Question = Question(
    id               = questionId,
    title            = Jsoup.parse(title).text(),
    answerCount      = answerCount,
    lastActivityDate = lastActivityDate,
    body             = body?.let { Jsoup.parse(it).text() } ?: ""
)

@JvmName("dtoListToDomain")
fun List<QuestionDto>.toDomain(): List<Question> = map { it.toDomain() }

// ── Domain → Entity ───────────────────────────────────────────────────────
fun Question.toEntity(): QuestionEntity = QuestionEntity(
    id               = id,
    title            = title,
    answerCount      = answerCount,
    lastActivityDate = lastActivityDate,
    body             = body
)

fun List<Question>.toEntity(): List<QuestionEntity> = map { it.toEntity() }

// ── Entity → Domain ───────────────────────────────────────────────────────
fun QuestionEntity.toDomain(): Question = Question(
    id               = id,
    title            = title,
    answerCount      = answerCount,
    lastActivityDate = lastActivityDate,
    body             = body
)

@JvmName("entityListToDomain")
fun List<QuestionEntity>.toDomain(): List<Question> = map { it.toDomain() }