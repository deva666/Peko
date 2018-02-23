package com.markodevcic.peko

import android.content.Context
import android.support.v7.app.AlertDialog
import kotlinx.coroutines.experimental.suspendCancellableCoroutine

interface PermissionRationale {

	suspend fun shouldRequestAfterRationaleShown(): Boolean = false

	companion object {
		val EMPTY: PermissionRationale = EmptyPermissionRationale()
	}
}

internal class EmptyPermissionRationale : PermissionRationale

class AlertDialogPermissionRationale(private val context: Context,
									 private val builderInit: AlertDialog.Builder.() -> Unit) : PermissionRationale {

	override suspend fun shouldRequestAfterRationaleShown(): Boolean {
		return suspendCancellableCoroutine { continuation ->
			var resumed = false
			val builder = AlertDialog.Builder(context)
			builder.builderInit()
			builder.setPositiveButton(android.R.string.ok) { _, _ ->
						if (!resumed) {
							resumed = true
							continuation.resume(true)
						}
					}
					.setNegativeButton(android.R.string.cancel) { _, _ ->
						if (!resumed) {
							resumed = true
							continuation.resume(false)
						}
					}
					.setOnDismissListener {
						if (!resumed) {
							resumed = true
							continuation.resume(false)
						}
					}
					.show()
		}
	}
}