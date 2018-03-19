package com.markodevcic.peko

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred

internal interface PermissionRequesterFactory {
	fun getRequester(context: Context): Deferred<PermissionRequester>

	companion object {
		val defaultFactory: PermissionRequesterFactory = PermissionRequesterFactoryImpl()
	}
}

private class PermissionRequesterFactoryImpl : PermissionRequesterFactory {
	override fun getRequester(context: Context): Deferred<PermissionRequester> {
		val completableDeferred = CompletableDeferred<PermissionRequester>()
		PekoActivity.deferred = completableDeferred
		val intent = Intent(context, PekoActivity::class.java)
		context.startActivity(intent)
		return completableDeferred
	}
}
