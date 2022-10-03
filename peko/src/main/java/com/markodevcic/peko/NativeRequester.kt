package com.markodevcic.peko

import kotlinx.coroutines.channels.ReceiveChannel

internal interface NativeRequester {
	fun requestPermissions(permissions: Array<out String>)
	fun finish()
	val resultsChannel: ReceiveChannel<PermissionResult>
}
