package com.markodevcic.peko.rationale

import android.support.design.widget.Snackbar
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SnackBarRationale(private val snackbar: Snackbar, private val actionTitle: String) : PermissionRationale {
	private var isCancelled = false

	fun cancel() {
		isCancelled = true
	}

	override suspend fun shouldRequestAfterRationaleShownAsync(): Boolean {
		if (isCancelled) return false
		return suspendCancellableCoroutine { continuation ->
			var resumed = false
			snackbar.setAction(actionTitle) {
				if (!continuation.isCancelled && !resumed) {
					resumed = true
					continuation.resume(true)
				}
			}
			snackbar.addCallback(object : Snackbar.Callback() {
				override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
					super.onDismissed(transientBottomBar, event)
					if (!continuation.isCancelled && !resumed) {
						resumed = true
						continuation.resume(false)
					}
				}
			})
			snackbar.show()
		}
	}
}