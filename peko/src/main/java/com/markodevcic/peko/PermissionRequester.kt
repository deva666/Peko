package com.markodevcic.peko

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.*


interface PermissionRequester {
	fun areGranted(vararg permissions: String): Boolean
	fun flowPermissions(vararg permissions: String): Flow<PermissionResult>

	companion object {
		fun initialize(context: Context) {
			check(context.applicationContext == context) { "Application Context expected as parameter to avoid memory leaks." }
			appContext = context
		}

		private var appContext: Context? = null

		val instance: PermissionRequester
			get() = PekoPermissionRequester()
	}

	private class PekoPermissionRequester : PermissionRequester {

		private val requesterFactory: NativeRequesterFactory = NativeRequesterFactory.defaultFactory


		override fun areGranted(vararg permissions: String): Boolean {
			val context = checkNotNull(appContext) { "App Context is null. Forgot to call the initialize method?" }
			val request = checkPermissions(context, permissions)
			return request.denied.isEmpty()
		}

		override fun flowPermissions(vararg permissions: String): Flow<PermissionResult> {
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
			val permissionsGroup = permissions.groupBy { p -> ContextCompat.checkSelfPermission(context, p) }
			val denied = permissionsGroup[PackageManager.PERMISSION_DENIED] ?: listOf()
			val granted = permissionsGroup[PackageManager.PERMISSION_GRANTED] ?: listOf()
			return PermissionRequest(granted, denied)
		}
	}
}


suspend fun Flow<PermissionResult>.allGranted(): Boolean {
	return this.toList().all { p -> p is PermissionResult.Granted }
}

suspend fun Flow<PermissionResult>.deniedPermissions(): Collection<PermissionResult> {
	return this.filterIsInstance<PermissionResult.Denied>().toSet()
}

suspend fun Flow<PermissionResult>.deniedPermanently(): Collection<PermissionResult> {
	return this.filterIsInstance<PermissionResult.Denied.DeniedPermanently>().toSet()
}

suspend fun Flow<PermissionResult>.needsRationalePermissions(): Collection<PermissionResult> {
	return this.filterIsInstance<PermissionResult.Denied.NeedsRationale>().toSet()
}

suspend fun Flow<PermissionResult>.grantedPermissions(): Collection<PermissionResult> {
	return this.filterIsInstance<PermissionResult.Granted>().toSet()
}
