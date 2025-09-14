package com.topdon.module.user.activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.navigation.NavigationManager
import com.topdon.module.user.R
import com.topdon.module.user.model.FaqRepository
import com.topdon.module.user.model.QuestionData
import java.util.ArrayList


// Legacy ARouter route annotation - now using NavigationManager
class QuestionActivity : BaseActivity() {
    // View references - migrated from synthetic views
    private lateinit var questionRecycler: RecyclerView

    override fun initContentView() = R.layout.activity_question

    override fun initView() {
    // Initialize views - migrated from synthetic views
    questionRecycler = findViewById(R.id.question_recycler)

    val adapter = MyAdapter(FaqRepository.getQuestionList(intent.getBooleanExtra("isTS001", false)))
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

    private class MyAdapter(private val questionList: ArrayList<QuestionData>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var onItemClickListener: ((data: QuestionData) -> Unit)? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder {
            return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_question, parent, false))
        }

    override fun getItemCount(): Int = questionList.size

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) {
            if (holder is ItemHolder) {
                val itemQuestionInfo: TextView = holder.rootView.findViewById(R.id.item_question_info)
                val itemQuestionLay: ConstraintLayout = holder.rootView.findViewById(R.id.item_question_lay)

                itemQuestionInfo.text = questionList[position].question
                itemQuestionLay.setOnClickListener {
                    onItemClickListener?.invoke(questionList[position])
                }
            }
        }

    itemQuestionInfo.text = questionList[position].question
    itemQuestionLay.setOnClickListener {
    onItemClickListener?.invoke(questionList[position])
    }
    }
    }

    private class ItemHolder(val rootView: View) : RecyclerView.ViewHolder(rootView)
    }
}
