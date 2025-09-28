package com.mpdc4gsr.libunified.app.ktbase

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.dialog.MsgDialog
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import kotlin.coroutines.cancellation.CancellationException

abstract class BaseViewModelActivity<VM : BaseViewModel> : BaseActivity() {
    protected lateinit var viewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        initVM()
        super.onCreate(savedInstanceState)
        setupObservers()
    }

    private fun initVM() {
        providerVMClass().let {
            viewModel = ViewModelProvider(this).get(it)
            lifecycle.addObserver(viewModel)
        }
    }

    private fun setupObservers() {
        // Observe UI state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    handleUiState(uiState)
                }
            }
        }

        // Observe UI events
        lifecycleScope.launch {
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
            is BaseViewModel.UiEvent.NavigateBack -> onBackPressed()
        }
    }

    protected open fun showLoading() {
        // Override in subclasses to show loading indicator
    }

    protected open fun hideLoading() {
        // Override in subclasses to hide loading indicator
    }

    protected open fun showError(message: String) {
        httpErrorTip(message, "")
    }

    protected open fun showMessage(message: String) {
        // Override in subclasses for custom message display
        httpErrorTip(message, "")
    }

    abstract fun providerVMClass(): Class<VM>

    protected fun requestError(it: Exception?) {
        it?.run {
            when (it) {
                is TimeoutCancellationException -> httpErrorTip(
                    getString(R.string.http_time_out),
                    ""
                )

                is CancellationException -> Log.d(
                    "$TAG--->[ph][ph][ph][ph][ph][ph]",
                    it.message.toString()
                )

                else -> httpErrorTip(getString(R.string.http_code_z5004), "")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::viewModel.isInitialized) {
            lifecycle.removeObserver(viewModel)
        }
    }

    open fun httpErrorTip(
        text: String,
        requestUrl: String,
    ) {
        MsgDialog.Builder(this)
            .setMessage(text)
            .setImg(R.drawable.ic_tip_error_svg)
            .create().show()
    }
}
