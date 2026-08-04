package com.markodevcic.peko

import android.os.Bundle
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap

internal class PekoActivity : FragmentActivity(),
	ActivityCompat.OnRequestPermissionsResultCallback,
	NativeActivity {

	private lateinit var viewModel: PekoViewModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
		viewModel = ViewModelProvider(this@PekoActivity)[PekoViewModel::class.java]
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		val requestId =
			intent.getStringExtra("requestId") ?: throw IllegalStateException("missing request Id intent flag")
		val completableDeferred = idToRequesterMap.remove(requestId)
		completableDeferred?.complete(this)
	}

	override fun requestPermissions(permissions: Array<out String>, channel: Pair<Int, Channel<PermissionResult>>) {
		viewModel.putChannel(channel)
		ActivityCompat.requestPermissions(this@PekoActivity, permissions, channel.first)
	}

	override fun checkStateOfDeniedPermissions(permissions: Array<out String>,channel: Channel<PermissionState>) {
		if (permissions.isEmpty()){
			channel.close()
		}
		val needRationalePermissions = permissions
			.filter { p -> ActivityCompat.shouldShowRequestPermissionRationale(this@PekoActivity,p) }
		val permanentlyDeniedPermissions = permissions
			.filter { p -> !needRationalePermissions.contains(p) }

		needRationalePermissions.forEach { permission ->
			channel.trySend(PermissionState.NeedsRationale(permission))
		}
		permanentlyDeniedPermissions.forEach { permission ->
			channel.trySend(PermissionState.NeverAskedOrDeniedPermanently(permission))
		}
		channel.close()

	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		val channel = viewModel.getChannel(requestCode)
		if (channel != null) {
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
				channel.trySend(PermissionResult.Cancelled)
			} else {
				for (p in grantedPermissions) {
					channel.trySend(PermissionResult.Granted(p))
				}
				for (p in needsRationalePermissions) {
					channel.trySend(PermissionResult.Denied.NeedsRationale(p))
				}
				for (p in doNotAskAgainPermissions) {
					channel.trySend(PermissionResult.Denied.DeniedPermanently(p))
				}
			}
			channel.close()
		}
	}

	companion object {
		internal var idToRequesterMap = ConcurrentHashMap<String, CompletableDeferred<NativeActivity>>()
	}
}