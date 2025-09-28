package com.mpdc4gsr.libunified.app.navigation

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.config.RouterConfig
import com.mpdc4gsr.libunified.databinding.LayoutBottomNavigationBinding

/**
 * Helper class to add consistent bottom navigation to activities
 * Following MVVM architecture and repository pattern guidelines
 */
class BottomNavigationHelper private constructor(
    private val activity: Activity,
    private val currentPage: NavigationPage = NavigationPage.MAIN
) {
    
    enum class NavigationPage {
        GALLERY, MAIN, MINE
    }
    
    companion object {
        fun create(activity: Activity, currentPage: NavigationPage = NavigationPage.MAIN): BottomNavigationHelper {
            return BottomNavigationHelper(activity, currentPage)
        }
    }
    
    private var binding: LayoutBottomNavigationBinding? = null
    private var onNavigationItemSelected: ((NavigationPage) -> Unit)? = null
    
    /**
     * Add bottom navigation to an activity's layout
     * @param parentLayout The parent layout to add navigation to (should be ConstraintLayout)
     * @param contentAboveNavigation The view that should be above navigation
     */
    fun addToActivity(parentLayout: ViewGroup, contentAboveNavigation: View? = null): View {
        binding = LayoutBottomNavigationBinding.inflate(LayoutInflater.from(activity))
        
        // Add the navigation layout to parent
        val layoutParams = when (parentLayout) {
            is ConstraintLayout -> {
                ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    
                    // If there's content above, constrain to it
                    contentAboveNavigation?.let { content ->
                        topToBottom = content.id
                    }
                }
            }
            else -> ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        parentLayout.addView(binding!!.root, layoutParams)
        
        // Update content layout if needed
        contentAboveNavigation?.let { content ->
            if (parentLayout is ConstraintLayout) {
                val contentParams = content.layoutParams as ConstraintLayout.LayoutParams
                contentParams.bottomToTop = binding!!.root.id
                content.layoutParams = contentParams
            }
        }
        
        setupNavigation()
        updateSelectedState()
        
        return binding!!.root
    }
    
    private fun setupNavigation() {
        binding?.apply {
            clNavGallery.setOnClickListener { 
                onNavigationItemSelected?.invoke(NavigationPage.GALLERY) ?: navigateToDefault(NavigationPage.GALLERY)
            }
            
            clNavMain.setOnClickListener { 
                onNavigationItemSelected?.invoke(NavigationPage.MAIN) ?: navigateToDefault(NavigationPage.MAIN)
            }
            
            clNavMine.setOnClickListener { 
                onNavigationItemSelected?.invoke(NavigationPage.MINE) ?: navigateToDefault(NavigationPage.MINE)
            }
        }
    }
    
    private fun navigateToDefault(page: NavigationPage) {
        val routerPath = when (page) {
            NavigationPage.GALLERY -> RouterConfig.GALLERY
            NavigationPage.MAIN -> RouterConfig.IR_MAIN
            NavigationPage.MINE -> RouterConfig.MINE
        }
        
        try {
            NavigationManager.getInstance()
                .build(routerPath)
                .navigation(activity)
        } catch (e: Exception) {
            // Fallback to basic navigation
            when (page) {
                NavigationPage.GALLERY -> {
                    // Try to navigate to main app gallery
                    val intent = Intent()
                    intent.setClassName(activity, "mpdc4gsr.activities.MainActivity")
                    intent.putExtra("page", 0)
                    activity.startActivity(intent)
                }
                NavigationPage.MAIN -> {
                    val intent = Intent()
                    intent.setClassName(activity, "mpdc4gsr.activities.MainActivity") 
                    intent.putExtra("page", 1)
                    activity.startActivity(intent)
                }
                NavigationPage.MINE -> {
                    val intent = Intent()
                    intent.setClassName(activity, "mpdc4gsr.activities.MainActivity")
                    intent.putExtra("page", 2)
                    activity.startActivity(intent)
                }
            }
        }
    }
    
    fun setOnNavigationItemSelectedListener(listener: (NavigationPage) -> Unit) {
        onNavigationItemSelected = listener
    }
    
    fun updateSelectedPage(page: NavigationPage) {
        currentPage
        updateSelectedState()
    }
    
    private fun updateSelectedState() {
        binding?.apply {
            // Reset all selections
            ivNavGallery.isSelected = false
            tvNavGallery.isSelected = false
            ivNavMain.isSelected = false
            tvNavMain.isSelected = false
            ivNavMine.isSelected = false
            tvNavMine.isSelected = false
            
            // Set current selection
            when (currentPage) {
                NavigationPage.GALLERY -> {
                    ivNavGallery.isSelected = true
                    tvNavGallery.isSelected = true
                    updateBackgroundImage(false)
                }
                NavigationPage.MAIN -> {
                    ivNavMain.isSelected = true
                    tvNavMain.isSelected = true
                    updateBackgroundImage(true)
                }
                NavigationPage.MINE -> {
                    ivNavMine.isSelected = true
                    tvNavMine.isSelected = true
                    updateBackgroundImage(false)
                }
            }
        }
    }
    
    private fun updateBackgroundImage(isMainSelected: Boolean) {
        binding?.ivBottomBg?.setImageResource(
            if (isMainSelected) R.drawable.ic_main_bg_select 
            else R.drawable.ic_main_bg_not_select
        )
    }
    
    /**
     * Call this in activity's onDestroy to clean up resources
     */
    fun cleanup() {
        binding = null
        onNavigationItemSelected = null
    }
}