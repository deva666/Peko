package com.markodevcic.peko

data class PermissionRequestResult(val grantedPermissions: Collection<String>,
							  val deniedPermissions: Collection<String>)