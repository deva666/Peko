package com.markodevcic.peko

import android.os.Bundle
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel

internal class PekoActivity : FragmentActivity(),
	ActivityCompat.OnRequestPermissionsResultCallback,
	PermissionRequester {

	private lateinit var viewModel: PekoViewModel

	override val resultsChannel: ReceiveChannel<PermissionResults>
		get() = viewModel.channel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
		viewModel = ViewModelProvider(this@PekoActivity).get(PekoViewModel::class.java)
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		requesterDeferred?.complete(this)
		requesterDeferred = null
	}

	override fun requestPermissions(permissions: Array<out String>) {
		ActivityCompat.requestPermissions(this@PekoActivity, permissions, REQUEST_CODE)
	}

	@ExperimentalCoroutinesApi
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
			val deniedPermissions = setOf(userDeniedPermissions + deniedApOpPermissions)
			val needsRationalePermissions =
				userDeniedPermissions.filter { p -> ActivityCompat.shouldShowRequestPermissionRationale(this, p) }
			val doNotAskAgainPermissions = userDeniedPermissions.filter { p -> !needsRationalePermissions.contains(p) }
			deniedApOpPermissions =
				deniedApOpPermissions.filter { p -> !needsRationalePermissions.contains(p) }.toMutableSet()
			if (viewModel.channel.isClosedForSend) {
				return
			}
			viewModel.channel.trySend(
				when {
					permissions.isEmpty() -> PermissionResults.Cancelled
					deniedPermissions.isEmpty() -> PermissionResults.AllGranted(grantedPermissions.map { p ->
						PermissionResult.Granted(
							p
						)
					}.toSet())
					else -> {
						val denied = mutableSetOf<PermissionResult>()
						if (needsRationalePermissions.isNotEmpty()) {
							denied.addAll(needsRationalePermissions.map { p -> PermissionResult.Denied.NeedsRationale(p) }
								.toSet())
						}
						if (doNotAskAgainPermissions.isNotEmpty()) {
							denied.addAll(doNotAskAgainPermissions.map { p ->
								PermissionResult.Denied.PermanentlyDenied(
									p
								)
							}.toSet())
						}
						if (deniedApOpPermissions.isNotEmpty()) {
							denied.addAll(deniedApOpPermissions.map { p -> PermissionResult.Denied.JustDenied(p) }
								.toSet())
						}
						PermissionResults.Denied((grantedPermissions.map { p -> PermissionResult.Granted(p) } + denied).toSet())
					}
				}
			)
		}
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