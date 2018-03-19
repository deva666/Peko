package com.markodevcic.peko

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.PermissionChecker
import android.view.WindowManager
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.launch

internal class PekoActivity : Activity(),
		ActivityCompat.OnRequestPermissionsResultCallback,
		PermissionRequester {

	private val channel = Channel<PermissionRequestResult>()

	override val resultsChannel: ReceiveChannel<PermissionRequestResult>
		get() = channel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		deferred?.complete(this)
		deferred = null
	}

	override fun requestPermissions(permissions: Array<out String>) {
		ActivityCompat.requestPermissions(this@PekoActivity, permissions, REQUEST_CODE)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == REQUEST_CODE) {
			val grantedPermissions = ArrayList<String>()
			val deniedPermissions = ArrayList<String>()
			for (i in permissions.indices) {
				val permission = permissions[i]
				when (grantResults[i]) {
					PermissionChecker.PERMISSION_DENIED, PermissionChecker.PERMISSION_DENIED_APP_OP -> deniedPermissions.add(permission)
					PermissionChecker.PERMISSION_GRANTED -> grantedPermissions.add(permission)
				}
			}
			launch(UI) {
				channel.send(PermissionRequestResult(grantedPermissions, deniedPermissions))
			}
		}
	}

	override fun finish() {
		super.finish()
		channel.close()
		deferred = null
	}

	companion object {
		private const val REQUEST_CODE = 93173
		internal var deferred: CompletableDeferred<PermissionRequester>? = null
	}
}


