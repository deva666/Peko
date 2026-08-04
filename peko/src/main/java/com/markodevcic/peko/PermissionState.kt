package com.markodevcic.peko

sealed class PermissionState {

    /**
     * Represents a granted permission
     * @param permission, the permission which is granted
     */
    data class Granted(val permission: String) : PermissionState()

    /**
     * Represents a permission that needs a rationale to be shown before the next request
     * @param permission, the permission which needs rationale
     */
    data class NeedsRationale(val permission: String) : PermissionState()

    /**
     * Represents a permission whose state cannot be distinguished by Android
     * without a permission request. It may be either never requested or
     * denied permanently.
     * @param permission, the permission whose exact state cannot be determined.
     */
    data class NeverAskedOrDeniedPermanently(val permission: String) : PermissionState()
}