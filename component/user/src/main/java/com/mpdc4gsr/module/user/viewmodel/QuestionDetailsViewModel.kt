package com.mpdc4gsr.module.user.viewmodel
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
class QuestionDetailsViewModel : BaseViewModel() {
    private val _question = MutableStateFlow("")
    val question: StateFlow<String> = _question.asStateFlow()
    private val _answer = MutableStateFlow("")
    val answer: StateFlow<String> = _answer.asStateFlow()
    fun loadQuestionDetails(question: String?, answer: String?) {
        _question.value = question ?: ""
        _answer.value = answer ?: ""
    }
}