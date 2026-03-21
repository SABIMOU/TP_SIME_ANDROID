package fr.mastersid.etudiant.template.domain.model

data class Question(
    val id: Int,
    val title: String,
    val answerCount: Int,
    val lastActivityDate: Long = 0L,
    val body: String = ""
)
