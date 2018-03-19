package com.markodevcic.peko.rationale

interface PermissionRationale {

	suspend fun shouldRequestAfterRationaleShownAsync(): Boolean = false

	companion object {
		val EMPTY: PermissionRationale = EmptyPermissionRationale()
	}
}

internal class EmptyPermissionRationale : PermissionRationale