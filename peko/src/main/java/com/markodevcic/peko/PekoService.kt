package com.markodevcic.peko

import android.content.Context
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume


internal class PekoService(context: Context,
                           private val request: PermissionRequest,
                           private val requesterFactory: PermissionRequesterFactory = PermissionRequesterFactory.defaultFactory,
                           private val dispatcher: CoroutineDispatcher = Dispatchers.Main) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = dispatcher + job

    private val pendingPermissions = mutableSetOf<String>()
    private val grantedPermissions = mutableSetOf<String>()
    private val deniedPermissions = mutableSetOf<String>()
    private val contextReference: WeakReference<out Context> = WeakReference(context)

    private val job = Job()
    private lateinit var requester: PermissionRequester
    private lateinit var continuation: CancellableContinuation<Result>

    suspend fun requestPermissions(): Result {
        val context = contextReference.get()
                ?: return Result.Denied(request.denied)

        return suspendCancellableCoroutine { continuation ->
            setupContinuation(continuation)

            pendingPermissions.addAll(request.denied)
            grantedPermissions.addAll(request.granted)

            requestPermissions(context)
        }
    }

    private fun setupContinuation(continuation: CancellableContinuation<Result>) {
        this.continuation = continuation
        continuation.invokeOnCancellation { fail ->
            if (fail !is ActivityRotatingException) {
                job.cancel()
                if (::requester.isInitialized) {
                    requester.finish()
                }
            }
        }
    }

    suspend fun resumeRequest(): Result {
        if (::requester.isInitialized) {
            return suspendCancellableCoroutine { continuation ->
                setupContinuation(continuation)
            }
        } else {
            throw IllegalStateException("trying to resume a request that doesn't exist")
        }
    }

    private fun requestPermissions(context: Context) {
        this.launch {
            requester = requesterFactory.getRequester(context).await()
            requester.requestPermissions(request.denied.toTypedArray())
            for (result in requester.resultsChannel) {
                when (result) {
                    is Result.Granted -> {
                        permissionsGranted(result.grantedPermissions)
                    }
                    is Result.Denied -> {
                        updateDeniedPermissions(result.deniedPermissions)
                    }
                    is Result.NeedsRationale -> {
                        requester.finish()
                        continuation.resume(result)
                    }
                    is Result.DoNotAskAgain -> {
                        requester.finish()
                        continuation.resume(result)
                    }
                }
//                permissionsGranted(result.grantedPermissions)
//                permissionsDenied(result.deniedPermissions)
            }
        }
    }

    private fun permissionsGranted(permissions: Collection<String>) {
        pendingPermissions.removeAll(permissions)
        grantedPermissions.addAll(permissions)
        checkIfRequestComplete()
    }

//    private fun permissionsDenied(permissions: Collection<String>) {
//        val showRationalePermissions = permissions.any { p -> !rationaleChecker.checkIfRationaleShownAlready(p) }
//        if (showRationalePermissions) {
//            this.launch {
//                if (rationale.shouldRequestAfterRationaleShownAsync()) {
//                    requester.requestPermissions(permissions.toTypedArray())
//                } else {
//                    updateDeniedPermissions(permissions)
//                }
//                rationaleChecker.setRationaleShownFor(permissions)
//            }
//        } else {
//            updateDeniedPermissions(permissions)
//        }
//    }

    private fun updateDeniedPermissions(permissions: Collection<String>) {
        pendingPermissions.removeAll(permissions)
        deniedPermissions.addAll(permissions)
        checkIfRequestComplete()
    }

    private fun checkIfRequestComplete() {
        if (pendingPermissions.isEmpty() && continuation.isActive) {
            requester.finish()
            continuation.resume(if (deniedPermissions.isEmpty()) Result.Granted(grantedPermissions) else Result.Denied(deniedPermissions))
        }
    }
}