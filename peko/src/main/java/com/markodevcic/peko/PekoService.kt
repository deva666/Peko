package com.markodevcic.peko

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import com.markodevcic.peko.rationale.PermissionRationale
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference

internal class PekoService(context: Context,
						   private val permissionRequest: PermissionRequest,
						   private val rationale: PermissionRationale,
						   private val sharedPreferences: SharedPreferences,
						   private val requesterFactory: PermissionRequesterFactory = PermissionRequesterFactory.defaultFactory,
						   private val dispatcher: CoroutineDispatcher = UI) {

	private val pendingPermissions = mutableSetOf<String>()
	private val grantedPermissions = mutableSetOf<String>()
	private val deniedPermissions = mutableSetOf<String>()
	private val contextReference: WeakReference<out Context> = WeakReference(context)
	private val job = Job()

	private lateinit var requester: PermissionRequester

	fun requestPermissions() {
		val context = contextReference.get()
		if (context == null) {
			Peko.clearCurrentRequest()
			return
		}
		pendingPermissions.addAll(permissionRequest.denied)
		grantedPermissions.addAll(permissionRequest.granted)
		if (isTargetSdkUnderAndroidM(context)) {
			updateDeniedPermissions(pendingPermissions)
		} else {
			launch(job + dispatcher) {
				requester = requesterFactory.getRequester(context).await()
				requester.requestPermissions(permissionRequest.denied.toTypedArray())
				for (result in requester.resultsChannel) {
					permissionsGranted(result.grantedPermissions)
					permissionsDenied(result.deniedPermissions)
				}
			}
		}
	}

	fun cancelRequest() {
		job.cancel()
	}

	private fun isTargetSdkUnderAndroidM(activity: Context): Boolean {
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
				if (rationale.shouldRequestAfterRationaleShownAsync()) {
					requester.requestPermissions(permissions.toTypedArray())
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
		if (contextReference.get() == null) {
			Peko.clearCurrentRequest()
			finishRequest()
		} else if (pendingPermissions.isEmpty()) {
			Peko.onPermissionResult(PermissionRequestResult(grantedPermissions, deniedPermissions))
			finishRequest()
		}
	}

	private fun finishRequest() {
		requester.finish()
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