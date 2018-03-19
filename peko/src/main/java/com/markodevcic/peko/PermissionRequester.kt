package com.markodevcic.peko

import kotlinx.coroutines.experimental.channels.ReceiveChannel

internal interface PermissionRequester {
	fun requestPermissions(permissions: Array<out String>)
	fun finish()
	val resultsChannel: ReceiveChannel<PermissionRequestResult>
}
