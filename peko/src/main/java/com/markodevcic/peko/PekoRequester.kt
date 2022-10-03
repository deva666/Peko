package com.markodevcic.peko

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.*

class PekoRequester {

	private val requesterFactory: PermissionRequesterFactory = PermissionRequesterFactory.defaultFactory

	fun flowPermissions(vararg permissions: String): Flow<PermissionResult> {
		val context = checkNotNull(appContext) { "App Context is null. Forgot to call the initialize method?" }
		if (isTargetSdkUnderAndroidM(context)) {
			return permissions.map { p -> PermissionResult.Granted(p) }.asFlow()
		}
		val request = checkPermissions(context, permissions)

		val flow = callbackFlow {
			val requester = requesterFactory.getRequesterAsync(context).await()
			for (granted in request.granted) {
				trySend(PermissionResult.Granted(granted))
			}
			if (request.denied.isNotEmpty()) {
				requester.requestPermissions(request.denied.toTypedArray())
				for (result in requester.resultsChannel) {
					send(result)
				}
				requester.finish()
				channel.close()
			} else {
				requester.finish()
				channel.close()
			}
		}
		return flow
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
		return PermissionRequest(granted,listOf(), denied)
	}

	companion object {
		fun initialize(context: Context) {
			appContext = context
		}

		private var appContext: Context? = null
	}
}