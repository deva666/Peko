package com.markodevcic.peko

import android.os.Bundle
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.concurrent.ConcurrentHashMap

internal class PekoActivity : FragmentActivity(),
	ActivityCompat.OnRequestPermissionsResultCallback,
	NativeRequester {

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
		val requestId =
			intent.getStringExtra("requestId") ?: throw IllegalStateException("missing request Id intent flag")
		val completableDeferred =
			idToRequesterMap[requestId] ?: throw IllegalStateException("missing completable deferred")
		completableDeferred.complete(this)
		idToRequesterMap.remove(requestId)
	}

	override fun requestPermissions(permissions: Array<out String>) {
		ActivityCompat.requestPermissions(this@PekoActivity, permissions, REQUEST_CODE)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == REQUEST_CODE) {
			val grantedPermissions = mutableSetOf<String>()
			val deniedPermissions = mutableSetOf<String>()
			for (i in permissions.indices) {
				val permission = permissions[i]
				when (grantResults[i]) {
					PermissionChecker.PERMISSION_DENIED, PermissionChecker.PERMISSION_DENIED_APP_OP -> deniedPermissions.add(
						permission
					)
					PermissionChecker.PERMISSION_GRANTED -> grantedPermissions.add(permission)
				}
			}
			val needsRationalePermissions =
				deniedPermissions.filter { p -> ActivityCompat.shouldShowRequestPermissionRationale(this, p) }
			val doNotAskAgainPermissions = deniedPermissions.filter { p -> !needsRationalePermissions.contains(p) }
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
					viewModel.channel.trySend(PermissionResult.Denied.DeniedPermanently(p))
				}
			}
			viewModel.channel.close()
		}
	}

	override fun finish() {
		super.finish()
		viewModel.channel.close()
	}

	companion object {
		private const val REQUEST_CODE = 931
		internal var idToRequesterMap = ConcurrentHashMap<String, CompletableDeferred<NativeRequester>>()
	}
}