package com.mpdc4gsr.module.user.activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.app.ktbase.BaseActivity
import com.mpdc4gsr.libunified.app.navigation.NavigationManager
import com.mpdc4gsr.libunified.app.utils.Constants
import com.mpdc4gsr.libunified.app.view.TitleView
import com.mpdc4gsr.module.user.R
import com.mpdc4gsr.libunified.app.R as RCore

class ElectronicManualActivity : BaseActivity() {

    private lateinit var titleView: TitleView
    private lateinit var electronicManualRecycler: RecyclerView

    override fun initContentView() = R.layout.activity_electronic_manual

    override fun initView() {

        titleView = findViewById(R.id.title_view)
        electronicManualRecycler = findViewById(R.id.electronic_manual_recycler)

        val productType = intent.getIntExtra(Constants.SETTING_TYPE, 0)

        titleView.setTitleText(if (productType == Constants.SETTING_BOOK) RCore.string.electronic_manual else RCore.string.app_question)

        val adapter = MyAdapter(productType == 1)
        adapter.onPickListener = { isTS001 ->
            if (isTS001) {
                if (productType == Constants.SETTING_BOOK) {

                } else {

                    NavigationManager.getInstance().build(RouterConfig.QUESTION)
                        .withBoolean("isTS001", true).navigation(this)
                }
            } else {
                if (productType == Constants.SETTING_BOOK) {

                    NavigationManager.getInstance().build(RouterConfig.PDF)
                        .withBoolean("isTS001", false).navigation(this)
                } else {

                    NavigationManager.getInstance().build(RouterConfig.QUESTION)
                        .withBoolean("isTS001", false).navigation(this)
                }
            }
        }

        electronicManualRecycler.layoutManager = LinearLayoutManager(this)
        electronicManualRecycler.adapter = adapter
    }

    override fun initData() {
    }

    private class MyAdapter(private val isFAQ: Boolean) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var onPickListener: ((isTS001: Boolean) -> Unit)? = null

        private val optionList: ArrayList<String> = ArrayList(2)

        init {

            if (isFAQ) {
                optionList.add("TS001")
            }
            optionList.add("TS004")
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder {
            return ItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_electronic_manual, parent, false)
            )
        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) {
            if (holder is ItemViewHolder) {
                val itemText: TextView = holder.rootView.findViewById(R.id.item_text)
                val itemLay: ConstraintLayout = holder.rootView.findViewById(R.id.item_lay)

                itemText.text = optionList[position]
                itemLay.setOnClickListener {
                    onPickListener?.invoke(isFAQ && position == 0)
                }
            }
        }

        override fun getItemCount(): Int = optionList.size

        private class ItemViewHolder(val rootView: View) : RecyclerView.ViewHolder(rootView)
    }
}
