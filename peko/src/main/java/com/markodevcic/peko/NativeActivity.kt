package com.markodevcic.peko

import kotlinx.coroutines.channels.Channel

internal interface NativeActivity {
	fun requestPermissions(permissions: Array<out String>, channel: Pair<Int, Channel<PermissionResult>>)
	fun checkStateOfDeniedPermissions(permissions: Array<out String>, channel: Channel<PermissionState>)
	fun finish()
}