package com.markodevcic.peko

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlin.random.Random

internal interface NativeActivityFactory {
	fun getActivityAsync(context: Context): Deferred<NativeActivity>

	companion object {
		fun default(): NativeActivityFactory = NativeActivityFactoryImpl()
	}
}

private class NativeActivityFactoryImpl : NativeActivityFactory {
	override fun getActivityAsync(context: Context): Deferred<NativeActivity> {
		val completableDeferred = CompletableDeferred<NativeActivity>()
		val requestId = getRequestId()
		PekoActivity.idToRequesterMap[requestId] = completableDeferred
		val intent = Intent(context, PekoActivity::class.java)
		intent.putExtra("requestId", requestId)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		context.startActivity(intent)
		return completableDeferred
	}

	private fun getRequestId(): String {
		return Random.nextInt(Int.MAX_VALUE).toString()
	}
}