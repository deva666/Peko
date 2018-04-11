package com.markodevcic.peko

import android.content.Context
import android.content.SharedPreferences
import com.markodevcic.peko.rationale.PermissionRationale
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import java.lang.ref.WeakReference

internal class PekoService(context: Context,
						   private val request: PermissionRequest,
						   private val rationale: PermissionRationale,
						   private val sharedPreferences: SharedPreferences,
						   private val requesterFactory: PermissionRequesterFactory = PermissionRequesterFactory.defaultFactory,
						   private val dispatcher: CoroutineDispatcher = UI) {

	private val pendingPermissions = mutableSetOf<String>()
	private val grantedPermissions = mutableSetOf<String>()
	private val deniedPermissions = mutableSetOf<String>()
	private val contextReference: WeakReference<out Context> = WeakReference(context)

	private lateinit var deferredResult: CompletableDeferred<PermissionRequestResult>
	private lateinit var requester: PermissionRequester
	private val job = Job()

	fun requestPermissions(): Deferred<PermissionRequestResult> {
		val context = contextReference.get()
				?: return CompletableDeferred(PermissionRequestResult(request.granted, request.denied))

		deferredResult = CompletableDeferred()
		deferredResult.invokeOnCompletion(onCancelling = true) {
			if (deferredResult.isCancelled) {
				job.cancel()
				if (::requester.isInitialized) {
					requester.finish()
				}
			}
		}

		pendingPermissions.addAll(request.denied)
		grantedPermissions.addAll(request.granted)

		requestPermissions(context)

		return deferredResult
	}

	private fun requestPermissions(context: Context) {
		launch(job + dispatcher) {
			requester = requesterFactory.getRequester(context).await()
			requester.requestPermissions(request.denied.toTypedArray())
			for (result in requester.resultsChannel) {
				permissionsGranted(result.grantedPermissions)
				permissionsDenied(result.deniedPermissions)
			}
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
			launch(job + dispatcher) {
				if (rationale.shouldRequestAfterRationaleShownAsync()) {
					requester.requestPermissions(permissions.toTypedArray())
				} else {
					updateDeniedPermissions(permissions)
				}
			}
			setRationaleShownFor(request.denied)
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
		if (pendingPermissions.isEmpty() || contextReference.get() == null) {
			requester.finish()
			deferredResult.complete(PermissionRequestResult(grantedPermissions, deniedPermissions))
		}
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