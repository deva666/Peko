package com.markodevcic.peko.rationale

import android.support.design.widget.Snackbar
import kotlinx.coroutines.experimental.suspendCancellableCoroutine

class SnackBarRationale(private val snackbar: Snackbar, private val actionTitle: String) : PermissionRationale {
	override suspend fun shouldRequestAfterRationaleShown(): Boolean {
		return suspendCancellableCoroutine { continuation ->
			var resumed = false
			snackbar.setAction(actionTitle, {
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
			snackbar.show()
		}
	}
}