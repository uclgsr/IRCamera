package mpdc4gsr.core.di
import android.content.Context
interface AppContainer {
    val context: Context
    // Add repository dependencies here as they are created
    // Example:
    // val sensorRepository: SensorRepository
    // val userRepository: UserRepository
    // val networkRepository: NetworkRepository
}
class DefaultAppContainer(
    override val context: Context
) : AppContainer {
    // Lazy initialization of dependencies
    // Add repository instances here
    // Example:
    // override val sensorRepository: SensorRepository by lazy {
    //     DefaultSensorRepository(context)
    // }
}
