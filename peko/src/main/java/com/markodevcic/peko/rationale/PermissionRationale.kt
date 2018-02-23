package com.markodevcic.peko.rationale

interface PermissionRationale {

	suspend fun shouldRequestAfterRationaleShown(): Boolean = false

	companion object {
		val EMPTY: PermissionRationale = EmptyPermissionRationale()
	}
}

internal class EmptyPermissionRationale : PermissionRationale