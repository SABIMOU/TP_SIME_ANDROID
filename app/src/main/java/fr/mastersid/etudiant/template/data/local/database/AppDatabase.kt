package fr.mastersid.etudiant.template.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import fr.mastersid.etudiant.template.data.local.dao.AppDao
import fr.mastersid.etudiant.template.data.local.entity.QuestionEntity

@Database(
    entities     = [QuestionEntity::class],
    version      = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        const val DATABASE_NAME = "stack_database"
    }
}