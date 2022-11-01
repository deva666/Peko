package com.markodevcic.peko

/**
 * Represents a result of a permission request.
 * A sealed class of [PermissionResult.Granted] , [PermissionResult.Denied] and [PermissionResult.Cancelled] subclasses.
 * [PermissionResult.Denied] can be either [PermissionResult.Denied.DeniedPermanently] or [PermissionResult.Denied.NeedsRationale]
 */
sealed class PermissionResult {

	/**
	 * Represents a granted permission
	 * @param permission, the permission which is granted
	 */
	data class Granted(val permission: String) : PermissionResult()

	/**
	 * Sealed class that represents a denied permission
	 * @param permission, the permission which is denied
	 */
	sealed class Denied(open val permission: String) : PermissionResult() {

		/**
		 * Represents a permission that needs a rationale to be shown before the next request
		 * @param permission, the permission which needs rationale
		 */
		data class NeedsRationale(override val permission: String) : Denied(permission)

		/**
		 * Represents a permission that was denied permanently.
		 * Android OS will not show the request dialog for this permission in all further requests.
		 * @param permission, the permission which was denied permanently
		 */
		data class DeniedPermanently(override val permission: String) : Denied(permission)
	}

	/**
	 * Represents a permission request that was cancelled.
	 * It is safe to repeat the request.
	 */
	object Cancelled : PermissionResult()
}
