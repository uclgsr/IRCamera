package com.topdon.module.thermal.ir.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.topdon.lib.core.tools.GlideLoader
import com.topdon.module.thermal.ir.R
import kotlinx.coroutines.launch

class GalleryFragment : Fragment() {

    private var path = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gallery, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        path = requireArguments().getString("path")!!
        
        // Initialize view with findViewById
        val fragmentGalleryImg: ImageView = view.findViewById(R.id.fragment_gallery_img)
        Glide.with(this).load(path).into(fragmentGalleryImg)
    }

}