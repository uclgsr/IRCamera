package com.topdon.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.topdon.module.thermal.R

/**
 * Mine Fragment migrated from user module
 * Simplified version for MPDC4GSR
 */
class MineFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Create a simple placeholder view
        return inflater.inflate(android.R.layout.simple_list_item_1, container, false)
    }
}