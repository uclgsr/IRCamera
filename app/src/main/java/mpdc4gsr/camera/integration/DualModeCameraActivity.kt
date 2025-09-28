package mpdc4gsr.camera.integration

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.csl.irCamera.R
import com.csl.irCamera.databinding.ActivityDualModeCameraBinding
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModelActivity
import mpdc4gsr.activities.MainActivity

class DualModeCameraActivity : BaseViewModelActivity<DualModeCameraViewModel>() {
    private lateinit var previewView: PreviewView
    private lateinit var cameraModeSelector: LinearLayout

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.onPermissionGranted()
            } else {
                viewModel.onPermissionDenied()
            }
        }

    override fun providerVMClass(): Class<DualModeCameraViewModel> =
        DualModeCameraViewModel::class.java

    override fun initContentView() = R.layout.activity_camera_test_consolidated

    override fun initView() {
        previewView = findViewById(R.id.preview_view)
        cameraModeSelector = findViewById(R.id.camera_mode_selector)
        setupBottomNavigation()

        val initialMode = intent.getStringExtra("INITIAL_MODE") ?: "VIDEO_4K"
        val enableSamsungOptimizations =
            intent.getBooleanExtra("ENABLE_SAMSUNG_OPTIMIZATIONS", true)

        setupObservers()
        viewModel.initialize(initialMode, enableSamsungOptimizations)
        checkCameraPermission()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.bottom_navigation)

        bottomNavigation.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.cl_nav_gallery)
            ?.setOnClickListener {
                navigateToMainActivity(0) // Gallery page
            }

        bottomNavigation.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.cl_nav_main)
            ?.setOnClickListener {
                navigateToMainActivity(1) // Main page
            }

        bottomNavigation.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.cl_nav_mine)
            ?.setOnClickListener {
                navigateToMainActivity(2) // Mine page
            }

        // Update navigation background to show main is selected (camera is main functionality)
        bottomNavigation.findViewById<android.widget.ImageView>(R.id.iv_navigation_bg)
            ?.setImageResource(R.drawable.ic_main_bg_select)
    }

    private fun navigateToMainActivity(pageIndex: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("page", pageIndex)
        }
        startActivity(intent)
        finish()
    }

    private fun setupObservers() {
        viewModel.permissionState.observe(this) { permissionState ->
            when (permissionState) {
                DualModeCameraViewModel.PermissionState.GRANTED -> {
                    viewModel.initializeCamera(this, this, previewView)
                }

                DualModeCameraViewModel.PermissionState.DENIED -> {
                    finish()
                }

                else -> { /* Handle other states if needed */
                }
            }
        }

        viewModel.cameraState.observe(this) { cameraState ->
            // Update UI based on camera state
            // Could add loading indicators, record buttons, etc.
        }

        viewModel.cameraMode.observe(this) { mode ->
            // Update mode selector UI
            updateModeSelector(mode)
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.statusMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearStatusMessage()
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.onPermissionGranted()
            }

            else -> {
                viewModel.requestPermission()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun updateModeSelector(mode: DualModeCameraViewModel.CameraMode) {
        // Update UI to reflect current camera mode
        // This would typically update button states, indicators, etc.
    }

    override fun initData() {
        // Initialize any data needed for the activity
        // This method is called by BaseActivity after initView()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cleanup()
    }
}
