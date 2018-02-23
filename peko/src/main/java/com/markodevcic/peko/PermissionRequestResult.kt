package com.markodevcic.peko

class PermissionRequestResult(val grantedPermissions: Collection<String>,
							  val deniedPermissions: Collection<String>)