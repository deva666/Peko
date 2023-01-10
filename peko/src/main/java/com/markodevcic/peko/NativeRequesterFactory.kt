package com.markodevcic.peko

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

internal interface NativeRequesterFactory {
	fun getRequesterAsync(context: Context, vararg permissions: String): Deferred<NativeRequester>

	companion object {
		fun default(): NativeRequesterFactory = NativeRequesterFactoryImpl()
	}
}

private class NativeRequesterFactoryImpl : NativeRequesterFactory {
	override fun getRequesterAsync(context: Context, vararg permissions: String): Deferred<NativeRequester> {
		val completableDeferred = CompletableDeferred<NativeRequester>()
		val allPermissions = permissions.joinToString(",")
		PekoActivity.permissionsToRequesterMap[allPermissions] = completableDeferred
		val intent = Intent(context, PekoActivity::class.java)
		intent.putExtra("permissions", allPermissions)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		context.startActivity(intent)
		return completableDeferred
	}
}
