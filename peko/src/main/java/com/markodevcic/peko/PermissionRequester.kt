package com.markodevcic.peko

import kotlinx.coroutines.flow.Flow

interface PermissionRequester {
	fun areGranted(vararg permissions: String): Boolean
	fun flowPermissions(vararg permissions: String): Flow<PermissionResult>
}