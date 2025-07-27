package com.rnlocationpopup

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

@ReactModule(name = RnLocationPopupModule.NAME)
class RnLocationPopupModule(reactContext: ReactApplicationContext) :
  NativeRnLocationPopupSpec(reactContext), ActivityEventListener {

  private var promise: Promise? = null

  init {
    reactContext.addActivityEventListener(this)
  }

  override fun getName(): String {
    return NAME
  }

  override fun isLocationEnabled(promise: Promise) {
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

  override fun promptForEnableLocation(promise: Promise) {
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

  override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_CHECK_SETTINGS) {
      if (resultCode == Activity.RESULT_OK) {
        promise?.resolve("enabled")
      } else {
        promise?.reject("CANCELLED", "User cancelled the request")
      }
    }
  }

  override fun onNewIntent(intent: Intent?) {}

  companion object {
    const val NAME = "RnLocationPopup"
    const val REQUEST_CHECK_SETTINGS = 999
  }
}