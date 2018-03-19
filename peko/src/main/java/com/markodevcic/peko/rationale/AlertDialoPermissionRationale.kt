package com.markodevcic.peko.rationale

import android.R
import android.content.Context
import android.support.v7.app.AlertDialog
import kotlinx.coroutines.experimental.suspendCancellableCoroutine


/**
[PermissionRationale] implementation that displays Alert Dialog to the user.
[builderInit] receiver function is used to set your custom message and title
Don't use Functions [AlertDialog.Builder.setPositiveButton], [AlertDialog.Builder.setNegativeButton]
and [AlertDialog.Builder.setOnDismissListener], they will be overridden
 */
class AlertDialogPermissionRationale(private val context: Context,
									 private val builderInit: AlertDialog.Builder.() -> Unit) : PermissionRationale {

	override suspend fun shouldRequestAfterRationaleShownAsync(): Boolean {
		return suspendCancellableCoroutine { continuation ->
			var resumed = false
			val builder = AlertDialog.Builder(context)
			builder.builderInit()
			builder.setPositiveButton(R.string.ok) { _, _ ->
				if (!resumed) {
					resumed = true
					continuation.resume(true)
				}
			}
					.setNegativeButton(R.string.cancel) { _, _ ->
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