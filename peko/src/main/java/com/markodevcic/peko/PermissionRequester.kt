package com.markodevcic.peko

internal interface PermissionRequester {
	fun requestPermissions(permissions: Array<out String>)
	fun finish()
}