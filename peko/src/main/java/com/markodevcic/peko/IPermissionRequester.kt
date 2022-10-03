package com.markodevcic.peko

import kotlinx.coroutines.flow.Flow

interface IPermissionRequester {
	fun areGranted(vararg permissions: String): Boolean
	fun flowPermissions(vararg permissions: String): Flow<PermissionResult>
	
	val default: IPermissionRequester
		get() = PermissionRequester()
}