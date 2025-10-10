package com.mpdc4gsr.module.thermalunified.report.viewmodel

import android.text.TextUtils
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.network.IResponseCallback
import com.mpdc4gsr.libunified.app.lms.utils.NetworkUtils
import com.mpdc4gsr.libunified.app.lms.utils.StringUtils
import com.mpdc4gsr.libunified.app.repository.BaseRepository
import com.mpdc4gsr.libunified.app.utils.HttpHelp
import com.mpdc4gsr.module.thermalunified.compat.ContextProvider
import com.mpdc4gsr.module.thermalunified.report.bean.ReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ModernPdfViewModel : BaseViewModel() {
    // Modern StateFlow-based state management
    private val _reportDataState = MutableStateFlow<ReportDataState>(ReportDataState.Idle)
    val reportDataState: StateFlow<ReportDataState> = _reportDataState.asStateFlow()
    private val _paginationState = MutableStateFlow(PaginationState())
    val paginationState: StateFlow<PaginationState> = _paginationState.asStateFlow()

    // One-time events using SharedFlow
    private val _events = MutableSharedFlow<PdfEvent>()
    val events: SharedFlow<PdfEvent> = _events.asSharedFlow()

    // Data classes for type-safe state management
    sealed class ReportDataState {
        object Idle : ReportDataState()

        object Loading : ReportDataState()

        data class Success(
            val data: ReportData,
            val isLoadMore: Boolean = false,
        ) : ReportDataState()

        data class Error(
            val message: String,
            val code: Int = -1,
        ) : ReportDataState()

        object NoNetwork : ReportDataState()
    }

    data class PaginationState(
        val currentPage: Int = 1,
        val hasMorePages: Boolean = true,
        val totalPages: Int = 0,
        val isLoadingMore: Boolean = false,
    )

    sealed class PdfEvent {
        data class ShowToast(
            val message: String,
        ) : PdfEvent()

        data class ShowError(
            val message: String,
        ) : PdfEvent()

        data class NavigateToReport(
            val reportId: String,
        ) : PdfEvent()

        object RefreshCompleted : PdfEvent()

        data class ShareReport(
            val reportData: ReportData,
        ) : PdfEvent()
    }

    // Repository instance
    private val reportRepository = ReportRepository()

    fun getReportData(
        isTC007: Boolean,
        page: Int = 1,
        forceRefresh: Boolean = false,
    ) {
        launchWithLoading {
            try {
                // Check network connectivity first
                if (!NetworkUtils.isConnected(ContextProvider.getContext())) {
                    _reportDataState.value = ReportDataState.NoNetwork
                    _events.emit(PdfEvent.ShowError("No network connection available"))
                    return@launchWithLoading
                }
                // Update loading states
                if (page == 1) {
                    _reportDataState.value = ReportDataState.Loading
                } else {
                    _paginationState.value = _paginationState.value.copy(isLoadingMore = true)
                }
                // Fetch data through repository
                val result = reportRepository.getReportData(isTC007, page, forceRefresh)
                when (result) {
                    is BaseRepository.Result.Success -> {
                        val reportData = result.data
                        _reportDataState.value =
                            ReportDataState.Success(
                                data = reportData,
                                isLoadMore = page > 1,
                            )
                        // Update pagination state
                        _paginationState.value =
                            _paginationState.value.copy(
                                currentPage = page,
                                hasMorePages = reportData.hasMoreData(),
                                isLoadingMore = false,
                            )
                        if (page == 1) {
                            _events.emit(PdfEvent.RefreshCompleted)
                        }
                    }

                    is BaseRepository.Result.Error -> {
                        val errorMessage = result.exception.message ?: "Unknown error occurred"
                        _reportDataState.value = ReportDataState.Error(errorMessage)
                        _paginationState.value = _paginationState.value.copy(isLoadingMore = false)
                        _events.emit(PdfEvent.ShowError(errorMessage))
                    }

                    is BaseRepository.Result.Loading -> {
                        // Already handled above
                    }
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to load report data"
                _reportDataState.value = ReportDataState.Error(errorMessage)
                _paginationState.value = _paginationState.value.copy(isLoadingMore = false)
                _events.emit(PdfEvent.ShowError(errorMessage))
            }
        }
    }

    fun loadNextPage(isTC007: Boolean) {
        val currentState = _paginationState.value
        if (currentState.hasMorePages && !currentState.isLoadingMore) {
            getReportData(isTC007, currentState.currentPage + 1)
        }
    }

    fun refreshData(isTC007: Boolean) {
        getReportData(isTC007, 1, forceRefresh = true)
    }

    fun navigateToReport(reportId: String) {
        launchWithErrorHandling {
            _events.emit(PdfEvent.NavigateToReport(reportId))
        }
    }

    fun shareReport(reportData: ReportData) {
        launchWithErrorHandling {
            _events.emit(PdfEvent.ShareReport(reportData))
        }
    }

    fun clearErrorState() {
        super.clearError()
        if (_reportDataState.value is ReportDataState.Error) {
            _reportDataState.value = ReportDataState.Idle
        }
    }

    fun resetStates() {
        _reportDataState.value = ReportDataState.Idle
        _paginationState.value = PaginationState()
    }

    private inner class ReportRepository : BaseRepository() {
        private val cacheKey = "report_data"

        suspend fun getReportData(
            isTC007: Boolean,
            page: Int,
            forceRefresh: Boolean = false,
        ): BaseRepository.Result<ReportData> =
            safeCall {
                val key = "${cacheKey}_${isTC007}_$page"
                if (!forceRefresh) {
                    // Try cached data first (5 minute cache)
                    getCachedOrExecute(key, 5 * 60 * 1000L) {
                        fetchReportDataFromNetwork(isTC007, page)
                    }
                } else {
                    // Force refresh - clear cache and fetch
                    clearCache(key)
                    fetchReportDataFromNetwork(isTC007, page)
                }
            }

        private suspend fun fetchReportDataFromNetwork(
            isTC007: Boolean,
            page: Int,
        ): ReportData =
            suspendCancellableCoroutine { continuation ->
                val downLatch = CountDownLatch(1)
                var result: ReportData? = null
                var error: Exception? = null
                HttpHelp.getFirstReportData(
                    isTC007,
                    page,
                    object : IResponseCallback {
                        override fun onResponse(response: String?) {
                            try {
                                result =
                                    if (!response.isNullOrEmpty()) {
                                        Gson().fromJson(response, ReportData::class.java)
                                    } else {
                                        ReportData().apply {
                                            code = -1
                                            msg = "Empty response from server"
                                        }
                                    }
                            } catch (e: Exception) {
                                error = Exception("JSON parsing error: ${e.message}")
                            } finally {
                                downLatch.countDown()
                            }
                        }

                        override fun onFail(exception: Exception?) {
                            error = exception ?: Exception("Network request failed")
                            result =
                                ReportData().apply {
                                    msg = exception?.message ?: "Network error"
                                    code = -1
                                }
                            downLatch.countDown()
                        }

                        override fun onFail(
                            failMsg: String?,
                            errorCode: String,
                        ) {
                            super.onFail(failMsg, errorCode)
                            // Handle localized error messages
                            try {
                                val localizedMessage =
                                    StringUtils.getResString(
                                        LMS.mContext,
                                        if (TextUtils.isEmpty(errorCode)) -500 else errorCode.toInt(),
                                    )
                                // Emit toast event on main thread
                                viewModelScope.launch {
                                    _events.emit(PdfEvent.ShowToast(localizedMessage))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            error = Exception(failMsg ?: "Server error")
                            result =
                                ReportData().apply {
                                    msg = failMsg
                                    code = if (TextUtils.isEmpty(errorCode)) -500 else errorCode.toInt()
                                }
                            downLatch.countDown()
                        }
                    },
                )
                continuation.invokeOnCancellation {
                    downLatch.countDown()
                }
                // Wait for network response in IO dispatcher
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        downLatch.await()
                        when {
                            error != null -> continuation.resumeWithException(error!!)
                            result != null -> continuation.resume(result!!)
                            else -> continuation.resumeWithException(Exception("Unknown error occurred"))
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }
    }

    // Extension functions for ReportData
    private fun ReportData.hasMoreData(): Boolean =
        code == 200 && data?.records?.isNotEmpty() == true && data!!.records!!.size >= 20

    companion object {
        private const val TAG = "ModernPdfViewModel"
    }
}
