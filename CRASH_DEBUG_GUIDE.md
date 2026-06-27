# TiffinCraft App Crash Debugging Guide

## Issue: App crashes after login with "Welcome" message

### What We've Fixed

1. ✅ **Added proper error handling** in both CustomerHomeActivity and CookHomeActivity
2. ✅ **Fixed deprecated `onBackPressed()`** - Replaced with `OnBackPressedCallback`
3. ✅ **Added missing imports** - Added `Log` and `OnBackPressedCallback` imports
4. ✅ **Created missing color resource** - Added `bottom_nav_color.xml` selector
5. ✅ **Added try-catch blocks** around onCreate and view initialization

### How to Debug the Crash

#### Step 1: Clean and Rebuild
```bash
cd frontend
./gradlew clean
./gradlew build
```

Or in Android Studio:
- **Build > Clean Project**
- **Build > Rebuild Project**

#### Step 2: Check Logcat for Exact Error

When you run the app and it crashes, immediately check **Logcat** in Android Studio:

1. Open **Logcat** tab (bottom of Android Studio)
2. Filter by your package: `com.tiffincraft.app`
3. Look for lines with:
   - `E/` (Error logs in RED)
   - `CustomerHomeActivity` or `CookHomeActivity` tags
   - Stack traces starting with `java.lang.RuntimeException` or `NullPointerException`

#### Step 3: Common Crash Causes and Solutions

##### 1. **Missing Drawable Resources**
**Error:** `Resources$NotFoundException: Drawable resource ID #0x...`

**Solution:** Check if all drawable files referenced in layouts exist:
```bash
cd frontend/app/src/main/res/drawable
ls -la
```

Missing drawables to check:
- `ic_notifications.xml`
- `ic_search.xml`
- `ic_filter.xml`
- `hero_customer_banner.png`
- Any icon referenced in layouts

##### 2. **Missing Layout Views**
**Error:** `NullPointerException` when calling methods on views

**Solution:** Check that all `findViewById()` IDs match the layout XML:
- `bottomNavigation`
- `fabCart`
- `tvGreeting`
- `searchBar`

Our fix: Added null checks and logging for missing views.

##### 3. **Bottom Navigation Menu Issues**
**Error:** Crash when setting up bottom navigation

**Solution:** Verify menu files exist:
- ✅ `menu/bottom_nav_customer.xml`
- ✅ `menu/bottom_nav_cook.xml`

##### 4. **Color Resource Missing**
**Error:** `Resources$NotFoundException: Color resource ID`

**Solution:** We created `color/bottom_nav_color.xml` - should be fixed now.

##### 5. **Activity Not in Manifest**
**Error:** `ActivityNotFoundException`

**Solution:** Verify in `AndroidManifest.xml`:
```xml
<activity
    android:name=".activities.customer.CustomerHomeActivity"
    android:theme="@style/Theme.TiffinCraft.NoActionBar"/>

<activity
    android:name=".activities.cook.CookHomeActivity"
    android:theme="@style/Theme.TiffinCraft.NoActionBar"/>
```
✅ Already present in manifest

### Step 4: Run with Enhanced Logging

Our changes added debug logging:
- `CustomerHomeActivity onCreate completed successfully` - If you see this, onCreate worked
- `CookHomeActivity onCreate completed successfully` - If you see this, onCreate worked

**If you DON'T see these logs**, the crash happens during onCreate.
**If you DO see these logs**, the crash happens after onCreate (likely in navigation or animations).

### Step 5: Test Backend Connection

Ensure your backend is running:
```bash
cd backend
node server.js
```

Should show:
```
Server running on port 5000
```

### Expected Logcat Output (Success)

```
D/LoginActivity: Login successful
D/SessionManager: Session created for user...
D/CustomerHomeActivity: CustomerHomeActivity onCreate completed successfully
```

### Expected Logcat Output (Crash)

```
E/AndroidRuntime: FATAL EXCEPTION: main
E/AndroidRuntime: Process: com.tiffincraft.app, PID: xxxxx
E/AndroidRuntime: java.lang.RuntimeException: Unable to start activity...
E/AndroidRuntime: Caused by: [ACTUAL ERROR HERE]
```

### Quick Test Commands

#### Option 1: Run via Android Studio
1. Click **Run** (green play button)
2. Select your device/emulator
3. Login and observe Logcat

#### Option 2: Run via Command Line
```bash
cd frontend
./gradlew installDebug
adb logcat -s "TiffinCraft:*" "AndroidRuntime:E"
```

### If Crash Persists

**Copy the FULL error from Logcat** and share it. Look for:
1. The exception type (e.g., `NullPointerException`, `ResourceNotFoundException`)
2. The exact line number where it crashes
3. The stack trace showing which method caused the issue

Example:
```
E/AndroidRuntime: java.lang.NullPointerException: Attempt to invoke virtual method 
    'void android.widget.TextView.setText(...)' on a null object reference
    at com.tiffincraft.app.activities.customer.CustomerHomeActivity.loadUserData(CustomerHomeActivity.java:87)
```

This tells us:
- **Problem:** TextView is null
- **Location:** `loadUserData()` method, line 87
- **Fix:** Check that `findViewById()` successfully found the view

### Testing Checklist

- [ ] Backend server is running on port 5000
- [ ] App builds without errors
- [ ] Can reach login screen
- [ ] Login shows "Welcome" toast
- [ ] Check Logcat immediately after crash
- [ ] Copy full error message from Logcat
- [ ] Verify which activity you're logging into (customer vs cook)

### Contact Points

If you need help:
1. **Copy the full Logcat error**
2. **Note which user role you're testing** (customer or cook)
3. **Share the error here for analysis**

---

## Recent Changes Summary

### Files Modified:
1. `CookHomeActivity.java` - Added error handling, fixed deprecated methods
2. `CustomerHomeActivity.java` - Added error handling, fixed deprecated methods
3. `color/bottom_nav_color.xml` - Created missing color selector

### Changes Made:
- ✅ Added try-catch in onCreate methods
- ✅ Added logging for debugging
- ✅ Fixed deprecated onBackPressed() method
- ✅ Added null checks for views
- ✅ Created missing color resource for bottom navigation
- ✅ Added missing imports (Log, OnBackPressedCallback)

**Next Step:** Clean, rebuild, and run the app while monitoring Logcat.
