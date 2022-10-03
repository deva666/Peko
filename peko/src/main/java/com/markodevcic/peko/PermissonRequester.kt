package com.markodevcic.peko

import kotlinx.coroutines.flow.Flow

interface PermissonRequester {
	fun areGranted(vararg permissions: String): Boolean
	fun flowPermissions(vararg permissions: String): Flow<PermissionResult>
}