package com.markodevcic.peko

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.*

/**
 * Interface for requesting or checking if permissions are granted.
 * Obtain the default implementation with the [instance] function.
 */
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
	 * The result is a [Flow] of [PermissionResult] for each permission requested.
	 * @return [Flow] of [PermissionResult]
	 */
	fun request(vararg permissions: String): Flow<PermissionResult>

	companion object {
		/**
		 * Initialize the [PermissionRequester].
		 * Use Android Application Context to avoid memory leaks.
		 * Needs to be called before any other invocation on [PermissionRequester]
		 * @throws [IllegalStateException] if the passed [Context] is an Activity
		 * @param [context] Android Application Context
		 */
		fun initialize(context: Context) {
			check(context !is Activity) { "Application Context expected as parameter to avoid memory leaks." }
			appContext = context
		}

		private var appContext: Context? = null

		internal var requesterFactory = NativeRequesterFactory.default()
		internal var requestBuilder = PermissionRequestBuilder.default()

		/**
		 * Default Peko implementation of the [PermissionRequester]
		 * @return [PermissionRequester]
		 */
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
					val requester = requesterFactory.requesterChannel(requireContext(), *permissions).receive()
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

/**
 * Suspending function that checks if all permissions form the flow are granted.
 * Suspends until the underlying [Flow] completes.
 * @return [Boolean]
 */
suspend fun Flow<PermissionResult>.allGranted(): Boolean {
	return this.toSet().all { p -> p is PermissionResult.Granted }
}


/**
 * Suspending function that checks if any of the permissions form the flow are granted.
 * Commonly used with Android >= 12 and location requests, where either Coarse or Fine location permission can be enough to proceed.
 * Suspends until the underlying [Flow] completes.
 * @return [Boolean]
 */
suspend fun Flow<PermissionResult>.anyGranted(): Boolean {
	return this.toSet().any { p -> p is PermissionResult.Granted }
}

/**
 * Suspending function that returns a collection of permissions that are denied
 * Suspends until the underlying [Flow] completes.
 * @return Collection of [PermissionResult]
 */
suspend fun Flow<PermissionResult>.deniedPermissions(): Collection<PermissionResult> {
	return this.filterIsInstance<PermissionResult.Denied>().toSet()
}

/**
 * Suspending function that returns a collection of permissions that are denied permanently
 * Suspends until the underlying [Flow] completes.
 * @return Collection of [PermissionResult]
 */
suspend fun Flow<PermissionResult>.deniedPermanently(): Collection<PermissionResult> {
	return this.filterIsInstance<PermissionResult.Denied.DeniedPermanently>().toSet()
}

/**
 * Suspending function that returns a collection of permissions that need a permission rationale shown.
 * Suspends until the underlying [Flow] completes.
 * @return Collection of [PermissionResult]
 */
suspend fun Flow<PermissionResult>.needsRationalePermissions(): Collection<PermissionResult> {
	return this.filterIsInstance<PermissionResult.Denied.NeedsRationale>().toSet()
}

/**
 * Suspending function that returns a collections of permissions that are granted.
 * Suspends until the underlying [Flow] completes.
 * @return Collection of [PermissionResult]
 */
suspend fun Flow<PermissionResult>.grantedPermissions(): Collection<PermissionResult> {
	return this.filterIsInstance<PermissionResult.Granted>().toSet()
}

/**
 * Suspending function that checks if the permission request was cancelled.
 * Suspends until the underlying [Flow] completes.
 * @return [Boolean]
 */
suspend fun Flow<PermissionResult>.isCancelled(): Boolean {
	return this.filterIsInstance<PermissionResult.Cancelled>().firstOrNull() != null
}