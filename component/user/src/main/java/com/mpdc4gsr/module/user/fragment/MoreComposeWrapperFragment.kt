package com.mpdc4gsr.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mpdc4gsr.libunified.app.compose.theme.LibUnifiedTheme
import com.mpdc4gsr.module.user.compose.MoreComposeFragment
import com.mpdc4gsr.module.user.viewmodel.MoreComposeFragmentViewModel

class MoreComposeWrapperFragment : Fragment() {

    private val viewModel: MoreComposeFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val isTC007 = arguments?.getBoolean("IS_TC007", false) ?: false
                LibUnifiedTheme {
                    MoreComposeFragment(
                        viewModel = viewModel,
                        isTC007 = isTC007
                    )
                }
            }
        }
    }
}
