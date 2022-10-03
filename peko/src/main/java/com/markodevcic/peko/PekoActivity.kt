package com.markodevcic.peko

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.ReceiveChannel

internal class PekoActivity : FragmentActivity(),
	ActivityCompat.OnRequestPermissionsResultCallback,
	PermissionRequester {

	private lateinit var viewModel: PekoViewModel

	override val resultsChannel: ReceiveChannel<PermissionResult>
		get() = viewModel.channel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
		viewModel = ViewModelProvider(this@PekoActivity)[PekoViewModel::class.java]
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		requesterDeferred?.complete(this)
		requesterDeferred = null
	}

	override fun requestPermissions(permissions: Array<out String>) {
		Log.d("Peko", "requestPermissions")
		ActivityCompat.requestPermissions(this@PekoActivity, permissions, REQUEST_CODE)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == REQUEST_CODE) {
			val grantedPermissions = mutableSetOf<String>()
			val userDeniedPermissions = mutableSetOf<String>()
			var deniedApOpPermissions = mutableSetOf<String>()
			for (i in permissions.indices) {
				val permission = permissions[i]
				when (grantResults[i]) {
					PermissionChecker.PERMISSION_DENIED -> userDeniedPermissions.add(permission)
					PermissionChecker.PERMISSION_DENIED_APP_OP -> deniedApOpPermissions.add(permission)
					PermissionChecker.PERMISSION_GRANTED -> grantedPermissions.add(permission)
				}
			}
			val deniedPermissions = mutableSetOf<String>()
			deniedPermissions.addAll(userDeniedPermissions)
			deniedPermissions.addAll(deniedApOpPermissions)
			val needsRationalePermissions =
				userDeniedPermissions.filter { p -> ActivityCompat.shouldShowRequestPermissionRationale(this, p) }
			val doNotAskAgainPermissions = userDeniedPermissions.filter { p -> !needsRationalePermissions.contains(p) }
			deniedApOpPermissions =
				deniedApOpPermissions.filter { p -> !needsRationalePermissions.contains(p) }.toMutableSet()
			if (viewModel.channel.isClosedForSend) {
				return
			}
			if (permissions.isEmpty()) {
				viewModel.channel.trySend(PermissionResult.Cancelled)
			} else {
				for (p in grantedPermissions) {
					viewModel.channel.trySend(PermissionResult.Granted(p))
				}
				for (p in needsRationalePermissions) {
					viewModel.channel.trySend(PermissionResult.Denied.NeedsRationale(p))
				}
				for (p in doNotAskAgainPermissions) {
					viewModel.channel.trySend(PermissionResult.Denied.PermanentlyDenied(p))
				}
				for (p in deniedApOpPermissions) {
					viewModel.channel.trySend(PermissionResult.Denied.JustDenied(p))
				}
			}
//			viewModel.channel.trySend(
//				when {
//					permissions.isEmpty() -> PermissionResults.Cancelled
//					deniedPermissions.isEmpty() -> PermissionResults.AllGranted(grantedPermissions.map { p ->
//						PermissionResult.Granted(
//							p
//						)
//					}.toSet())
//					else -> {
//						val denied = mutableSetOf<PermissionResult>()
//						if (needsRationalePermissions.isNotEmpty()) {
//							denied.addAll(needsRationalePermissions.map { p -> PermissionResult.Denied.NeedsRationale(p) }
//								.toSet())
//						}
//						if (doNotAskAgainPermissions.isNotEmpty()) {
//							denied.addAll(doNotAskAgainPermissions.map { p ->
//								PermissionResult.Denied.PermanentlyDenied(
//									p
//								)
//							}.toSet())
//						}
//						if (deniedApOpPermissions.isNotEmpty()) {
//							denied.addAll(deniedApOpPermissions.map { p -> PermissionResult.Denied.JustDenied(p) }
//								.toSet())
//						}
//						PermissionResults.Denied((grantedPermissions.map { p -> PermissionResult.Granted(p) } + denied).toSet())
//					}
//				}
//			)
			viewModel.channel.close()
		}
	}

	override fun close() {

	}

	override fun finish() {
		super.finish()
		viewModel.channel.close()
		requesterDeferred = null
	}

	companion object {
		private const val REQUEST_CODE = 931
		internal var requesterDeferred: CompletableDeferred<PermissionRequester>? = null
	}
}