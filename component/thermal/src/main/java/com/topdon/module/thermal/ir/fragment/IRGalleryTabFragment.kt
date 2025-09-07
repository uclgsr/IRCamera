package com.topdon.module.thermal.ir.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.topdon.module.thermal.R

/**
 * IRGalleryTabFragment - Consolidated thermal gallery tab fragment
 * Migrated from thermal-ir module for MPDC4GSR
 */
class IRGalleryTabFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(android.R.layout.simple_list_item_1, container, false)
    }
}