package com.markodevcic.peko

sealed class PermissionResult {

	class Granted(val grantedPermissions: Collection<String>) : PermissionResult()

	open class Denied(val deniedPermissions: Collection<String>) : PermissionResult()

	class NeedsRationale(deniedPermissions: Collection<String>) : Denied(deniedPermissions)
	class DoNotAskAgain(deniedPermissions: Collection<String>) : Denied(deniedPermissions)
}