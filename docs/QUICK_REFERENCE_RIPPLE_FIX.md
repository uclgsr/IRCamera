# Quick Reference: Ripple Crash Fixes

## Import Statement
```kotlin
import mpdc4gsr.core.ui.deferAction
import mpdc4gsr.core.ui.safeClickable
import mpdc4gsr.core.ui.safeClickableNoRipple
import mpdc4gsr.core.ui.safeClickableDeferred
```

## Common Patterns

### IconButton (Back/Navigation)
```kotlin
// Before
IconButton(onClick = { finish() }) { ... }

// After
IconButton(onClick = deferAction { finish() }) { ... }
```

### Dialog Button
```kotlin
// Before
Button(onClick = onDismiss) { ... }

// After  
Button(onClick = deferAction { onDismiss() }) { ... }
```

### Navigation Card
```kotlin
// Before
Card(onClick = { startActivity(...) }) { ... }

// After
Card(onClick = deferAction { startActivity(...) }) { ... }
```

### Custom Clickable
```kotlin
// Before
Modifier.clickable { navigate() }

// After
Modifier.safeClickableDeferred { navigate() }
```

### Inline Pattern (when you can't use deferAction)
```kotlin
val scope = rememberCoroutineScope()
onClick = {
    scope.launch {
        withFrameNanos { }
        yourAction()
    }
}
```

## Decision Tree

```
Is this a navigation action? (finish, startActivity, navigate)
├─ YES → Use deferAction or safeClickableDeferred
│
├─ Is this a dialog dismiss?
│  └─ YES → Use deferAction or withFrameNanos
│
├─ Is this a destructive action?
│  └─ YES → Consider safeClickableNoRipple
│
└─ Regular list/card item?
   └─ YES → Use safeClickable
```

## Remember

- Always defer actions that cause view detachment
- Test on physical devices
- Monitor for ripple crashes in logs
- One-frame delay is usually enough
