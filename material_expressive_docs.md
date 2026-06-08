# MaterialExpressiveTheme Official API Reference Documentation

**Artifact:** `androidx.compose.material3:material3`  
**Added in:** `1.5.0-alpha21`

---

## Signature
```kotlin
@Composable
fun MaterialExpressiveTheme(    
    colorScheme: ColorScheme? = null,    
    motionScheme: MotionScheme? = null,    
    shapes: Shapes? = null,    
    typography: Typography? = null,    
    content: @Composable () -> Unit
): Unit
```

---

## Description
Material Expressive Theming refers to the customization of your Material Design app to better reflect your product’s brand.

Material components such as `Button` and `Checkbox` use values provided here when retrieving default values.

All values may be set by providing this component with the `colorScheme`, `typography`, and `shapes` attributes. Use this to configure the overall theme of elements within this `MaterialTheme`.

Any values that are not set will fall back to the defaults. To inherit the current value from the theme, pass them into subsequent calls and override only the parts of the theme definition that need to change.

Alternatively, only call this function at the top of your application, and then call `MaterialTheme` to specify separate `MaterialTheme`(s) for different screens or parts of your UI, overriding only the parts of the theme definition that need to change.

---

## Parameters

* **`colorScheme: ColorScheme? = null`**
  A complete definition of the Material Color theme for this hierarchy.
* **`motionScheme: MotionScheme? = null`**
  A complete definition of the Material motion theme for this hierarchy.
* **`shapes: Shapes? = null`**
  A set of corner shapes to be used as this hierarchy's shape system.
* **`typography: Typography? = null`**
  A set of text styles to be used as this hierarchy's typography system.
* **`content: @Composable () -> Unit`**
  The content inheriting this theme.

---

## Code Example
```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val isDarkTheme = isSystemInDarkTheme()
val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

val darkColorScheme = darkColorScheme(primary = Color(0xFF66ffc7))

val colorScheme =
    when {
        supportsDynamicColor && isDarkTheme -> {
            dynamicDarkColorScheme(LocalContext.current)
        }
        supportsDynamicColor && !isDarkTheme -> {
            dynamicLightColorScheme(LocalContext.current)
        }
        isDarkTheme -> darkColorScheme
        else -> expressiveLightColorScheme()
    }

val shapes = Shapes(largeIncreased = RoundedCornerShape(36.0.dp))

MaterialExpressiveTheme(colorScheme = colorScheme, shapes = shapes) {
    val currentTheme = if (!isSystemInDarkTheme()) "light" else "dark"
    ExtendedFloatingActionButton(
        text = { Text("FAB with text style and color from $currentTheme expressive theme") },
        icon = { Icon(Icons.Filled.Favorite, contentDescription = "Localized Description") },
        onClick = {},
    )
}
```
