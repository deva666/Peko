package com.markodevcic.peko

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

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
	 * checks and returns state of all permissions
	 * @return [Flow]
	 */
	fun checkPermissionsState(vararg permissions: String): Flow<PermissionState>

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

		internal var activityFactory = NativeActivityFactory.default()
		internal var permissionGrouper = PermissionGrouper.default()


		/**
		 * Default Peko implementation of the [PermissionRequester]
		 * @return [PermissionRequester]
		 */
		fun instance(): PermissionRequester = PekoPermissionManager(activityFactory, permissionGrouper)
	}

	private class PekoPermissionManager (
		private val activityFactory: NativeActivityFactory,
		private val permissionGrouper: PermissionGrouper
	) : PermissionRequester {
		var nativeActivity: NativeActivity? = null
		private val nativeActivityExecutors: MutableSet<Int> = mutableSetOf()
		private val nativeActivityMutex = Mutex() // Mutex for synchronization

		suspend fun initNativeActivity(): Int {
			nativeActivityMutex.withLock {
				var executorId: Int
				do {
					executorId = Random.nextInt(0, Int.MAX_VALUE)
				} while (nativeActivityExecutors.contains(executorId))
				nativeActivityExecutors.add(executorId)

				if (nativeActivity == null) {
					nativeActivity = withContext(Dispatchers.Main) {
						activityFactory.getActivityAsync(requireContext()).await()
					}
				}

				return executorId
			}
		}

		suspend fun finishNativeActivity(executorId:Int) {
			nativeActivityMutex.withLock{
				nativeActivityExecutors.remove(executorId)
				if (nativeActivityExecutors.isEmpty()){
					nativeActivity?.finish()
					nativeActivity = null
				}
			}
		}

		override fun areGranted(vararg permissions: String): Boolean {
			val permissionGroup = permissionGrouper.group(requireContext(), *permissions)
			return permissionGroup.denied.isEmpty()
		}

		override fun request(vararg permissions: String): Flow<PermissionResult> {
			return channelFlow {
				val permissionGroup = permissionGrouper.group(requireContext(), *permissions)
				for (granted in permissionGroup.granted) {
					trySend(PermissionResult.Granted(granted))
				}
				if (permissionGroup.denied.isNotEmpty()) {
					val requestId = initNativeActivity()
					val channel = Channel<PermissionResult>(Channel.UNLIMITED)
					try {
						nativeActivity!!.requestPermissions(
							permissionGroup.denied.toTypedArray(),
							Pair(requestId, channel)
						)
						for (result in channel) {
							trySend(result)
						}
					} finally {
						channel.close()
						finishNativeActivity(requestId)
					}
				}
			}
		}

		override fun isAnyGranted(vararg permissions: String): Boolean {
			val permissionGroup = permissionGrouper.group(requireContext(), *permissions)
			return permissions.isNotEmpty() && permissionGroup.granted.isNotEmpty()
		}

		override fun checkPermissionsState(vararg permissions: String): Flow<PermissionState> {
			return channelFlow {
				if (permissions.isEmpty()) return@channelFlow

				val permissionGroup = permissionGrouper.group(requireContext(), *permissions)
				permissionGroup.granted.forEach { granted ->
					trySend(PermissionState.Granted(granted))
				}

				if (permissionGroup.denied.isNotEmpty()) {
					val checkStateId = initNativeActivity()
					val channel = Channel<PermissionState>(Channel.UNLIMITED)
					try {
						nativeActivity!!.checkStateOfDeniedPermissions(
							permissionGroup.denied.toTypedArray(),
							channel
						)
						for (state in channel) {
							trySend(state)
						}
					} finally {
						channel.close()
						finishNativeActivity(checkStateId)
					}
				}
			}
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
