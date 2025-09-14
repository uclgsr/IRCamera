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
import com.topdon.lib.core.utils.Constants
import com.topdon.lib.core.view.TitleView
import com.topdon.module.user.R
import com.topdon.lib.core.R as RCore


// Legacy ARouter route annotation - now using NavigationManager
class ElectronicManualActivity : BaseActivity() {
    // View references - migrated from synthetic views
    private lateinit var titleView: TitleView
    private lateinit var electronicManualRecycler: RecyclerView

    override fun initContentView() = R.layout.activity_electronic_manual

    override fun initView() {
        // Initialize views - migrated from synthetic views
        titleView = findViewById(R.id.title_view)
        electronicManualRecycler = findViewById(R.id.electronic_manual_recycler)

        val productType = intent.getIntExtra(Constants.SETTING_TYPE, 0) // 0-电子说明书 1-FAQ

    val productType = intent.getIntExtra(Constants.SETTING_TYPE, 0) //0-电子说明书 1-FAQ

        val adapter = MyAdapter(productType == 1)
        adapter.onPickListener = { isTS001 ->
            if (isTS001) {
                if (productType == Constants.SETTING_BOOK) {
//电子说明书-TS001
                } else {
                    // FAQ-TS001
                    NavigationManager.getInstance().build(RouterConfig.QUESTION).withBoolean("isTS001", true).navigation(this)
                }
            } else {
                if (productType == Constants.SETTING_BOOK) {
//电子说明书-TS004
                    NavigationManager.getInstance().build(RouterConfig.PDF).withBoolean("isTS001", false).navigation(this)
                } else {
                    // FAQ-TS004
                    NavigationManager.getInstance().build(RouterConfig.QUESTION).withBoolean("isTS001", false).navigation(this)
                }
            }
        }

    val adapter = MyAdapter(productType == 1)
    adapter.onPickListener = { isTS001 ->
    if (isTS001) {
    if (productType == Constants.SETTING_BOOK) {
    //电子说明书-TS001
    } else {
    //FAQ-TS001
    NavigationManager.getInstance().build(RouterConfig.QUESTION).withBoolean("isTS001", true).navigation(this)
    }
    } else {
    if (productType == Constants.SETTING_BOOK) {
    //电子说明书-TS004
    NavigationManager.getInstance().build(RouterConfig.PDF).withBoolean("isTS001", false).navigation(this)
    } else {
    //FAQ-TS004
    NavigationManager.getInstance().build(RouterConfig.QUESTION).withBoolean("isTS001", false).navigation(this)
    }
    }
    }

    electronicManualRecycler.layoutManager = LinearLayoutManager(this)
    electronicManualRecycler.adapter = adapter
    }

    override fun initData() {
    }

    private class MyAdapter(private val isFAQ: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var onPickListener: ((isTS001: Boolean) -> Unit)? = null

    private val optionList: ArrayList<String> = ArrayList(2)

        init {
//由于 TC001 的说明书为旧版本 样式， 2024-4-9 产品决定先hide，只放 TS004 的说明书
            if (isFAQ) {
                optionList.add("TS001")
            }
            optionList.add("TS004")
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder {
            return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_electronic_manual, parent, false))
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
