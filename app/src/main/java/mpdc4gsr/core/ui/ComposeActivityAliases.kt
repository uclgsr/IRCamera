package mpdc4gsr.core.ui

/**
 * Backward compatibility type aliases for Compose activities moved to feature modules.
 * This allows existing code to continue using the old package name during migration.
 */

// Base Compose Infrastructure
typealias BaseComposeActivity<VM> = com.mpdc4gsr.libunified.app.compose.base.BaseComposeActivity<VM>

// Note: BaseViewModel typealias is in BaseViewModel.kt to maintain proper type hierarchy

// GSR Feature Activities
typealias GSRGalleryComposeActivity = mpdc4gsr.feature.gsr.ui.GSRGalleryComposeActivity
typealias GSRQuickRecordingComposeActivity = mpdc4gsr.feature.gsr.ui.GSRQuickRecordingComposeActivity
typealias SensorDashboardComposeActivity = mpdc4gsr.feature.gsr.ui.SensorDashboardComposeActivity
typealias SensorDashboardComposeEnhanced = mpdc4gsr.feature.gsr.ui.SensorDashboardComposeEnhanced
typealias UnifiedSensorComposeActivity = mpdc4gsr.feature.gsr.ui.UnifiedSensorComposeActivity
typealias ShimmerConfigComposeActivity = mpdc4gsr.feature.gsr.ui.ShimmerConfigComposeActivity
// Note: Additional GSR activity aliases are in sensors/gsr/ActivityAliases.kt to maintain backward compatibility

// Network Feature Activities
typealias NetworkClientTestActivityCompose = mpdc4gsr.feature.network.ui.NetworkClientTestActivityCompose
typealias NetworkClientTestComposeActivity = mpdc4gsr.feature.network.ui.NetworkClientTestComposeActivity
typealias NetworkConfigComposeActivity = mpdc4gsr.feature.network.ui.NetworkConfigComposeActivity
typealias SimpleNetworkTestActivityCompose = mpdc4gsr.feature.network.ui.SimpleNetworkTestActivityCompose

// Camera Feature Activities
typealias DualModeCameraActivityCompose = mpdc4gsr.feature.camera.ui.DualModeCameraActivityCompose

// Settings Feature Activities
typealias SettingsComposeActivity = mpdc4gsr.feature.settings.ui.SettingsComposeActivity
typealias MoreHelpComposeActivity = mpdc4gsr.feature.settings.ui.MoreHelpComposeActivity
typealias PolicyComposeActivity = mpdc4gsr.feature.settings.ui.PolicyComposeActivity
typealias ClauseComposeActivity = mpdc4gsr.feature.settings.ui.ClauseComposeActivity
typealias VersionComposeActivity = mpdc4gsr.feature.settings.ui.VersionComposeActivity
typealias WebViewComposeActivity = mpdc4gsr.feature.settings.ui.WebViewComposeActivity
typealias PdfComposeActivity = mpdc4gsr.feature.settings.ui.PdfComposeActivity

// Testing Feature Activities
typealias SensorDashboardTestActivityCompose = mpdc4gsr.feature.testing.ui.SensorDashboardTestActivityCompose
typealias FaultTolerantRecordingComposeActivity = mpdc4gsr.feature.testing.ui.FaultTolerantRecordingComposeActivity
typealias ComposeComponentsShowcaseActivity = mpdc4gsr.feature.testing.ui.ComposeComponentsShowcaseActivity
typealias ComposeMigrationLauncherActivity = mpdc4gsr.feature.testing.ui.ComposeMigrationLauncherActivity

// Main Feature Activities
typealias UnifiedComposeActivity = mpdc4gsr.feature.main.ui.UnifiedComposeActivity
typealias DeviceTypeComposeActivity = mpdc4gsr.feature.main.ui.DeviceTypeComposeActivity
// Note: MainActivity alias is in MainActivity.kt to avoid circular reference

// Thermal Feature Activities
typealias IRGalleryEditComposeActivity = mpdc4gsr.feature.thermal.ui.IRGalleryEditComposeActivity
