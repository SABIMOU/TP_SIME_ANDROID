package fr.mastersid.etudiant.template.presentation.items

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.mastersid.etudiant.template.domain.repository.QuestionsRepository
import fr.mastersid.etudiant.template.domain.repository.QuestionsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuestionsViewModel @Inject constructor(
    private val repository: QuestionsRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(QuestionsUiState())
    val uiState: LiveData<QuestionsUiState> = _uiState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.questionsResponse.collect { response ->
                val current = _uiState.value ?: QuestionsUiState()
                _uiState.postValue(
                    when (response) {
                        is QuestionsResponse.Idle ->
                            current.copy(
                                isUpdating   = false,
                                questions    = response.questions,
                                errorMessage = null
                            )
                        is QuestionsResponse.Pending ->
                            current.copy(
                                isUpdating   = true,
                                errorMessage = null
                            )
                        is QuestionsResponse.Error ->
                            current.copy(
                                isUpdating   = false,
                                errorMessage = response.message
                            )
                    }
                )
            }
        }
    }

    fun updateQuestions() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateQuestionsInfo()
        }
    }

    fun toggleFilter(onlyNotAnswered: Boolean) {
        _uiState.postValue(_uiState.value?.copy(onlyNotAnswered = onlyNotAnswered))
    }

    fun dismissError() {
        _uiState.postValue(_uiState.value?.copy(errorMessage = null))
    }
}