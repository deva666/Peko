@file:Suppress("MemberVisibilityCanBePrivate", "unused", "CanBeParameter")

package com.markodevcic.peko

sealed class PermissionResult(val permission: String) {
	class Granted(permission: String) : PermissionResult(permission)

	sealed class Denied(permission: String) : PermissionResult(permission) {
		class NeedsRationale(permission: String) : Denied(permission)
		class JustDenied(permission: String) : Denied(permission)
		class PermanentlyDenied(permission: String) : Denied(permission)
	}

	object Cancelled : PermissionResult("")
}
