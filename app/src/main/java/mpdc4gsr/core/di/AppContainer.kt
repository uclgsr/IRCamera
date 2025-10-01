package mpdc4gsr.core.di

import android.content.Context

/**
 * Application-level dependency injection container
 *
 * This provides a simple manual DI setup following Clean Architecture principles.
 * For future enhancement, consider migrating to Hilt for compile-time verification
 * and better testing support.
 *
 * Usage:
 * ```
 * val appContainer = (application as IRCameraApplication).appContainer
 * val repository = appContainer.sensorRepository
 * ```
 */
interface AppContainer {
    val context: Context

    // Add repository dependencies here as they are created
    // Example:
    // val sensorRepository: SensorRepository
    // val userRepository: UserRepository
    // val networkRepository: NetworkRepository
}

/**
 * Default implementation of AppContainer
 */
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
