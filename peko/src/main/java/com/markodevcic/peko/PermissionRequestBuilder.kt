package com.markodevcic.peko

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

internal interface PermissionRequestBuilder {
	fun createPermissionRequest(context: Context, vararg permissions: String): PermissionRequest

	private class PekoPermissionRequestBuilder : PermissionRequestBuilder {
		override fun createPermissionRequest(context: Context, vararg permissions: String): PermissionRequest {
			val permissionsGroup = permissions.groupBy { p -> ContextCompat.checkSelfPermission(context, p) }
			val denied = permissionsGroup[PackageManager.PERMISSION_DENIED] ?: listOf()
			val granted = permissionsGroup[PackageManager.PERMISSION_GRANTED] ?: listOf()
			return PermissionRequest(granted, denied)
		}

	}

	companion object {
		fun default(): PermissionRequestBuilder = PekoPermissionRequestBuilder()
	}
}