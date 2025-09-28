package com.mpdc4gsr.libunified.app.ktbase

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.mpdc4gsr.libunified.app.navigation.BottomNavigationHelper

/**
 * Base Activity with automatic bottom navigation
 * Extends BaseActivity to maintain existing functionality while adding navigation
 * Following MVVM architecture patterns
 */
abstract class BaseNavigationActivity : BaseActivity() {
    
    private var navigationHelper: BottomNavigationHelper? = null
    
    /**
     * Override this to specify the current navigation page
     * Default is MAIN
     */
    protected open fun getCurrentNavigationPage(): BottomNavigationHelper.NavigationPage {
        return BottomNavigationHelper.NavigationPage.MAIN
    }
    
    /**
     * Override this to disable automatic navigation addition
     * Default is true (navigation will be added)
     */
    protected open fun shouldShowBottomNavigation(): Boolean = true
    
    /**
     * Override this to handle custom navigation behavior
     * If not overridden, uses default navigation via NavigationManager
     */
    protected open fun onNavigationItemSelected(page: BottomNavigationHelper.NavigationPage) {
        // Default behavior handled by BottomNavigationHelper
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (shouldShowBottomNavigation()) {
            setupBottomNavigation()
        }
    }
    
    private fun setupBottomNavigation() {
        // Find the root layout
        val rootView = findViewById<View>(android.R.id.content) as? ViewGroup
        val contentView = rootView?.getChildAt(0) as? ViewGroup
        
        if (contentView is ConstraintLayout) {
            // Find the main content view (usually the one that takes most space)
            val mainContentView = findMainContentView(contentView)
            
            navigationHelper = BottomNavigationHelper.create(this, getCurrentNavigationPage())
            navigationHelper?.setOnNavigationItemSelectedListener { page ->
                onNavigationItemSelected(page)
            }
            
            navigationHelper?.addToActivity(contentView, mainContentView)
        } else {
            // Fallback: try to add navigation to any ViewGroup
            contentView?.let { parent ->
                navigationHelper = BottomNavigationHelper.create(this, getCurrentNavigationPage())
                navigationHelper?.setOnNavigationItemSelectedListener { page ->
                    onNavigationItemSelected(page)
                }
                navigationHelper?.addToActivity(parent)
            }
        }
    }
    
    /**
     * Find the main content view that should be above navigation
     * This is usually the largest view or the one with most constraints
     */
    private fun findMainContentView(parent: ConstraintLayout): View? {
        var mainView: View? = null
        var maxArea = 0
        
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val layoutParams = child.layoutParams as? ConstraintLayout.LayoutParams
            
            // Skip views that are likely to be navigation or small UI elements
            if (layoutParams != null) {
                val area = child.width * child.height
                if (area > maxArea && !isLikelyBottomNavigation(child)) {
                    maxArea = area
                    mainView = child
                }
            }
        }
        
        return mainView
    }
    
    /**
     * Heuristic to determine if a view is likely bottom navigation
     */
    private fun isLikelyBottomNavigation(view: View): Boolean {
        val layoutParams = view.layoutParams as? ConstraintLayout.LayoutParams ?: return false
        
        // Check if view is constrained to bottom and has typical navigation characteristics
        return layoutParams.bottomToBottom == ConstraintLayout.LayoutParams.PARENT_ID &&
                view.id.toString().contains("bottom", ignoreCase = true) ||
                view.id.toString().contains("nav", ignoreCase = true)
    }
    
    /**
     * Update the selected navigation page
     */
    protected fun updateNavigationPage(page: BottomNavigationHelper.NavigationPage) {
        navigationHelper?.updateSelectedPage(page)
    }
    
    override fun onDestroy() {
        navigationHelper?.cleanup()
        super.onDestroy()
    }
}