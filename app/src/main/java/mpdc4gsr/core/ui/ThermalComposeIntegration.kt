package mpdc4gsr.core.ui

/**
 * Integration examples showing how to wire Compose screens with existing ViewModels
 * These examples demonstrate the connection between new UI and existing business logic
 *
 * Migration strategy documentation:
 *
 * 1. Activity Integration:
 *    - Replace setContentView(R.layout.activity_name) with setContent { ComposableScreen() }
 *    - Keep existing Activity class for navigation and lifecycle management
 *    - Gradually migrate ViewModel interactions to Compose state
 *
 * 2. Fragment Replacement:
 *    - Replace Fragment onCreateView with Compose equivalent
 *    - Use FragmentContainerView with ComposeView if gradual migration needed
 *    - Convert Fragment arguments to Compose parameters
 *
 * 3. EventBus Integration:
 *    - Subscribe to EventBus in LaunchedEffect
 *    - Convert events to ViewModel state updates
 *    - Eventually replace EventBus with StateFlow/SharedFlow patterns
 *
 * 4. Custom View Replacement:
 *    - IrSurfaceView can be wrapped in AndroidView for immediate compatibility
 *    - TemperatureView functionality moved to Canvas-based Composables
 *    - TitleView completely replaced by TitleBar Composable
 *
 * 5. Testing Strategy:
 *    - Use @Preview for UI testing during development
 *    - Create integration tests for ViewModel-Compose interactions
 *    - Ensure thermal camera functionality remains unchanged
 */
