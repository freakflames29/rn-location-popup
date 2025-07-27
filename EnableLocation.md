# How to Add a Native Location Enabler to a React Native App (For Android)

This guide provides a detailed, step-by-step process for creating a native Android module that can:
1.  Check if the device's location services are currently enabled.
2.  Prompt the user with a native Android popup to enable location services if they are off.

This guide is designed to be easy to follow, even for those new to React Native or native Android development.

---

## Step 1: Create the Native Module Files

These files contain the actual Android code that will run on the user's device.

### File 1: The Core Logic (`LocationEnablerModule.kt`)

This file contains the main logic for checking the location status and showing the popup.

1.  **Navigate** to `android/app/src/main/java/com/your-app-name/`.
    > **Tip:** Your app name is the package name you see in other files in this directory, like `MainActivity.kt`.
2.  **Create a new file** named `LocationEnablerModule.kt`.
3.  **Copy and paste** the following code into the new file.

    ```kotlin
    package com.kolkata // IMPORTANT: Change "com.kolkata" to your app's package name!

    import android.app.Activity
    import android.content.IntentSender
    import com.facebook.react.bridge.*
    import com.google.android.gms.common.api.ResolvableApiException
    import com.google.android.gms.location.*
    import android.app.Activity.RESULT_OK

    class LocationEnablerModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

        private var promise: Promise? = null

        override fun getName() = "LocationEnabler"

        @ReactMethod
        fun isLocationEnabled(promise: Promise) {
            val activity: Activity = currentActivity ?: run {
                promise.reject("NO_ACTIVITY", "No foreground activity!")
                return
            }

            val locationRequest = LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .build()

            val builder = LocationSettingsRequest
                .Builder()
                .addLocationRequest(locationRequest)

            val settingsClient = LocationServices.getSettingsClient(activity)
            val task = settingsClient.checkLocationSettings(builder.build())

            task.addOnSuccessListener {
                promise.resolve(true) // Location is on
            }
            task.addOnFailureListener { ex ->
                promise.resolve(false) // Location is off
            }
        }

        @ReactMethod
        fun promptForEnableLocation(promise: Promise) {
            val activity: Activity = currentActivity ?: run {
                promise.reject("NO_ACTIVITY", "No foreground activity!")
                return
            }
            this.promise = promise

            val locationRequest = LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .build()

            val builder = LocationSettingsRequest
                .Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true) // This is what forces the popup

            val settingsClient = LocationServices.getSettingsClient(activity)
            val task = settingsClient.checkLocationSettings(builder.build())

            task.addOnSuccessListener {
                promise.resolve("already-enabled")
            }
            task.addOnFailureListener { ex ->
                if (ex is ResolvableApiException) {
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ex.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        promise.reject("SEND_INTENT_EXCEPTION", sendEx)
                    }
                } else {
                    promise.reject("UNRESOLVABLE", ex)
                }
            }
        }

        fun onActivityResult(requestCode: Int, resultCode: Int) {
            if (requestCode == REQUEST_CHECK_SETTINGS) {
                if (resultCode == RESULT_OK) {
                    promise?.resolve("enabled")
                } else {
                    promise?.reject("CANCELLED", "User cancelled the request")
                }
            }
        }

        companion object {
            const val REQUEST_CHECK_SETTINGS = 999
        }
    }
    ```

### File 2: The Module "Package" (`LocationEnablerPackage.kt`)

This file registers the module so React Native can find and use it.

1.  In the **same directory** as the previous file, **create a new file** named `LocationEnablerPackage.kt`.
2.  **Copy and paste** the following code.

    ```kotlin
    package com.kolkata // IMPORTANT: Change "com.kolkata" to your app's package name!

    import com.facebook.react.ReactPackage
    import com.facebook.react.bridge.NativeModule
    import com.facebook.react.bridge.ReactApplicationContext
    import com.facebook.react.uimanager.ViewManager

    class LocationEnablerPackage : ReactPackage {
        override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
            return listOf(LocationEnablerModule(reactContext))
        }

        override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
            return emptyList()
        }
    }
    ```

---

## Step 2: Register the Module with Your App

Now, you need to tell your Android app to load the new module package.

### File 3: Your Application Class (`MainApplication.kt`)

1.  **Open** the `MainApplication.kt` file, located in the same directory.
2.  **Find** the `getPackages()` function.
3.  **Add** `add(LocationEnablerPackage())` to the list of packages.

    ```kotlin
    // ... other imports
    import com.kolkata.LocationEnablerPackage // <-- 1. Add this import

    class MainApplication : Application(), ReactApplication {

      override val reactNativeHost: ReactNativeHost =
          object : DefaultReactNativeHost(this) {
            override fun getPackages(): List<ReactPackage> =
                PackageList(this).packages.apply {
                  // ...
                  add(LocationEnablerPackage()) // <-- 2. Add this line
                }

            // ... rest of the file
          }
    }
    ```

### File 4: Your Main Activity (`MainActivity.kt`)

This change allows your module to receive the result from the location popup (i.e., whether the user tapped "Yes" or "No").

1.  **Open** the `MainActivity.kt` file.
2.  **Add** the `onActivityResult` function to the class.

    ```kotlin
    package com.kolkata // Your package name

    // ... other imports
    import android.content.Intent // <-- 1. Add this import
    import com.facebook.react.bridge.ReactContext // <-- 2. Add this import

    class MainActivity : ReactActivity() {

      // ... (existing code like getMainComponentName and createReactActivityDelegate)

      // vvv 3. Add this entire function vvv
      override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val reactContext: ReactContext? = reactNativeHost.reactInstanceManager.currentReactContext
        if (reactContext != null) {
            val module = reactContext.getNativeModule(LocationEnablerModule::class.java)
            module?.onActivityResult(requestCode, resultCode)
        }
      }
    }
    ```

---

## Step 3: Configure Gradle (Android's Build System)

These changes tell Android's build system to download the necessary libraries.

### File 5: Project-Level `build.gradle`

1.  **Open** the file at `android/build.gradle`.
2.  **Ensure** you have a `kotlinVersion` defined and the `kotlin-gradle-plugin` in your dependencies.

    ```groovy
    buildscript {
        ext {
            // ...
            kotlinVersion = "2.1.20" // Or a recent version
        }
        // ...
        dependencies {
            // ...
            classpath("org.jetbrains.kotlin:kotlin-gradle-plugin") // <-- Add this if it's missing
        }
    }
    ```

### File 6: App-Level `build.gradle`

1.  **Open** the file at `android/app/build.gradle`.
2.  **Add** the Google Play Services location library.

    ```groovy
    dependencies {
        // ... (other dependencies like react-android)
        implementation 'com.google.android.gms:play-services-location:21.0.1' // <-- Add this line
    }
    ```

### File 7: `settings.gradle`

1.  **Open** the file at `android/settings.gradle`.
2.  **Update** the `dependencyResolutionManagement` block. Using `PREFER_SETTINGS` helps avoid common build errors with React Native.

    ```groovy
    dependencyResolutionManagement {
        // vvv Use PREFER_SETTINGS to avoid build issues vvv
        repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
        repositories {
            google()
            mavenCentral()
            maven { url 'https://www.jitpack.io' }
        }
    }
    ```

---

## Step 4: Create the TypeScript Bridge

This file makes the native module easy to use from your JavaScript/TypeScript code.

### File 8: The Bridge (`LocationEnabler.ts`)

1.  In your project's `src` folder, **create a new file** named `LocationEnabler.ts`.
2.  **Copy and paste** the following code.

    ```typescript
    import { NativeModules } from 'react-native';

    const { LocationEnabler } = NativeModules;

    // This defines the functions our native module has
    interface LocationEnablerInterface {
      isLocationEnabled(): Promise<boolean>;
      promptForEnableLocation(): Promise<string>;
    }

    export default LocationEnabler as LocationEnablerInterface;
    ```

---

## Step 5: Use the Module in Your App!

Now you can use the module in any of your React Native components.

### File 9: Example Usage (`App.tsx`)

Here is a complete example of how to use your new module.

```typescript
import React, { useEffect, useState } from 'react';
import { View, Text, Button, StyleSheet, Platform } from 'react-native';
import LocationEnabler from './src/LocationEnabler'; // <-- Import the bridge

const App = () => {
  const [locationStatus, setLocationStatus] = useState<string>('Checking...');

  // Function to check the location status
  const checkLocationStatus = async () => {
    if (Platform.OS === 'android') { // Only run on Android
      try {
        const isEnabled = await LocationEnabler.isLocationEnabled();
        setLocationStatus(isEnabled ? 'On' : 'Off');
      } catch (e) {
        console.error(e);
        setLocationStatus('Error');
      }
    }
  };

  // Function to prompt the user to enable location
  const enableLocation = async () => {
    if (Platform.OS === 'android') {
      try {
        const result = await LocationEnabler.promptForEnableLocation();
        console.log('Prompt result:', result); // "already-enabled", "enabled", or an error
        // Re-check the status after the user has made a choice
        await checkLocationStatus();
      } catch (e) {
        console.error(e);
      }
    }
  };

  // Check the location status when the component first loads
  useEffect(() => {
    checkLocationStatus();
  }, []);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Location Status</Text>
      <Text style={styles.status}>{locationStatus}</Text>
      {locationStatus === 'Off' && (
        <Button title="Enable Location" onPress={enableLocation} />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  title: {
    fontSize: 22,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  status: {
    fontSize: 18,
    marginBottom: 20,
  },
});

export default App;
```

And that's it! You now have a reusable native module for handling location services in your Android app.