package com.markodevcic.peko

sealed class PermissionResult {
	data class Granted(val permission: String) : PermissionResult()
	sealed class Denied(open val permission: String) : PermissionResult() {
		data class NeedsRationale(override val permission: String) : Denied(permission)
		data class DeniedPermanently(override val permission: String) : Denied(permission)
	}

	object Cancelled : PermissionResult()
}
