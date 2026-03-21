package fr.mastersid.etudiant.template.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import fr.mastersid.etudiant.template.data.repository.RemoteQuestionsRepository
import fr.mastersid.etudiant.template.domain.repository.QuestionsRepository

@Module
@InstallIn(ViewModelComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindQuestionsRepository(
        impl: RemoteQuestionsRepository
    ): QuestionsRepository
}