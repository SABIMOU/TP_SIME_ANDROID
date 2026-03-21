package fr.mastersid.etudiant.template.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = QuestionEntity.TABLE_NAME)
data class QuestionEntity(

    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "answer_count")
    val answerCount: Int,

    @ColumnInfo(name = "last_activity_date")
    val lastActivityDate: Long,

    @ColumnInfo(name = "body")
    val body: String

) {
    companion object {
        const val TABLE_NAME = "question_table"
    }
}