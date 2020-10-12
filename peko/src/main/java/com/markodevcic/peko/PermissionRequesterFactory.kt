package com.markodevcic.peko

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

internal interface PermissionRequesterFactory {
	fun getRequesterAsync(context: Context): Deferred<PermissionRequester>

	companion object {
		val defaultFactory: PermissionRequesterFactory = PermissionRequesterFactoryImpl()
	}
}

private class PermissionRequesterFactoryImpl : PermissionRequesterFactory {
	override fun getRequesterAsync(context: Context): Deferred<PermissionRequester> {
		val completableDeferred = CompletableDeferred<PermissionRequester>()
		PekoActivity.requesterDeferred = completableDeferred
		val intent = Intent(context, PekoActivity::class.java)
		context.startActivity(intent)
		return completableDeferred
	}
}
