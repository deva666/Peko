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
    private lateinit var continuation: CancellableContinuation<PermissionResults>

    suspend fun requestPermissions(): PermissionResults {
        val context = contextReference.get()
                ?: return PermissionResults.Denied.JustDenied(request.denied)

        return suspendCancellableCoroutine { continuation ->
            setupContinuation(continuation)
            grantedPermissions.addAll(request.granted)
            requestPermissions(context)
        }
    }

    private fun setupContinuation(continuation: CancellableContinuation<PermissionResults>) {
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

    suspend fun resumeRequest(): PermissionResults {
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
            requester = requesterFactory.getRequesterAsync(context).await()
            requester.requestPermissions(request.denied.toTypedArray())
            for (result in requester.resultsChannel) {
                tryCompleteRequest(result)
            }
        }
    }

    private fun tryCompleteRequest(result: PermissionResults) {
        if (continuation.isActive) {
            requester.finish()
            continuation.resume(if (result is PermissionResults.Granted)
                PermissionResults.Granted(grantedPermissions + result.grantedPermissions)
            else result)
        }
    }
}