package com.markodevcic.peko

import kotlinx.coroutines.channels.ReceiveChannel

internal interface PermissionRequester {
	fun requestPermissions(permissions: Array<out String>)
	fun finish()
	fun close()
	val resultsChannel: ReceiveChannel<PermissionResult>
}
