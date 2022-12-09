package com.markodevcic.peko

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.channels.Channel

internal interface NativeRequesterFactory {
	fun requesterChannel(context: Context, vararg permissions: String): Channel<NativeRequester>

	companion object {
		fun default(): NativeRequesterFactory = NativeRequesterFactoryImpl()
	}
}

private class NativeRequesterFactoryImpl : NativeRequesterFactory {
	override fun requesterChannel(context: Context, vararg permissions: String): Channel<NativeRequester> {
		val intent = Intent(context, PekoActivity::class.java)
		intent.addFlags(
			Intent.FLAG_ACTIVITY_NEW_TASK
					or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
					or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
		)
		val permissionList = ArrayList(permissions.sortedArray().toList())
		intent.putStringArrayListExtra("permissions", permissionList)
		context.startActivity(intent)
		val channel = Channel<NativeRequester>(Channel.BUFFERED)
		PekoActivity.permissionsToChannelMap[permissionList] = channel
		return channel
	}
}
