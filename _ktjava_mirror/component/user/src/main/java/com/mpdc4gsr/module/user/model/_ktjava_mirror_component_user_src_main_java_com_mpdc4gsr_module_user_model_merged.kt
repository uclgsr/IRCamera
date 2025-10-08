// Merged ALL .kt and .java files from the '_ktjava_mirror\component\user\src\main\java\com\mpdc4gsr\module\user\model' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:43


// ===== FROM: _ktjava_mirror\component\user\src\main\java\com\mpdc4gsr\module\user\model\component_user_src_main_java_com_mpdc4gsr_module_user_model_all.kt =====

// Merged .kt under 'component\user\src\main\java\com\mpdc4gsr\module\user\model' subtree
// Files: 1; Generated 2025-10-07 23:07:45


// ===== component\user\src\main\java\com\mpdc4gsr\module\user\model\FaqRepository.kt =====

package com.mpdc4gsr.module.user.model

import com.mpdc4gsr.libunified.compat.ContextProvider
import com.mpdc4gsr.libunified.R as RCore

object FaqRepository {
    fun getQuestionList(isTS001: Boolean): ArrayList<QuestionData> =
        if (isTS001) {
            arrayListOf(
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question1),
                    answer = ContextProvider.getContext().getString(RCore.string.answer1),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question2),
                    answer = ContextProvider.getContext().getString(RCore.string.answer2),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question3),
                    answer = ContextProvider.getContext().getString(RCore.string.answer3),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question4),
                    answer = ContextProvider.getContext().getString(RCore.string.answer4),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question5),
                    answer = ContextProvider.getContext().getString(RCore.string.answer5),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question6),
                    answer = ContextProvider.getContext().getString(RCore.string.answer6),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question7),
                    answer = ContextProvider.getContext().getString(RCore.string.answer7),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.question8),
                    answer = ContextProvider.getContext().getString(RCore.string.answer8),
                ),
            )
        } else {
            arrayListOf(
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.ts004_faq_q1),
                    answer = ContextProvider.getContext().getString(RCore.string.ts004_faq_a1),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.ts004_faq_q2),
                    answer = ContextProvider.getContext().getString(RCore.string.ts004_faq_a2),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.ts004_faq_q3),
                    answer = ContextProvider.getContext().getString(RCore.string.ts004_faq_a3),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.ts004_faq_q4),
                    answer = ContextProvider.getContext().getString(RCore.string.ts004_faq_a4),
                ),
                QuestionData(
                    question = ContextProvider.getContext().getString(RCore.string.ts004_faq_q5),
                    answer = ContextProvider.getContext().getString(RCore.string.ts004_faq_a5),
                ),
            )
        }
}

data class QuestionData(
    val question: String,
    val answer: String,
)