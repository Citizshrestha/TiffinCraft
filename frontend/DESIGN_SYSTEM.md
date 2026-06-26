# TiffinCraft Design System Quick Reference

## Color Tokens

### Primary Colors
```xml
<color name="green_primary">#2E7D32</color>        <!-- Main brand color, buttons, active states -->
<color name="green_primary_dark">#1B5E20</color>   <!-- Darker variant -->
<color name="green_surface">#E8F5E9</color>        <!-- Light backgrounds, cards -->
<color name="green_brand">#1EAD6F</color>          <!-- Logo, accents -->
```

### Background Colors
```xml
<color name="cream_bg">#FAF6EE</color>             <!-- Main app background -->
<color name="white">#FFFFFFFF</color>              <!-- Cards, pure white surfaces -->
<color name="brown_dark">#3E1F00</color>           <!-- Dark text on cream -->
<color name="orange_warm">#E07B3A</color>          <!-- Accent, highlights -->
```

### Text Colors
```xml
<color name="text_title">#1A1A1A</color>           <!-- Primary headings -->
<color name="text_subtitle">#666666</color>        <!-- Secondary text -->
<color name="text_desc">#666666</color>            <!-- Body text -->
<color name="text_hint">#757575</color>            <!-- Placeholder text -->
```

### UI Elements
```xml
<color name="error_red">#D32F2F</color>            <!-- Errors, validation -->
<color name="input_border">#E0E0E0</color>         <!-- Input field borders -->
<color name="divider_text">#999999</color>         <!-- Dividers, separators -->
<color name="card_subtitle">#999999</color>        <!-- Card secondary text -->
```

## Typography Scale

### Display
- **App Name**: 42sp, bold, green_brand
- **Welcome Title**: 32sp, bold, text_title

### Headings
- **H1**: 30sp, bold, text_title (Auth screens)
- **H2**: 26sp, bold, text_title (Onboarding, SelectRole)
- **H3**: 20-22sp, bold, text_title (Section headers)
- **H4**: 18sp, bold, text_title (Subsections)

### Body
- **Body Large**: 16sp, regular, text_title (Input text)
- **Body Regular**: 15sp, regular, text_subtitle (Links, secondary)
- **Body Small**: 14sp, regular, text_desc (Descriptions)
- **Caption**: 13sp, regular, text_subtitle (Labels)
- **Tiny**: 12sp, regular, text_subtitle (Fine print)

## Spacing System

### Padding/Margin Scale
- **xs**: 4dp
- **sm**: 8dp
- **md**: 12dp
- **base**: 16dp
- **lg**: 24dp
- **xl**: 32dp
- **2xl**: 40dp
- **3xl**: 60dp

### Common Patterns
- **Card Padding**: 16-24dp
- **Screen Padding**: 24dp horizontal, 24-32dp vertical
- **Button Height**: 56dp (optimal touch target)
- **Input Height**: 56dp (with padding 16dp vertical, 12dp horizontal)
- **Bottom Nav Height**: 56dp (default)
- **App Bar Height**: wrap_content with 16dp padding

## Border Radius

### Interactive Elements
- **Buttons**: 16dp
- **Input Fields**: 14dp
- **Cards**: 16-20dp (16dp for content cards, 20dp for major cards)
- **Profile Images**: 24dp (for 48dp size = half)
- **Badges**: 20dp

### Small Elements
- **Dots (indicators)**: oval/circle (10dp size)
- **Small Buttons**: 12dp
- **Chips**: 20dp

## Elevation

### Hierarchy
- **Level 0**: 0dp (flat backgrounds)
- **Level 1**: 2dp (content cards, inputs at rest)
- **Level 2**: 4dp (raised buttons, role cards)
- **Level 3**: 8dp (app bar, bottom nav, floating elements)
- **Level 4**: 16dp (dialogs, modals)

## Icons

### Sizes
- **Small**: 18dp
- **Standard**: 24dp (most icons)
- **Large**: 48dp (profile avatars)
- **Extra Large**: 88dp (logo), 120dp (illustrations)

### Colors
- **Default**: #888888 (gray)
- **Active/Primary**: green_primary
- **Error**: error_red
- **Disabled**: 38% opacity

## Animation Timings

### Micro-interactions
- **Button Press**: 100ms
- **Ripple**: 200ms
- **State Change**: 150ms

### Standard
- **Fade In/Out**: 300-500ms
- **Slide**: 400-600ms
- **Scale**: 400ms

### Entrance Animations
- **Stagger Delay**: 100-150ms between elements
- **Total Duration**: 500-600ms per element

## Touch Targets

### Minimum Sizes (Accessibility)
- **Buttons**: 48x48dp minimum
- **Icons**: 48x48dp touch area (24dp visible)
- **List Items**: 48dp height minimum
- **Bottom Nav Items**: 56dp height

## Component Specifications

### MaterialButton (Primary)
```xml
android:layout_height="56dp"
android:backgroundTint="@color/green_primary"
android:textColor="@color/white"
android:textSize="16sp"
android:textStyle="bold"
android:letterSpacing="0.05"
app:cornerRadius="16dp"
android:elevation="4dp"
```

### TextInputLayout (Outlined)
```xml
style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
app:boxCornerRadius="14dp"
app:boxStrokeWidth="2dp"
app:boxStrokeWidthFocused="2.5dp"
app:boxStrokeColor="@color/green_primary"
app:hintTextColor="@color/green_primary"
app:errorTextColor="@color/error_red"
```

### CardView
```xml
app:cardCornerRadius="16dp"
app:cardElevation="2dp"
app:cardBackgroundColor="@color/white"
android:padding="16dp"
```

### BottomNavigationView
```xml
android:background="@color/white"
app:itemIconTint="@color/bottom_nav_color"
app:itemTextColor="@color/bottom_nav_color"
app:labelVisibilityMode="labeled"
app:elevation="8dp"
```

## Layout Patterns

### Screen Container
```xml
<ScrollView>
    <ConstraintLayout>
        android:paddingStart="24dp"
        android:paddingEnd="24dp"
        android:paddingTop="24dp"
        android:paddingBottom="32dp"
    </ConstraintLayout>
</ScrollView>
```

### Form Section
```xml
<LinearLayout
    android:orientation="vertical"
    android:padding="24dp">
    <!-- Input fields with 14dp marginBottom -->
</LinearLayout>
```

### Stat Card Grid
```xml
<LinearLayout
    android:orientation="horizontal"
    android:layout_marginBottom="12dp">
    <CardView android:layout_weight="1" android:layout_marginEnd="8dp"/>
    <CardView android:layout_weight="1" android:layout_marginStart="8dp"/>
</LinearLayout>
```

## Status Bar Configuration

### Light Theme
```java
getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
```

### Transparent (Splash)
```xml
<item name="android:statusBarColor">@android:color/transparent</item>
<item name="android:windowTranslucentStatus">true</item>
<item name="android:windowLightStatusBar">true</item>
```

## Validation Patterns

### Email
```java
if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
    tilEmail.setError(getString(R.string.error_email_invalid));
}
```

### Phone (10+ digits)
```java
if (phone.replaceAll("[^0-9]", "").length() < 10) {
    tilPhone.setError(getString(R.string.error_phone_invalid));
}
```

### Password (6+ characters)
```java
if (password.length() < 6) {
    tilPassword.setError(getString(R.string.error_password_length));
}
```

## Best Practices

### Do's ✓
- Use Material Design 3 components
- Provide visual feedback for all interactions
- Clear errors when user starts typing
- Show loading states during API calls
- Use proper content descriptions for accessibility
- Implement entrance animations for polish
- Match system theme (light status bar)
- Use elevation hierarchy correctly

### Don'ts ✗
- Don't use hardcoded colors (#RRGGBB in layouts)
- Don't skip input validation
- Don't leave buttons enabled during loading
- Don't ignore back press behavior
- Don't use small touch targets (<48dp)
- Don't mix animation durations randomly
- Don't forget error handling in API calls
- Don't use generic error messages

## Testing Checklist

- [ ] All buttons have press feedback
- [ ] All inputs have validation
- [ ] All errors have clear messages
- [ ] All loading states show feedback
- [ ] All animations are smooth (60fps)
- [ ] All text is readable (sufficient contrast)
- [ ] All touch targets are 48dp+ 
- [ ] All icons have content descriptions
- [ ] All screens handle back press properly
- [ ] All forms clear errors on input
- [ ] All API failures show user-friendly messages
- [ ] All sessions persist correctly
