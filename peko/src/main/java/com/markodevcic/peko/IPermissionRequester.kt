package com.markodevcic.peko

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet

interface IPermissionRequester {
	fun areGranted(vararg permissions: String): Boolean
	fun flowPermissions(vararg permissions: String): Flow<PermissionResult>
	
	val default: IPermissionRequester
		get() = PermissionRequester()
}


suspend fun Flow<PermissionResult>.allGranted(): Boolean {
	return this.toList().all { p -> p is PermissionResult.Granted }
}

suspend fun Flow<PermissionResult>.deniedPermissions(): Collection<PermissionResult> {
	return this.filterIsInstance<PermissionResult.Denied>().toSet()
}

suspend fun Flow<PermissionResult>.deniedPermanently(): Collection<PermissionResult> {
	return this.filterIsInstance<PermissionResult.Denied.DeniedPermanently>().toSet()
}

suspend fun Flow<PermissionResult>.needsRationalePermissions(): Collection<PermissionResult> {
	return this.filterIsInstance<PermissionResult.Denied.NeedsRationale>().toSet()
}

suspend fun Flow<PermissionResult>.grantedPermissions(): Collection<PermissionResult> {
	return this.filterIsInstance<PermissionResult.Granted>().toSet()
}