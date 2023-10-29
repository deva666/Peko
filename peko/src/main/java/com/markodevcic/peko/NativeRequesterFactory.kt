package com.markodevcic.peko

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.util.concurrent.ThreadLocalRandom

internal interface NativeRequesterFactory {
	fun getRequesterAsync(context: Context, vararg permissions: String): Deferred<NativeRequester>

	companion object {
		fun default(): NativeRequesterFactory = NativeRequesterFactoryImpl()
	}
}

private class NativeRequesterFactoryImpl : NativeRequesterFactory {
	override fun getRequesterAsync(context: Context, vararg permissions: String): Deferred<NativeRequester> {
		val completableDeferred = CompletableDeferred<NativeRequester>()
		val requestId = getRequestId()
		PekoActivity.idToRequesterMap[requestId] = completableDeferred
		val intent = Intent(context, PekoActivity::class.java)
		intent.putExtra("requestId", requestId)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		context.startActivity(intent)
		return completableDeferred
	}

	private fun getRequestId(): String {
		val random = ThreadLocalRandom.current().nextInt(Int.MAX_VALUE)
		return random.hashCode().toString()
	}
}
