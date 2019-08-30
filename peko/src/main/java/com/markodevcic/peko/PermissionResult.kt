package com.markodevcic.peko

sealed class PermissionResult {
	class Granted(val grantedPermissions: Collection<String>) : PermissionResult()
	sealed class Denied(val deniedPermissions: Collection<String>)  : PermissionResult() {
		class JustDenied(deniedPermissions: Collection<String>): Denied(deniedPermissions)
		class NeedsRationale(deniedPermissions: Collection<String>) : Denied(deniedPermissions)
		class DeniedPermanently(deniedPermissions: Collection<String>) : Denied(deniedPermissions)
	}
}