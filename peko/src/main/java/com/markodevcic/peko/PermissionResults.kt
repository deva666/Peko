@file:Suppress("MemberVisibilityCanBePrivate", "unused", "CanBeParameter")

package com.markodevcic.peko

sealed class PermissionResults(
	val results: Collection<PermissionResult>
) {

	val deniedPermissions = results.filterIsInstance<PermissionResult.Denied>()
		.map { r -> r.permission }.toSet()
	val needsRationalePermissions =
		results.filterIsInstance<PermissionResult.Denied.NeedsRationale>().map { r -> r.permission }.toSet()
	val justDeniedPermissions =
		results.filterIsInstance<PermissionResult.Denied.JustDenied>().map { r -> r.permission }.toSet()
	val deniedPermanentlyPermissions =
		results.filterIsInstance<PermissionResult.Denied.PermanentlyDenied>().map { r -> r.permission }.toSet()
	val grantedPermissions = results.filterIsInstance<PermissionResult.Granted>().map { r -> r.permission }.toSet()

	class AllGranted(
		results: Collection<PermissionResult>,
	) : PermissionResults(results)

	class Denied(
		results: Collection<PermissionResult>,
	) : PermissionResults(results)

	object Cancelled : PermissionResults(setOf())
}

sealed class PermissionResult(val permission: String) {
	class Granted(permission: String) : PermissionResult(permission)

	sealed class Denied(permission: String) : PermissionResult(permission) {
		class NeedsRationale(permission: String) : Denied(permission)
		class JustDenied(permission: String) : Denied(permission)
		class PermanentlyDenied(permission: String) : Denied(permission)
	}

	object Cancelled : PermissionResult("")
}


/**
 * Represents a reason why a permission is denied.
 */
sealed class DeniedReason {
	/**
	 * A permission was denied. It is safe to ask again for this permission. Can happen if a user closes the permission dialog without selecting an option.
	 * @param permissions all permissions that were just denied.
	 */
	class JustDenied(val permissions: Collection<String>) : DeniedReason()

	/**
	 * A rationale should be shown explaining why this permission is needed. Usually happens after user denies the permission for the first time.
	 * @param permissions all permissions that need rationale shown.
	 */
	class NeedsRationale(val permissions: Collection<String>) : DeniedReason()

	/**
	 * Permissions for which the native permission dialog will not appear anymore. Usually happens after a user denies after rationale was shown.
	 * @param permissions all permissions that were denied permenently
	 */
	class DeniedPermanently(val permissions: Collection<String>) : DeniedReason()
}