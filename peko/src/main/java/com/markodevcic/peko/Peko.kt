package com.markodevcic.peko

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import java.lang.ref.WeakReference

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
object Peko {

	private var currentContinuation: CancellableContinuation<PermissionRequestResult>? = null
	private var service: PekoService? = null

	suspend fun requestPermissions(activity: Activity,
								   vararg permissions: String,
								   rationale: PermissionRationale = PermissionRationale.EMPTY): PermissionRequestResult {
		return suspendCancellableCoroutine { continuation ->
			checkRequestNotInProgress()
			currentContinuation = continuation
			val request = checkPermissions(activity, permissions)
			if (request.denied.isNotEmpty()) {
				service = PekoService(request, WeakReference(activity), rationale, activity.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE))
				service?.requestPermissions()
			} else {
				onPermissionResult(PermissionRequestResult(request.granted, request.denied))
			}
		}

	}

	private fun checkRequestNotInProgress() {
		if (service != null) {
			throw IllegalStateException("Can't request permission while another request in progress")
		}
	}

	private fun checkPermissions(context: Context, permissions: Array<out String>): PermissionRequest {
		val permissionsGroup = permissions.groupBy { p -> ActivityCompat.checkSelfPermission(context, p) }
		val denied = permissionsGroup[PackageManager.PERMISSION_DENIED] ?: listOf()
		val granted = permissionsGroup[PackageManager.PERMISSION_GRANTED] ?: listOf()
		return PermissionRequest(granted, denied)
	}

	internal fun onPermissionResult(result: PermissionRequestResult) {
		currentContinuation?.resume(result)
		clearCurrentRequest()
	}

	internal fun clearCurrentRequest() {
		currentContinuation = null
		service = null
	}
}