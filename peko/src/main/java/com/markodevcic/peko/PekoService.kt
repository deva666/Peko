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

    private val grantedPermissions = mutableSetOf<String>()
    private val contextReference: WeakReference<out Context> = WeakReference(context)

    private val job = Job()
    private lateinit var requester: PermissionRequester
    private lateinit var continuation: CancellableContinuation<PermissionResult>

    suspend fun requestPermissions(): PermissionResult {
        val context = contextReference.get()
                ?: return PermissionResult.Denied(request.denied)

        return suspendCancellableCoroutine { continuation ->
            setupContinuation(continuation)
            grantedPermissions.addAll(request.granted)
            requestPermissions(context)
        }
    }

    private fun setupContinuation(continuation: CancellableContinuation<PermissionResult>) {
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

    suspend fun resumeRequest(): PermissionResult {
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
                tryCompleteRequest(result)
            }
        }
    }

    private fun tryCompleteRequest(result: PermissionResult) {
        if (continuation.isActive) {
            requester.finish()
            continuation.resume(if (result is PermissionResult.Granted)
                PermissionResult.Granted(grantedPermissions + result.grantedPermissions)
            else result)
        }
    }
}