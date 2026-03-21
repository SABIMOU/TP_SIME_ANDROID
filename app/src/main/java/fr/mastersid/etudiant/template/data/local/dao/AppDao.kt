package fr.mastersid.etudiant.template.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.mastersid.etudiant.template.data.local.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("SELECT * FROM ${QuestionEntity.TABLE_NAME} ORDER BY last_activity_date DESC")
    fun getQuestionsFlow(): Flow<List<QuestionEntity>>
}