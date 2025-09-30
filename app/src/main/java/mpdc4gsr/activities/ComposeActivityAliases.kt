package mpdc4gsr.activities

/**
 * Backward compatibility type aliases for Compose activities moved to feature modules.
 * This allows existing code to continue using the old package name during migration.
 */

// GSR Feature Activities
typealias GSRGalleryActivityCompose = mpdc4gsr.feature.gsr.ui.GSRGalleryActivityCompose
typealias GSRQuickRecordingActivityCompose = mpdc4gsr.feature.gsr.ui.GSRQuickRecordingActivityCompose
typealias SensorDashboardComposeActivity = mpdc4gsr.feature.gsr.ui.SensorDashboardComposeActivity
typealias SensorDashboardComposeEnhanced = mpdc4gsr.feature.gsr.ui.SensorDashboardComposeEnhanced
typealias UnifiedSensorActivityCompose = mpdc4gsr.feature.gsr.ui.UnifiedSensorActivityCompose
typealias ShimmerConfigComposeActivity = mpdc4gsr.feature.gsr.ui.ShimmerConfigComposeActivity
typealias GSRDeviceManagementActivityCompose = mpdc4gsr.feature.gsr.ui.GSRDeviceManagementComposeActivity
typealias MultiModalRecordingActivityCompose = mpdc4gsr.feature.gsr.ui.MultiModalRecordingComposeActivity
typealias SessionManagerActivityCompose = mpdc4gsr.feature.gsr.ui.SessionManagerComposeActivity

// Network Feature Activities
typealias NetworkClientTestActivityCompose = mpdc4gsr.feature.network.ui.NetworkClientTestActivityCompose
typealias NetworkClientTestComposeActivity = mpdc4gsr.feature.network.ui.NetworkClientTestComposeActivity
typealias NetworkConfigActivityCompose = mpdc4gsr.feature.network.ui.NetworkConfigActivityCompose
typealias SimpleNetworkTestActivityCompose = mpdc4gsr.feature.network.ui.SimpleNetworkTestActivityCompose

// Camera Feature Activities
typealias DualModeCameraActivityCompose = mpdc4gsr.feature.camera.ui.DualModeCameraActivityCompose

// Settings Feature Activities
typealias SettingsComposeActivity = mpdc4gsr.feature.settings.ui.SettingsComposeActivity
typealias MoreHelpActivityCompose = mpdc4gsr.feature.settings.ui.MoreHelpActivityCompose
typealias PolicyActivityCompose = mpdc4gsr.feature.settings.ui.PolicyActivityCompose
typealias ClauseActivityCompose = mpdc4gsr.feature.settings.ui.ClauseActivityCompose
typealias VersionActivityCompose = mpdc4gsr.feature.settings.ui.VersionActivityCompose
typealias WebViewActivityCompose = mpdc4gsr.feature.settings.ui.WebViewActivityCompose
typealias PdfActivityCompose = mpdc4gsr.feature.settings.ui.PdfActivityCompose

// Testing Feature Activities
typealias SensorDashboardTestActivityCompose = mpdc4gsr.feature.testing.ui.SensorDashboardTestActivityCompose
typealias FaultTolerantRecordingActivityCompose = mpdc4gsr.feature.testing.ui.FaultTolerantRecordingActivityCompose
typealias ComposeComponentsShowcaseActivity = mpdc4gsr.feature.testing.ui.ComposeComponentsShowcaseActivity
typealias ComposeMigrationLauncherActivity = mpdc4gsr.feature.testing.ui.ComposeMigrationLauncherActivity

// Main Feature Activities
typealias UnifiedComposeActivity = mpdc4gsr.feature.main.ui.UnifiedComposeActivity
typealias DeviceTypeActivityCompose = mpdc4gsr.feature.main.ui.DeviceTypeActivityCompose
typealias MainActivity = mpdc4gsr.feature.main.ui.MainActivity

// Device Feature Activities
typealias BlankDevActivityCompose = mpdc4gsr.feature.device.ui.BlankDevActivityCompose
typealias BlankDevComposeActivity = mpdc4gsr.feature.device.ui.BlankDevComposeActivity

// Thermal Feature Activities
typealias IRGalleryEditActivityCompose = mpdc4gsr.feature.thermal.ui.IRGalleryEditActivityCompose
