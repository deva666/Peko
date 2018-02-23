package com.markodevcic.peko

internal interface PermissionRequesterListener {
	fun onRequesterReady(requester: PermissionRequester)
	fun onPermissionResult(granted: Collection<String>, denied: Collection<String>)
}