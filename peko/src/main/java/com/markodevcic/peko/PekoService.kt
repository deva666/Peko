package com.markodevcic.peko

import android.content.Context
import com.markodevcic.peko.rationale.PermissionRationale
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext


internal class PekoService(context: Context,
						   private val request: PermissionRequest,
						   private val rationale: PermissionRationale,
						   private val rationaleChecker: RationaleChecker,
						   private val requesterFactory: PermissionRequesterFactory = PermissionRequesterFactory.defaultFactory,
						   private val dispatcher: CoroutineDispatcher = Dispatchers.Main) : CoroutineScope {

	override val coroutineContext: CoroutineContext
		get() = job + dispatcher

	private val pendingPermissions = mutableSetOf<String>()
	private val grantedPermissions = mutableSetOf<String>()
	private val deniedPermissions = mutableSetOf<String>()
	private val contextReference: WeakReference<out Context> = WeakReference(context)

	internal lateinit var deferredResult: CompletableDeferred<PermissionRequestResult>
	private lateinit var requester: PermissionRequester
	private val job = Job()

	fun requestPermissions(): Deferred<PermissionRequestResult> {
		val context = contextReference.get()
				?: return CompletableDeferred(PermissionRequestResult(request.granted, request.denied))

		deferredResult = CompletableDeferred()
		deferredResult.invokeOnCompletion { fail ->
			if (fail !is ActivityRotatingException) {
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
		this.launch {
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
		val showRationalePermissions = permissions.any { p -> !rationaleChecker.checkIfRationaleShownAlready(p) }
		if (showRationalePermissions && rationale != PermissionRationale.none) {
			this.launch {
				if (rationale.shouldRequestAfterRationaleShownAsync()) {
					requester.requestPermissions(permissions.toTypedArray())
				} else {
					updateDeniedPermissions(permissions)
				}
				rationaleChecker.setRationaleShownFor(permissions)
			}
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
}