package com.mpdc4gsr.libunified.app.ktbase

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.mpdc4gsr.libunified.R
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
            // Look for a main content view with a conventional ID first
            val mainContentView = contentView.findViewById<View>(android.R.id.content)
            
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