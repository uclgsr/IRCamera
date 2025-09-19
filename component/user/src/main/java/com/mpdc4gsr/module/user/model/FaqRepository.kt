package com.mpdc4gsr.module.user.model

import com.blankj.utilcode.util.Utils
import com.mpdc4gsr.lib.core.R as RCore

object FaqRepository {
    fun getQuestionList(isTS001: Boolean): ArrayList<QuestionData> =
        if (isTS001) {
            arrayListOf(
                QuestionData(
                    question = Utils.getApp().getString(RCore.string.question1),
                    answer = Utils.getApp().getString(RCore.string.answer1),
                ),
                QuestionData(
                    question = Utils.getApp().getString(RCore.string.question2),
                    answer = Utils.getApp().getString(RCore.string.answer2),
                ),
                QuestionData(
                    question = Utils.getApp().getString(RCore.string.question3),
                    answer = Utils.getApp().getString(RCore.string.answer3),
                ),
                QuestionData(
                    question = Utils.getApp().getString(RCore.string.question4),
                    answer = Utils.getApp().getString(RCore.string.answer4),
                ),
                QuestionData(
                    question = Utils.getApp().getString(RCore.string.question5),
                    answer = Utils.getApp().getString(RCore.string.answer5),
                ),
                QuestionData(
                    question = Utils.getApp().getString(RCore.string.question6),
                    answer = Utils.getApp().getString(RCore.string.answer6),
                ),
                QuestionData(
                    question = Utils.getApp().getString(RCore.string.question7),
                    answer = Utils.getApp().getString(RCore.string.answer7),
                ),
                QuestionData(
                    question = Utils.getApp().getString(RCore.string.question8),
                    answer = Utils.getApp().getString(RCore.string.answer8),
                ),
            )
        } else {
            arrayListOf(
                QuestionData(
                    question = Utils.getApp().getString(RCore.string.ts004_faq_q1),
                    answer = Utils.getApp().getString(RCore.string.ts004_faq_a1),
                ),
                QuestionData(
                    question = Utils.getApp().getString(RCore.string.ts004_faq_q2),
                    answer = Utils.getApp().getString(RCore.string.ts004_faq_a2),
                ),
                QuestionData(
                    question = Utils.getApp().getString(RCore.string.ts004_faq_q3),
                    answer = Utils.getApp().getString(RCore.string.ts004_faq_a3),
                ),
                QuestionData(
                    question = Utils.getApp().getString(RCore.string.ts004_faq_q4),
                    answer = Utils.getApp().getString(RCore.string.ts004_faq_a4),
                ),
                QuestionData(
                    question = Utils.getApp().getString(RCore.string.ts004_faq_q5),
                    answer = Utils.getApp().getString(RCore.string.ts004_faq_a5),
                ),
            )
        }
}

data class QuestionData(
    val question: String,
    val answer: String,
)
