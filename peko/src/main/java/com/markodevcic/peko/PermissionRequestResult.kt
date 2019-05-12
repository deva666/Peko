package com.markodevcic.peko

data class PermissionRequestResult(val grantedPermissions: Collection<String>,
								   val deniedPermissions: Collection<String>)

sealed class Result {

	class Granted(val grantedPermissions: Collection<String>) : Result()

	open class Denied(val deniedPermissions: Collection<String>) : Result()
	class NeedsRationale(deniedPermissions: Collection<String>) : Denied(deniedPermissions)
	class DoNotAskAgain(deniedPermissions: Collection<String>) : Denied(deniedPermissions)
}
fun getResult(r: Result) {
	when(r) {
		is Result.Granted -> {}
	}
}