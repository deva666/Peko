package com.markodevcic.peko

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

internal class PekoService(
	context: Context,
	private val request: PermissionRequest,
	private val requesterFactory: PermissionRequesterFactory = PermissionRequesterFactory.defaultFactory,
	private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : CoroutineScope {

	override val coroutineContext: CoroutineContext
		get() = dispatcher + job

	private val grantedPermissions = mutableSetOf<String>()
	private val contextReference: WeakReference<out Context> = WeakReference(context)

	private val job = Job()
	private  var requester: PermissionRequester? = null
	private lateinit var continuation: CancellableContinuation<PermissionResults>

	suspend fun requestPermissions(): PermissionResults {
		val context = contextReference.get()
			?: return PermissionResults.Cancelled

		return suspendCancellableCoroutine { continuation ->
			setupContinuation(continuation)
			grantedPermissions.addAll(request.granted)
			requestPermissions(context)
		}
	}

	fun flowPermissions(): Flow<PermissionResults> {

		val context = contextReference.get()
			?: return flowOf(PermissionResults.Cancelled)

		grantedPermissions.addAll(request.granted)
		Log.d("Peko", "Callback Flow deffered requester")
//		requester = requesterFactory.getRequesterAsync(context).await()
// 		return requester.resultsChannel.receiveAsFlow()
//
//		Log.d("Peko", "Permissions flow")
		return emptyFlow()
		var flow = callbackFlow {
			grantedPermissions.addAll(request.granted)
			Log.d("Peko", "Callback Flow deffered requester")
			if (requester == null) {
				requester = requesterFactory.getRequesterAsync(context).await()
				requester?.requestPermissions(request.denied.toTypedArray())
			}
//			requester.resultsChannel.receiveAsFlow()
			Log.d("Peko", "Callback Flow request permissions")
//			req?.resultsChannel?.consumeAsFlow()?.onEach {
//				send(it)
//			}?.onCompletion {
//				Log.d("Peko", "closing requester")
//				req?.finish()
//				channel.close()
//			}?.collect()
			for (result in requester!!.resultsChannel) {
				Log.d("Peko", "Send")
				send(result)
			}
			Log.d("Peko", "Closing flow")
			requester?.finish()
			channel.close()
		}
		flow = flow.onCompletion {
			requester?.close()
			requester = null
//			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
//				context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
//			}
		}
//		return flow
	}

	private fun setupContinuation(continuation: CancellableContinuation<PermissionResults>) {
		this.continuation = continuation
		continuation.invokeOnCancellation { fail ->
			if (fail !is ActivityRotatingException) {
				job.cancel()
				if (requester != null) {
					requester?.finish()
				}
			}
		}
	}

	suspend fun resumeRequest(): PermissionResults {
		if (requester != null) {
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
			requester?.requestPermissions(request.denied.toTypedArray())
			for (result in requester!!.resultsChannel) {
//				tryCompleteRequest(result)
			}
		}
	}

	private fun tryCompleteRequest(result: PermissionResults) {
		if (continuation.isActive) {
			requester?.finish()
			continuation.resume(
				if (result is PermissionResults.AllGranted)
					PermissionResults.AllGranted((grantedPermissions + result.grantedPermissions).map { p ->
						PermissionResult.Granted(
							p
						)
					}.toSet())
				else result
			)
		}
	}
}