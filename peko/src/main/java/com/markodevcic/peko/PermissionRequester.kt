package com.markodevcic.peko

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.toSet


interface PermissionRequester {

	/**
	 * Checks if all permissions are granted
	 * @return [Boolean]
	 */
	fun areGranted(vararg permissions: String): Boolean

	/**
	 * Checks if any of the permissions are granted
	 * @return [Boolean]
	 */
	fun isAnyGranted(vararg permissions: String): Boolean

	/**
	 * Starts the permission request flow.
	 * The result is a flow of [PermissionResult] for each permission requested.
	 * @return Flow of [PermissionResult]
	 */
	fun request(vararg permissions: String): Flow<PermissionResult>

	companion object {
		fun initialize(context: Context) {
			check(context !is Activity) { "Application Context expected as parameter to avoid memory leaks." }
			appContext = context
		}

		private var appContext: Context? = null

		internal var requesterFactory = NativeRequesterFactory.default()
		internal var requestBuilder = PermissionRequestBuilder.default()

		fun instance(): PermissionRequester = PekoPermissionRequester(requesterFactory, requestBuilder)
	}

	private class PekoPermissionRequester(
		private val requesterFactory: NativeRequesterFactory,
		private val permissionRequestBuilder: PermissionRequestBuilder
	) : PermissionRequester {

		override fun areGranted(vararg permissions: String): Boolean {
			val request = permissionRequestBuilder.createPermissionRequest(requireContext(), *permissions)
			return request.denied.isEmpty()
		}

		override fun request(vararg permissions: String): Flow<PermissionResult> {
			val request = permissionRequestBuilder.createPermissionRequest(requireContext(), *permissions)

			val flow = channelFlow {
				for (granted in request.granted) {
					trySend(PermissionResult.Granted(granted))
				}
				if (request.denied.isNotEmpty()) {
					val requester = requesterFactory.getRequesterAsync(requireContext()).await()
					requester.requestPermissions(request.denied.toTypedArray())
					for (result in requester.resultsChannel) {
						trySend(result)
					}
					requester.finish()
					channel.close()
				} else {
					channel.close()
				}
			}
			return flow
		}

		override fun isAnyGranted(vararg permissions: String): Boolean {
			val request = permissionRequestBuilder.createPermissionRequest(requireContext(), *permissions)
			return permissions.isNotEmpty() && request.granted.isNotEmpty()
		}

		private fun requireContext() =
			checkNotNull(appContext) { "App Context is null. Forgot to call the initialize method?" }
	}
}


suspend fun Flow<PermissionResult>.allGranted(): Boolean {
	return this.toSet().all { p -> p is PermissionResult.Granted }
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
