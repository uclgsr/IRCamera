package com.mpdc4gsr.libunified.app.ktbase
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
abstract class BaseViewModelFragment<VM : BaseViewModel> : BaseFragment() {
    protected lateinit var viewModel: VM
    abstract fun providerVMClass(): Class<VM>?
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        initVM()
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }
    private fun initVM() {
        providerVMClass()?.let {
            viewModel = ViewModelProvider(this).get(it)
            lifecycle.addObserver(viewModel)
        }
    }
    private fun setupObservers() {
        if (!this::viewModel.isInitialized) return
        // Observe UI state
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    handleUiState(uiState)
                }
            }
        }
        // Observe UI events
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvents.collect { event ->
                    handleUiEvent(event)
                }
            }
        }
    }
    protected open fun handleUiState(uiState: BaseViewModel.UiState) {
        // Handle loading state
        if (uiState.isLoading) {
            showLoading()
        } else {
            hideLoading()
        }
        // Handle error state
        uiState.error?.let { error ->
            showError(error)
        }
    }
    protected open fun handleUiEvent(event: BaseViewModel.UiEvent) {
        when (event) {
            is BaseViewModel.UiEvent.ShowError -> showError(event.message)
            is BaseViewModel.UiEvent.ShowMessage -> showMessage(event.message)
            is BaseViewModel.UiEvent.NavigateBack -> {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }
    protected open fun showLoading() {
        // Override in subclasses to show loading indicator
    }
    protected open fun hideLoading() {
        // Override in subclasses to hide loading indicator
    }
    protected open fun showError(message: String) {
        // Override in subclasses for custom error display
    }
    protected open fun showMessage(message: String) {
        // Override in subclasses for custom message display
    }
    override fun onDestroy() {
        super.onDestroy()
        if (this::viewModel.isInitialized) {
            lifecycle.removeObserver(viewModel)
        }
    }
}
