package com.markodevcic.peko

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import com.markodevcic.peko.rationale.PermissionRationale
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import java.util.concurrent.atomic.AtomicReference

object Peko {

	private val serviceReference = AtomicReference<PekoService?>(null)

	fun requestPermissionsAsync(activity: Activity,
								vararg permissions: String,
								rationale: PermissionRationale = PermissionRationale.EMPTY): Deferred<PermissionRequestResult> {

		val request = checkPermissions(activity, permissions)
		if (isTargetSdkUnderAndroidM(activity)) {
			return CompletableDeferred(PermissionRequestResult(listOf(), permissions.toList()))
		}

		return if (request.denied.isNotEmpty()) {
			val service = PekoService(activity, request, rationale,
					activity.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE))

			if (!serviceReference.compareAndSet(null, service)) {
				throw IllegalStateException("Can't request permission while another request in progress")
			}

			service.requestPermissions().apply {
				invokeOnCompletion {
					serviceReference.set(null)
				}
			}
		} else {
			CompletableDeferred(PermissionRequestResult(request.granted, request.denied))
		}
	}

	private fun isTargetSdkUnderAndroidM(context: Context): Boolean {
		return try {
			val info = context.packageManager.getPackageInfo(context.packageName, 0)
			val targetSdkVersion = info.applicationInfo.targetSdkVersion
			targetSdkVersion < Build.VERSION_CODES.M
		} catch (fail: PackageManager.NameNotFoundException) {
			false
		}
	}

	private fun checkPermissions(context: Context, permissions: Array<out String>): PermissionRequest {
		val permissionsGroup = permissions.groupBy { p -> ActivityCompat.checkSelfPermission(context, p) }
		val denied = permissionsGroup[PackageManager.PERMISSION_DENIED] ?: listOf()
		val granted = permissionsGroup[PackageManager.PERMISSION_GRANTED] ?: listOf()
		return PermissionRequest(granted, denied)
	}
}