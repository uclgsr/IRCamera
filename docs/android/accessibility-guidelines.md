# Accessibility Guidelines for IRCamera

## Overview

Accessibility ensures that the IRCamera app is usable by everyone, including people with disabilities. This document outlines the requirements and best practices for implementing accessible features.

## Core Requirements

### 1. Content Descriptions

All interactive elements and informative images must have meaningful content descriptions for screen readers like TalkBack.

**XML:**
```xml
<ImageButton
    android:id="@+id/record_button"
    android:contentDescription="@string/start_recording"
    ... />
```

**Compose:**
```kotlin
IconButton(
    onClick = { startRecording() },
    modifier = Modifier.semantics {
        contentDescription = "Start recording"
        role = Role.Button
    }
) {
    Icon(Icons.Default.RadioButtonChecked, contentDescription = null)
}
```

**Guidelines:**
- Decorative images: `contentDescription = null` or empty string
- Informative images: Describe the content/function
- Interactive elements: Describe the action that will occur
- Avoid redundant descriptions (e.g., don't say "button" if it's already a button)

### 2. Touch Target Size

Minimum touch target size: 48dp x 48dp (recommended: 56dp x 56dp for better accessibility)

**Compose:**
```kotlin
Button(
    onClick = { /* action */ },
    modifier = Modifier
        .minimumInteractiveComponentSize()
        .size(width = 100.dp, height = 48.dp)
) {
    Text("Record")
}
```

**XML:**
```xml
<Button
    android:minWidth="48dp"
    android:minHeight="48dp"
    ... />
```

### 3. Color Contrast

Minimum contrast ratios (WCAG 2.1 Level AA):
- Normal text: 4.5:1
- Large text (18pt+): 3:1
- UI components and graphics: 3:1

**Tools:**
- Android Studio Layout Inspector with Accessibility Scanner
- WebAIM Contrast Checker: https://webaim.org/resources/contrastchecker/

**Guidelines:**
- Don't rely on color alone to convey information
- Use icons, labels, or patterns in addition to color
- Test with Color Blindness simulators

### 4. Dynamic Type Support

Support user-defined text sizes (System Settings > Display > Font size)

**Compose:**
```kotlin
Text(
    text = "Recording duration: 00:15",
    style = MaterialTheme.typography.bodyLarge,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
)
```

**XML:**
```xml
<TextView
    android:textSize="16sp"
    android:maxLines="2"
    android:ellipsize="end"
    ... />
```

**Guidelines:**
- Use `sp` for text sizes (scales with user preferences)
- Use `dp` for layout dimensions
- Test with largest text size setting
- Allow text to wrap or truncate gracefully

### 5. Focus Order

Ensure logical focus order for keyboard and switch access navigation.

**Compose:**
```kotlin
Column(
    modifier = Modifier.semantics {
        isTraversalGroup = true
    }
) {
    // Elements will be traversed in order
    Button(...) // First
    Button(...) // Second
    Button(...) // Third
}
```

**XML:**
```xml
<LinearLayout
    android:focusable="true"
    android:focusableInTouchMode="false">
    <!-- Elements are focused in order -->
</LinearLayout>
```

### 6. Semantic Grouping

Group related content for screen readers.

**Compose:**
```kotlin
Row(
    modifier = Modifier.semantics(mergeDescendants = true) {
        contentDescription = "Recording: 5 minutes, 23 seconds"
    }
) {
    Icon(Icons.Default.FiberManualRecord, contentDescription = null)
    Text("5:23")
}
```

## Implementation Checklist

### For All Screens

- [ ] All interactive elements have appropriate content descriptions
- [ ] Touch targets are at least 48dp x 48dp
- [ ] Color contrast meets WCAG AA standards
- [ ] Text is readable at largest system font size
- [ ] Focus order is logical (left-to-right, top-to-bottom)
- [ ] No time-limited content (or provide extensions)
- [ ] Error messages are announced by screen readers

### For Camera/Recording Screens

- [ ] Recording status is announced (e.g., "Recording started")
- [ ] Timer updates are not too frequent (announce every 5-10 seconds or on demand)
- [ ] Camera controls have clear labels
- [ ] Preview image has description of what's being captured
- [ ] Recording indicators have both visual and non-visual cues

### For Settings Screens

- [ ] All settings have labels and descriptions
- [ ] Toggle states are announced (e.g., "Bluetooth, on")
- [ ] Slider values are announced as they change
- [ ] Section headers are properly marked

### For List/Grid Views

- [ ] List items have descriptive content descriptions
- [ ] Current position is announced (e.g., "Item 3 of 10")
- [ ] Actions on items are clearly labeled

## Testing Accessibility

### Manual Testing

1. **Enable TalkBack** (Settings > Accessibility > TalkBack)
   - Navigate through the app using swipe gestures
   - Ensure all content is announced clearly
   - Test all interactive elements

2. **Test with Large Text**
   - Set font size to largest (Settings > Display > Font size)
   - Ensure all text is readable and doesn't overflow

3. **Test Color Contrast**
   - Use Accessibility Scanner app
   - Test with grayscale mode
   - Test with color blindness simulators

4. **Test Touch Targets**
   - Use Accessibility Scanner to identify small touch targets
   - Manually verify comfortable tap areas

5. **Test Focus Navigation**
   - Connect keyboard and navigate using Tab key
   - Use Switch Access for testing alternative inputs

### Automated Testing

**Compose UI Testing:**
```kotlin
@Test
fun testButtonAccessibility() {
    composeTestRule.onNodeWithContentDescription("Start recording")
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()
}
```

**Espresso Testing:**
```kotlin
@Test
fun testRecordButtonAccessibility() {
    onView(withContentDescription("Start recording"))
        .check(matches(isDisplayed()))
        .perform(click())
}
```

## Accessibility Scanner Integration

Add Accessibility Scanner to debug builds for automated accessibility checks:

```kotlin
if (BuildConfig.DEBUG) {
    // Accessibility Scanner can be enabled from settings
    // https://developer.android.com/guide/topics/ui/accessibility/testing
}
```

## Screen Reader Announcements

### Live Announcements

For important status changes:

**Compose:**
```kotlin
val view = LocalView.current
LaunchedEffect(isRecording) {
    if (isRecording) {
        view.announceForAccessibility("Recording started")
    } else {
        view.announceForAccessibility("Recording stopped")
    }
}
```

**Traditional View:**
```kotlin
view.announceForAccessibility("Recording started")
```

### Custom Actions

Add custom actions for accessibility services:

**Compose:**
```kotlin
Card(
    modifier = Modifier.semantics {
        customActions = listOf(
            CustomAccessibilityAction("Delete") {
                deleteItem()
                true
            },
            CustomAccessibilityAction("Share") {
                shareItem()
                true
            }
        )
    }
) {
    // Content
}
```

## Common Patterns

### Loading States
```kotlin
if (isLoading) {
    CircularProgressIndicator(
        modifier = Modifier.semantics {
            contentDescription = "Loading recordings"
        }
    )
}
```

### Error States
```kotlin
if (error != null) {
    Text(
        text = error,
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
        }
    )
}
```

### Expandable Content
```kotlin
var expanded by remember { mutableStateOf(false) }

Column(
    modifier = Modifier.semantics {
        stateDescription = if (expanded) "Expanded" else "Collapsed"
    }
) {
    Button(
        onClick = { expanded = !expanded },
        modifier = Modifier.semantics {
            contentDescription = if (expanded) {
                "Collapse details"
            } else {
                "Expand details"
            }
        }
    ) {
        Text("Details")
    }
    
    if (expanded) {
        Text("Detailed information...")
    }
}
```

## Resources

### Tools
- [Accessibility Scanner](https://play.google.com/store/apps/details?id=com.google.android.apps.accessibility.auditor)
- [TalkBack Screen Reader](https://support.google.com/accessibility/android/answer/6283677)
- [Color Contrast Checker](https://webaim.org/resources/contrastchecker/)

### Documentation
- [Android Accessibility Overview](https://developer.android.com/guide/topics/ui/accessibility)
- [Compose Accessibility](https://developer.android.com/jetpack/compose/accessibility)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)

### Testing
- [Accessibility Testing Guide](https://developer.android.com/guide/topics/ui/accessibility/testing)
- [Espresso Accessibility Testing](https://developer.android.com/training/testing/espresso/accessibility-checking)

## Priority Implementation

### High Priority (Week 1-2)
1. Add content descriptions to all buttons and interactive elements
2. Ensure minimum touch target sizes
3. Test basic TalkBack navigation

### Medium Priority (Week 3-4)
4. Verify color contrast ratios
5. Test with large text sizes
6. Add semantic grouping for complex UI

### Low Priority (Ongoing)
7. Custom accessibility actions for advanced features
8. Haptic feedback for touch interactions
9. Sound effects for important actions (with option to disable)

## Maintenance

- Run Accessibility Scanner monthly
- Test with TalkBack before each release
- Include accessibility in code review checklist
- Gather feedback from users with disabilities

---

**Last Updated:** 2024
**Status:** Living document - updated as new accessibility features are added
