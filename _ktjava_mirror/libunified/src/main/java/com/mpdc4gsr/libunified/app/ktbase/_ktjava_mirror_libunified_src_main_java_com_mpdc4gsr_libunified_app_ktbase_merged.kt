// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\ktbase' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\ktbase\libunified_src_main_java_com_mpdc4gsr_libunified_app_ktbase_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\ktbase' subtree
// Files: 7; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\ktbase\BaseActivity.kt =====

package com.mpdc4gsr.libunified.app.ktbase

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.bean.response.ResponseUserInfo
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.common.UserInfoManager
import com.mpdc4gsr.libunified.app.compose.dialogs.LoadingDialogState
import com.mpdc4gsr.libunified.app.compose.dialogs.ProgressDialogState
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.bean.CommonBean
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

abstract class BaseActivity : AppCompatActivity() {
    val TAG = this.javaClass.simpleName
    protected abstract fun initContentView(): Int
    protected abstract fun initView()
    protected abstract fun initData()
    protected var savedInstanceState: Bundle? = null
    protected open fun isLockPortrait(): Boolean = true
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BaseApplication.instance.activitys.add(this)
        this.savedInstanceState = savedInstanceState
        observeDeviceEvents()
        if (isLockPortrait()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        @Suppress("DEPRECATION")
        window.navigationBarColor = ContextCompat.getColor(this, R.color.toolbar_16131E)
        setContentView(initContentView())
        initView()
        initData()
        synLogin()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            AppLanguageUtils.attachBaseContext(
                newBase,
                ConstantLanguages.ENGLISH
            )
        )
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        cameraDialogState.dismiss()
        super.onDestroy()
        activityScope.cancel()
        BaseApplication.instance.activitys.remove(this)
    }

    private fun observeDeviceEvents() {
        activityScope.launch {
            DeviceEventManager.deviceConnectionState.collectLatest { state ->
                state?.let {
                    if (it.isConnected) {
                        connected()
                    } else {
                        disConnected()
                    }
                }
            }
        }
        activityScope.launch {
            DeviceEventManager.socketConnectionState.collectLatest { state ->
                state?.let {
                    Log.d("onSocketConnectState", "${it.isConnected}")
                    if (it.isConnected) {
                        onSocketConnected(it.isTS004)
                    } else {
                        onSocketDisConnected(it.isTS004)
                    }
                }
            }
        }
    }

    protected open fun connected() {
    }

    protected open fun disConnected() {
    }

    protected open fun onSocketConnected(isTS004: Boolean) {
    }

    protected open fun onSocketDisConnected(isTS004: Boolean) {
    }

    private val loadingDialogState by lazy { LoadingDialogState(this) }
    fun showLoadingDialog(
        @StringRes resId: Int = R.string.tip_loading,
    ) {
        showLoadingDialog(getString(resId))
    }

    fun showLoadingDialog(text: CharSequence?) {
        loadingDialogState.show(text?.toString() ?: "")
    }

    fun dismissLoadingDialog() {
        loadingDialogState.dismiss()
    }

    private val cameraDialogState by lazy { ProgressDialogState(this) }
    fun showCameraLoading() {
        try {
            if (!(isFinishing && isDestroyed)) {
                cameraDialogState.show(
                    message = getString(R.string.tip_loading),
                    progress = -1f,
                    cancelable = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing camera loading: ${e.message}")
        }
    }

    fun dismissCameraLoading() {
        cameraDialogState.dismiss()
    }

    private fun synLogin() {
        if (this.javaClass.simpleName == "MainComposeActivity") {
            LMS.getInstance().syncUserInfo()
        }
        if (SharedManager.getHasShowClause() && LMS.getInstance().isLogin) {
            LMS.getInstance().getUserInfo { userinfo: CommonBean ->
                try {
                    val infoData = Gson().fromJson(userinfo.data, ResponseUserInfo::class.java)
                    UserInfoManager.getInstance().login(
                        token = LMS.getInstance().token,
                        userId = infoData.topdonId,
                        phone = infoData.phone,
                        email = infoData.email,
                        nickname = infoData.userName,
                        headUrl = infoData.avatar,
                    )
                } catch (e: Exception) {
                    XLog.e("login error:${e.message}")
                }
            }
        } else {
            if (UserInfoManager.getInstance().isLogin()) {
                UserInfoManager.getInstance().logout()
            }
        }
    }

    protected class TakePhotoResult : ActivityResultContract<File, File?>() {
        private lateinit var file: File
        override fun createIntent(
            context: Context,
            input: File,
        ): Intent {
            file = input
            val uri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            return Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }

        override fun parseResult(
            resultCode: Int,
            intent: Intent?,
        ): File? = if (resultCode == RESULT_OK) file else null
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\ktbase\BaseFragment.kt =====

package com.mpdc4gsr.libunified.app.ktbase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.compose.dialogs.LoadingDialogState
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseFragment : Fragment() {
    val TAG = BaseFragment::class.java.simpleName
    abstract fun initContentView(): Int
    abstract fun initView()
    abstract fun initData()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(initContentView(), container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        observeDeviceEvents()
        initView()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
        } else {
            initData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private val loadingDialogState by lazy { LoadingDialogState(requireContext()) }
    fun showLoadingDialog(
        @StringRes resId: Int = 0,
    ) {
        val message = if (resId == 0) getString(R.string.tip_loading) else getString(resId)
        loadingDialogState.show(message)
    }

    fun showLoadingDialog(text: CharSequence) {
        loadingDialogState.show(text.toString())
    }

    fun dismissLoadingDialog() {
        loadingDialogState.dismiss()
    }

    private fun observeDeviceEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            DeviceEventManager.deviceConnectionState.collectLatest { state ->
                state?.let {
                    if (it.isConnected) {
                        connected()
                    } else {
                        disConnected()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            DeviceEventManager.socketConnectionState.collectLatest { state ->
                state?.let {
                    if (it.isConnected) {
                        onSocketConnected(it.isTS004)
                    } else {
                        onSocketDisConnected(it.isTS004)
                    }
                }
            }
        }
    }

    protected open fun connected() {
    }

    protected open fun disConnected() {
    }

    protected open fun onSocketConnected(isTS004: Boolean) {
    }

    protected open fun onSocketDisConnected(isTS004: Boolean) {
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\ktbase\BaseViewModel.kt =====

package com.mpdc4gsr.libunified.app.ktbase

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel(), LifecycleObserver {
    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val isRefreshing: Boolean = false
    )

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        data class ShowMessage(val message: String) : UiEvent()
        object NavigateBack : UiEvent()
    }

    protected val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    protected val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    protected val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleError(exception)
    }

    protected open fun handleError(exception: Throwable) {
        val errorMessage = exception.message ?: "Unknown error occurred"
        _uiState.update { it.copy(error = errorMessage, isLoading = false) }
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowError(errorMessage))
        }
    }

    protected fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    protected fun setRefreshing(isRefreshing: Boolean) {
        _uiState.update { it.copy(isRefreshing = isRefreshing) }
    }

    open fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    protected fun launchWithErrorHandling(
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(exceptionHandler) {
            block()
        }
    }

    protected fun launchWithLoading(
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(exceptionHandler) {
            setLoading(true)
            try {
                block()
            } finally {
                setLoading(false)
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\ktbase\BaseViewModelActivity.kt =====

package com.mpdc4gsr.libunified.app.ktbase

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.compose.dialogs.SimpleMessageDialogState
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
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
            is BaseViewModel.UiEvent.NavigateBack -> onBackPressedDispatcher.onBackPressed()
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

    private val messageDialogState by lazy { SimpleMessageDialogState(this) }
    open fun httpErrorTip(
        text: String,
        requestUrl: String,
    ) {
        messageDialogState.show(
            iconRes = R.drawable.ic_tip_error_svg,
            message = text
        )
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\ktbase\BaseViewModelFragment.kt =====

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


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\ktbase\BaseWifiActivity.kt =====

package com.mpdc4gsr.libunified.app.ktbase

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.preference.PreferenceManager
import com.mpdc4gsr.libunified.app.utils.NetWorkUtils

abstract class BaseWifiActivity : BaseActivity() {
    protected val permissionList by lazy {
        if (this.applicationInfo.targetSdkVersion >= 34) {
            listOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else if (this.applicationInfo.targetSdkVersion == 33) {
            mutableListOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else {
            mutableListOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= 29) {
            NetWorkUtils.switchNetwork(true)
        }
        super.onCreate(savedInstanceState)
        PreferenceManager.getDefaultSharedPreferences(this@BaseWifiActivity)
            .edit()
            .putBoolean("use-sw-codec", true)
            .apply()
        PreferenceManager.getDefaultSharedPreferences(this@BaseWifiActivity)
            .getBoolean("auto_audio", false)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= 29) {
            NetWorkUtils.switchNetwork(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\ktbase\ViewModelFactory.kt =====

package com.mpdc4gsr.libunified.app.ktbase

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class BaseViewModelFactory(
    private val application: Application,
    private val repositories: Map<Class<*>, Any> = emptyMap()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(BaseViewModel::class.java) -> {
                BaseViewModel() as T
            }

            else -> {
                try {
                    // Try to create with application context
                    val constructor = modelClass.getDeclaredConstructor(Application::class.java)
                    constructor.newInstance(application) as T
                } catch (e: NoSuchMethodException) {
                    try {
                        // Try to create with repositories
                        createWithRepositories(modelClass)
                    } catch (e: Exception) {
                        // Fallback to default constructor
                        modelClass.getDeclaredConstructor().newInstance() as T
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : ViewModel> createWithRepositories(modelClass: Class<T>): T {
        val constructors = modelClass.declaredConstructors
        for (constructor in constructors) {
            val parameterTypes = constructor.parameterTypes
            val parameters = mutableListOf<Any>()
            var canCreate = true
            for (paramType in parameterTypes) {
                when {
                    paramType == Application::class.java -> {
                        parameters.add(application)
                    }

                    repositories.containsKey(paramType) -> {
                        parameters.add(repositories[paramType]!!)
                    }

                    else -> {
                        canCreate = false
                        break
                    }
                }
            }
            if (canCreate) {
                return constructor.newInstance(*parameters.toTypedArray()) as T
            }
        }
        throw IllegalArgumentException("Cannot create ViewModel ${modelClass.simpleName}")
    }

    class Builder(private val application: Application) {
        private val repositories = mutableMapOf<Class<*>, Any>()
        fun <T : Any> addRepository(repositoryClass: Class<T>, repository: T): Builder {
            repositories[repositoryClass] = repository
            return this
        }

        inline fun <reified T : Any> addRepository(repository: T): Builder {
            return addRepository(T::class.java, repository)
        }

        fun build(): BaseViewModelFactory {
            return BaseViewModelFactory(application, repositories)
        }
    }
}

inline fun <reified T : ViewModel> androidx.lifecycle.ViewModelStoreOwner.createViewModelWithFactory(
    factory: BaseViewModelFactory
): T {
    return ViewModelProvider(this, factory)[T::class.java]
}