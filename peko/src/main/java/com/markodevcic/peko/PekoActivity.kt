package com.markodevcic.peko

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.PermissionChecker
import android.view.WindowManager

internal class PekoActivity : Activity(),
		ActivityCompat.OnRequestPermissionsResultCallback,
		PermissionRequester {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		listener?.onRequesterReady(this)
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
			listener?.onPermissionResult(grantedPermissions, deniedPermissions)
		}
	}

	override fun finish() {
		super.finish()
		listener = null
	}

	companion object {
		private const val REQUEST_CODE = 93173

		private var listener: PermissionRequesterListener? = null

		internal fun startActivity(context: Context, intent: Intent, listener: PermissionRequesterListener) {
			this.listener = listener
			context.startActivity(intent)
		}
	}
}

