package com.markodevcic.peko

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

internal interface PermissionGrouper {
	fun group(context: Context, vararg permissions: String): PermissionGroup

	private class PekoPermissionGrouper : PermissionGrouper {
		override fun group(context: Context, vararg permissions: String): PermissionGroup {
			val permissionsGroup = permissions.groupBy { p -> ContextCompat.checkSelfPermission(context, p) }
			val denied = permissionsGroup[PackageManager.PERMISSION_DENIED] ?: listOf()
			val granted = permissionsGroup[PackageManager.PERMISSION_GRANTED] ?: listOf()
			return PermissionGroup(granted, denied)
		}

	}

	companion object {
		fun default(): PermissionGrouper = PekoPermissionGrouper()
	}
}