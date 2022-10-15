package com.markodevcic.peko

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

internal interface NativeRequesterFactory {
	fun getRequesterAsync(context: Context): Deferred<NativeRequester>

	companion object {
		fun default(): NativeRequesterFactory = NativeRequesterFactoryImpl()
	}
}

private class NativeRequesterFactoryImpl : NativeRequesterFactory {
	override fun getRequesterAsync(context: Context): Deferred<NativeRequester> {
		val completableDeferred = CompletableDeferred<NativeRequester>()
		PekoActivity.requesterDeferred = completableDeferred
		val intent = Intent(context, PekoActivity::class.java)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		context.startActivity(intent)
		return completableDeferred
	}
}
