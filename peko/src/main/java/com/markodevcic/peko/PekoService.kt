package com.markodevcic.peko

import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import com.markodevcic.peko.rationale.PermissionRationale
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

internal class PekoService(private val permissionRequest: PermissionRequest,
						   private val activityReference: WeakReference<Activity>,
						   private val rationale: PermissionRationale,
						   private val sharedPreferences: SharedPreferences) {

	private val pendingPermissions = mutableSetOf<String>()
	private val grantedPermissions = mutableSetOf<String>()
	private val deniedPermissions = mutableSetOf<String>()
	private var requester: PermissionRequester? = null

	fun requestPermissions() {
		val activity = activityReference.get()
		if (activity == null) {
			Peko.clearCurrentRequest()
			return
		}
		pendingPermissions.addAll(permissionRequest.denied)
		if (isTargetSdkUnderAndroidM(activity)) {
			updateDeniedPermissions(pendingPermissions)
		}
		PermissionRequester.startPermissionRequest(activity, object : PermissionRequesterListener {
			override fun onPermissionResult(granted: Collection<String>, denied: Collection<String>) {
				permissionsGranted(granted)
				permissionsDenied(denied)
			}

			override fun onRequesterReady(requester: PermissionRequester) {
				this@PekoService.requester = requester
				requester.requestPermissions(permissionRequest.denied.toTypedArray())
			}
		})
	}

	private fun isTargetSdkUnderAndroidM(activity: Activity): Boolean {
		return try {
			val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
			val targetSdkVersion = info.applicationInfo.targetSdkVersion
			targetSdkVersion < Build.VERSION_CODES.M
		} catch (fail: PackageManager.NameNotFoundException) {
			false
		}
	}

	private fun permissionsGranted(permissions: Collection<String>) {
		pendingPermissions.removeAll(permissions)
		grantedPermissions.addAll(permissions)
		checkIfRequestComplete()
	}

	private fun permissionsDenied(permissions: Collection<String>) {
		val showRationalePermissions = permissions.any { p -> !checkIfRationaleShownAlready(p) }
		if (showRationalePermissions && rationale != PermissionRationale.EMPTY) {
			launch(UI) {
				if (rationale.shouldRequestAfterRationaleShown()) {
					requester?.requestPermissions(permissions.toTypedArray())
				} else {
					updateDeniedPermissions(permissions)
				}
			}
			setRationaleShownFor(permissionRequest.denied)
		} else {
			updateDeniedPermissions(permissions)
		}
	}

	private fun updateDeniedPermissions(permissions: Collection<String>) {
		pendingPermissions.removeAll(permissions)
		deniedPermissions.addAll(permissions)
		checkIfRequestComplete()
	}

	private fun checkIfRequestComplete() {
		if (activityReference.get() == null) {
			Peko.clearCurrentRequest()
			finishRequest()
		} else if (pendingPermissions.isEmpty()) {
			Peko.onPermissionResult(PermissionRequestResult(grantedPermissions, deniedPermissions))
			finishRequest()
		}
	}

	private fun finishRequest() {
		requester?.finish()
		requester = null
	}

	private fun checkIfRationaleShownAlready(permission: String): Boolean {
		val rationaleShowedSet = sharedPreferences.getStringSet(RATIONALE_SHOWED_SET_KEY, setOf())
		return rationaleShowedSet.contains(permission)
	}

	private fun setRationaleShownFor(permissions: Collection<String>) {
		val rationaleShowedSet = sharedPreferences.getStringSet(RATIONALE_SHOWED_SET_KEY, mutableSetOf())
		rationaleShowedSet.addAll(permissions)
		sharedPreferences.edit()
				.putStringSet(RATIONALE_SHOWED_SET_KEY, rationaleShowedSet)
				.apply()
	}
}