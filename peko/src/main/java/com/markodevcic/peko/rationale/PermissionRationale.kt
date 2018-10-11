package com.markodevcic.peko.rationale

interface PermissionRationale {

	suspend fun shouldRequestAfterRationaleShownAsync(): Boolean = false

	companion object {
		val none: PermissionRationale = DefaultPermissionRationale()
	}
}

internal class DefaultPermissionRationale : PermissionRationale