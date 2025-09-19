package com.mpdc4gsr.module.user.activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.lib.core.config.RouterConfig
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import com.mpdc4gsr.lib.core.navigation.NavigationManager
import com.mpdc4gsr.module.user.R
import com.mpdc4gsr.module.user.model.FaqRepository
import com.mpdc4gsr.module.user.model.QuestionData

class QuestionActivity : BaseActivity() {

    private lateinit var questionRecycler: RecyclerView

    override fun initContentView() = R.layout.activity_question

    override fun initView() {

        questionRecycler = findViewById(R.id.question_recycler)

        val adapter =
            MyAdapter(FaqRepository.getQuestionList(intent.getBooleanExtra("isTS001", false)))
        adapter.onItemClickListener = {
            NavigationManager.getInstance()
                .build(RouterConfig.QUESTION_DETAILS)
                .withString("question", it.question)
                .withString("answer", it.answer)
                .navigation(this)
        }

        questionRecycler.layoutManager = LinearLayoutManager(this)
        questionRecycler.adapter = adapter
    }

    override fun initData() {
    }

    private class MyAdapter(private val questionList: ArrayList<QuestionData>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var onItemClickListener: ((data: QuestionData) -> Unit)? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder {
            return ItemHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_question, parent, false)
            )
        }

        override fun getItemCount(): Int = questionList.size

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) {
            if (holder is ItemHolder) {
                val itemQuestionInfo: TextView =
                    holder.rootView.findViewById(R.id.item_question_info)
                val itemQuestionLay: ConstraintLayout =
                    holder.rootView.findViewById(R.id.item_question_lay)

                itemQuestionInfo.text = questionList[position].question
                itemQuestionLay.setOnClickListener {
                    onItemClickListener?.invoke(questionList[position])
                }
            }
        }

        private class ItemHolder(val rootView: View) : RecyclerView.ViewHolder(rootView)
    }
}
