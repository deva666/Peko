package com.markodevcic.samples

import android.support.design.widget.Snackbar
import com.markodevcic.peko.rationale.PermissionRationale
import kotlinx.coroutines.experimental.suspendCancellableCoroutine

class SnackBarRationale(private val snackbar: Snackbar) : PermissionRationale {
	override suspend fun shouldRequestAfterRationaleShown(): Boolean {
		return suspendCancellableCoroutine { continuation ->
			var resumed = false
			snackbar.setAction("Request again", {
				if (!resumed) {
					resumed = true
					continuation.resume(true)
				}
			})
			snackbar.addCallback(object : Snackbar.Callback(){
				override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
					super.onDismissed(transientBottomBar, event)
					if (!resumed) {
						resumed = true
						continuation.resume(false)
					}
				}
			})
		}
	}
}