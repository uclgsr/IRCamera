package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.module.user.model.FaqRepository
import com.mpdc4gsr.module.user.model.QuestionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QuestionViewModel : BaseViewModel() {
    private val _questions = MutableStateFlow<List<QuestionData>>(emptyList())
    val questions: StateFlow<List<QuestionData>> = _questions.asStateFlow()

    fun loadQuestions(isTS001: Boolean) {
        launchWithErrorHandling {
            val questionList = FaqRepository.getQuestionList(isTS001)
            _questions.value = questionList
        }
    }
}
