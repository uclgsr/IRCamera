# Navigation Best Practices Guide

## Overview

This guide provides best practices for using the navigation system in the IRCamera application, following the
optimizations outlined in the Navigation Optimization Analysis.

## Navigation System Architecture

### Primary Navigation: UnifiedNavigation.kt

The application uses **UnifiedNavigation.kt** as the primary navigation system for Compose screens.

```kotlin
// Example: Navigating to GSR Settings
navController.navigate(UnifiedRoute.GSRSettings.route)

// Example: Navigating with arguments
navController.navigate(UnifiedRoute.GSRPlot.createRoute(sessionId))
```

### When to Use Each System

#### Use UnifiedNavigation for:

- All new Compose screens
- Feature navigation within the app module
- Type-safe navigation with sealed classes
- Screens with back stack management

#### Use NavigationManager for:

- Legacy activity launches (being phased out)
- Deep integration with thermal module activities
- Cross-module navigation (temporary)

## Best Practices

### 1. Route Definitions

Always define routes in the UnifiedRoute sealed class:

```kotlin
sealed class UnifiedRoute(val route: String) {
    object MyNewScreen : UnifiedRoute("my_new_screen")
    
    // For routes with parameters
    object MyDetailScreen : UnifiedRoute("my_detail/{id}") {
        fun createRoute(id: String) = "my_detail/$id"
    }
}
```

### 2. Adding New Routes

#### Step 1: Define the Route

```kotlin
object MyNewFeature : UnifiedRoute("my_new_feature")
```

#### Step 2: Add Composable Definition

```kotlin
composable(UnifiedRoute.MyNewFeature.route) {
    val startTime = remember { System.currentTimeMillis() }
    
    LaunchedEffect(Unit) {
        ComposePerformanceMonitor.trackNavigation("MyNewFeature", startTime)
    }
    
    MyNewFeatureScreen(
        onBackClick = { navController.popBackStack() }
    )
}
```

#### Step 3: Navigate to the Route

```kotlin
// In your screen
Button(onClick = { navController.navigate(UnifiedRoute.MyNewFeature.route) }) {
    Text("Go to My Feature")
}
```

### 3. Performance Monitoring

Always add performance tracking for new routes:

```kotlin
composable(UnifiedRoute.MyRoute.route) {
    val startTime = remember { System.currentTimeMillis() }
    
    LaunchedEffect(Unit) {
        ComposePerformanceMonitor.trackNavigation("MyRoute", startTime)
    }
    
    // Screen content
}
```

### 4. Handling Navigation Arguments

#### Type-Safe Arguments

For routes with parameters, use the createRoute pattern:

```kotlin
// Define route with parameter
object MyDetailScreen : UnifiedRoute("detail/{itemId}") {
    const val ARG_ITEM_ID = "itemId"
    
    fun createRoute(itemId: String): String {
        require(itemId.isNotBlank()) { "itemId cannot be blank" }
        return "detail/$itemId"
    }
}

// Navigate with argument
navController.navigate(UnifiedRoute.MyDetailScreen.createRoute(selectedItemId))

// Extract argument in composable
composable(UnifiedRoute.MyDetailScreen.route) { backStackEntry ->
    val itemId = backStackEntry.arguments?.getString(
        UnifiedRoute.MyDetailScreen.ARG_ITEM_ID
    ) ?: throw IllegalStateException("Missing itemId")
    
    MyDetailScreen(itemId = itemId)
}
```

### 5. Back Stack Management

#### Simple Back Navigation

```kotlin
MyScreen(
    onBackClick = { navController.popBackStack() }
)
```

#### Navigate with PopUp

Use when you want to clear back stack:

```kotlin
navController.navigate(UnifiedRoute.Home.route) {
    popUpTo(UnifiedRoute.Dashboard.route) { inclusive = true }
}
```

### 6. Animation Customization

Use NavigationAnimations for consistent animations:

```kotlin
// Default animations are already configured in UnifiedNavHost

// For custom animations per route
composable(
    route = UnifiedRoute.MyRoute.route,
    enterTransition = { with(NavigationAnimations) { fastSlideInFromRight() } },
    exitTransition = { with(NavigationAnimations) { fastSlideOutToLeft() } }
) {
    // Screen content
}
```

### 7. Error Handling

Handle navigation errors gracefully:

```kotlin
try {
    navController.navigate(UnifiedRoute.MyRoute.route)
} catch (e: Exception) {
    Log.e(TAG, "Navigation failed", e)
    // Show error to user
}
```

### 8. Testing Navigation

Test navigation flows in your unit tests:

```kotlin
@Test
fun testNavigationToDetail() {
    val navController = TestNavHostController(context)
    
    // Trigger navigation
    navController.navigate(UnifiedRoute.MyDetailScreen.createRoute("123"))
    
    // Assert current destination
    assertEquals(
        "detail/123",
        navController.currentBackStackEntry?.destination?.route
    )
}
```

## Common Patterns

### Pattern 1: List to Detail Navigation

```kotlin
// List Screen
LazyColumn {
    items(items) { item ->
        ItemCard(
            item = item,
            onClick = {
                navController.navigate(
                    UnifiedRoute.ItemDetail.createRoute(item.id)
                )
            }
        )
    }
}

// Detail Screen
composable(UnifiedRoute.ItemDetail.route) { backStackEntry ->
    val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
    ItemDetailScreen(
        itemId = itemId,
        onBackClick = { navController.popBackStack() }
    )
}
```

### Pattern 2: Settings Navigation

```kotlin
// Settings Menu
SettingsScreen(
    onAboutClick = { navController.navigate(UnifiedRoute.About.route) },
    onPrivacyClick = { navController.navigate(UnifiedRoute.Privacy.route) }
)
```

### Pattern 3: Bottom Navigation

```kotlin
BottomNavigation {
    items.forEach { item ->
        BottomNavigationItem(
            selected = currentRoute == item.route,
            onClick = {
                navController.navigate(item.route) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = { Icon(item.icon) },
            label = { Text(item.label) }
        )
    }
}
```

## Performance Considerations

### 1. Avoid Heavy Operations in Composables

```kotlin
// Bad - Heavy operation on every recomposition
composable(UnifiedRoute.MyRoute.route) {
    val data = loadHeavyData() // Called on every recomposition
    MyScreen(data)
}

// Good - Use remember and LaunchedEffect
composable(UnifiedRoute.MyRoute.route) {
    val viewModel: MyViewModel = viewModel()
    val data by viewModel.data.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    
    MyScreen(data)
}
```

### 2. Memory Management

Clear resources when navigating away:

```kotlin
composable(UnifiedRoute.MyRoute.route) {
    DisposableEffect(Unit) {
        // Setup
        val resource = acquireResource()
        
        onDispose {
            // Cleanup
            resource.release()
        }
    }
    
    MyScreen()
}
```

### 3. Monitor Navigation Performance

Check navigation metrics regularly:

```kotlin
val summary = ComposePerformanceMonitor.getPerformanceSummary()
summary.forEach { (route, metric) ->
    Log.d("NavPerf", "$route: avg=${metric.average}ms, max=${metric.max}ms")
}
```

## Migration Guide

### Migrating from Activity to Compose Navigation

#### Before (Activity-based)

```kotlin
composable(UnifiedRoute.MyRoute.route) {
    LaunchedEffect(Unit) {
        context.startActivity(Intent(context, MyActivity::class.java))
    }
    LoadingScreen()
}
```

#### After (Compose-based)

```kotlin
composable(UnifiedRoute.MyRoute.route) {
    val startTime = remember { System.currentTimeMillis() }
    
    LaunchedEffect(Unit) {
        ComposePerformanceMonitor.trackNavigation("MyRoute", startTime)
    }
    
    MyScreen(
        onBackClick = { navController.popBackStack() }
    )
}
```

## Troubleshooting

### Issue: Navigation not working

**Solution**: Check that the route is properly defined in UnifiedRoute and the composable is registered in
UnifiedNavHost.

### Issue: Back button not working

**Solution**: Ensure you're passing `onBackClick = { navController.popBackStack() }` to your screen and calling it
appropriately.

### Issue: Arguments not received

**Solution**: Verify that:

1. The route pattern includes the parameter: `"detail/{id}"`
2. You're using the createRoute function to navigate
3. You're extracting arguments correctly in the composable

### Issue: Slow navigation

**Solution**:

1. Check performance metrics using ComposePerformanceMonitor
2. Profile the destination screen for heavy operations
3. Ensure proper use of remember and LaunchedEffect
4. Consider using lazy loading for large datasets

## Resources

- [Navigation Optimization Analysis](./NAVIGATION_OPTIMIZATION_ANALYSIS.md)
- [Compose Navigation Documentation](https://developer.android.com/jetpack/compose/navigation)
- [Performance Monitoring](../app/src/main/java/mpdc4gsr/core/ui/ComposePerformanceMonitor.kt)

## Checklist for Adding Navigation

- [ ] Define route in UnifiedRoute sealed class
- [ ] Add composable in UnifiedNavHost
- [ ] Add performance tracking
- [ ] Handle back navigation
- [ ] Test navigation flow
- [ ] Document any special requirements
- [ ] Update this guide if adding new patterns

---

**Version**: 1.0  
**Last Updated**: 2024-10-01  
**Maintainer**: Development Team
