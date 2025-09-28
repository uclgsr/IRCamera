package mpdc4gsr.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.csl.irCamera.R
import mpdc4gsr.ui_components.SensorDashboardFragment

/**
 * Test activity for the SensorDashboardFragment to verify scrollable behavior
 */
class SensorDashboardTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_dashboard_test)
        
        if (savedInstanceState == null) {
            val fragment = SensorDashboardFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, "sensor_dashboard")
                .commit()
                
            // Simulate some sensor data updates after a delay
            fragment.view?.postDelayed({
                testSensorUpdates(fragment)
            }, 1000)
        }
    }
    
    private fun testSensorUpdates(fragment: SensorDashboardFragment) {
        // Test different sensor statuses
        fragment.updateSensorStatus("thermal_camera", SensorDashboardFragment.SensorStatus.CONNECTED, "TC001 Connected")
        fragment.updateSensorStatus("rgb_camera", SensorDashboardFragment.SensorStatus.STREAMING, "1080p @ 30fps")
        fragment.updateSensorStatus("shimmer_gsr", SensorDashboardFragment.SensorStatus.ERROR, "Device not found")
        fragment.updateSensorStatus("audio_recorder", SensorDashboardFragment.SensorStatus.SIMULATION, "Using test audio data")
        
        // Test recording status
        fragment.updateRecordingStatus(true, "TEST_SESSION_001")
        
        // Test simulation warning
        fragment.showSimulationWarning("thermal_camera", true)
        
        // Test multi-device status
        fragment.updateMultiDeviceStatus(2, 1, 4)
    }
}