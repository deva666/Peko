package com.markodevcic.peko

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import com.markodevcic.peko.rationale.PermissionRationale
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import java.lang.ref.WeakReference

object Peko {

	private var service: PekoService? = null
	private var deferred: CompletableDeferred<PermissionRequestResult>? = null

	fun requestPermissions(activity: Activity,
						   vararg permissions: String,
						   rationale: PermissionRationale = PermissionRationale.EMPTY): Deferred<PermissionRequestResult> {

		checkRequestNotInProgress()
		deferred = CompletableDeferred()
		val request = checkPermissions(activity, permissions)
		return if (request.denied.isNotEmpty()) {
			service = PekoService(request, WeakReference(activity), rationale,
					activity.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE))
			service?.requestPermissions()
			deferred!!
		} else {
			deferred?.apply {
				complete(PermissionRequestResult(request.granted, request.denied))
			}!!
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
		if (deferred?.isActive == true) {
			deferred?.complete(result)
		}
		clearCurrentRequest()
	}

	internal fun clearCurrentRequest() {
		deferred = null
		service = null
	}
}