@file:Suppress("DEPRECATION")

package com.markodevcic.peko

import android.app.Activity
import androidx.fragment.app.Fragment

/**
 * Requests permissions asynchronously. The function suspends only if request contains permissions that are denied.
 * Should be called from a coroutine which has a UI (Main) Dispatcher as context.
 * If the parent job is cancelled with [ActivityRotatingException], ongoing request will be retained and can be resumed with [resumePermissionRequest] function.
 * @return [PermissionResults]
 * @throws [IllegalStateException] if called while another request has not completed yet
 */
suspend fun Activity.requestPermissionsAsync(vararg permissions: String) =
    Peko.requestPermissionsAsync(this, *permissions)

/**
 * Checks if there is a request in progress.
 * If true is returned, resume the existing request by calling [resumePermissionRequest]
 */
fun Activity.isPermissionRequestInProgress(): Boolean = Peko.isRequestInProgress()

/**
 * Resumes a request that was previously canceled with [ActivityRotatingException]
 * @throws [IllegalStateException] if there is no request in progress
 */
//suspend fun Activity.resumePermissionRequest(): PermissionResults = Peko.resumeRequest()

/**
 * Checks if all permissions are granted
 */
fun Activity.areGranted(vararg permissions: String) = Peko.areGranted(this, *permissions)

/**
 * Requests permissions asynchronously. The function suspends only if request contains permissions that are denied.
 * Should be called from a coroutine which has a UI (Main) Dispatcher as context.
 * If the parent job is cancelled with [ActivityRotatingException], ongoing request will be retained and can be resumed with [resumePermissionRequest] function.
 * @return [PermissionResults]
 * @throws [IllegalStateException] if called while another request has not completed yet
 */
suspend fun android.app.Fragment.requestPermissionsAsync(vararg permissions: String) =
    Peko.requestPermissionsAsync(this.activity, *permissions)

/**
 * Checks if there is a request in progress.
 * If true is returned, resume the existing request by calling [resumePermissionRequest]
 */
fun android.app.Fragment.isPermissionRequestInProgress(): Boolean = Peko.isRequestInProgress()

/**
 * Checks if all permissions are granted
 */
fun android.app.Fragment.areGranted(vararg permissions: String) = Peko.areGranted(this.activity, *permissions)

/**
 * Resumes a request that was previously canceled with [ActivityRotatingException]
 * @throws [IllegalStateException] if there is no request in progress
 */
//suspend fun android.app.Fragment.resumePermissionRequest(): PermissionResults = Peko.resumeRequest()


/**
 * Requests permissions asynchronously. The function suspends only if request contains permissions that are denied.
 * Should be called from a coroutine which has a UI (Main) Dispatcher as context.
 * If the parent job is cancelled with [ActivityRotatingException], ongoing request will be retained and can be resumed with [resumePermissionRequest] function.
 * @return [PermissionResults]
 * @throws [IllegalStateException] if called while another request has not completed yet
 */
suspend fun Fragment.requestPermissionsAsync(vararg permissions: String) =
    Peko.requestPermissionsAsync(this.requireActivity(), *permissions)

/**
 * Checks if there is a request in progress.
 * If true is returned, resume the existing request by calling [resumePermissionRequest]
 */
fun Fragment.isPermissionRequestInProgress(): Boolean = Peko.isRequestInProgress()

/**
 * Resumes a request that was previously canceled with [ActivityRotatingException]
 * @throws [IllegalStateException] if there is no request in progress
 */
//suspend fun Fragment.resumePermissionRequest(): PermissionResults = Peko.resumeRequest()

/**
 * Checks if all permissions are granted
 */
fun Fragment.areGranted(vararg permissions: String) = Peko.areGranted(this.requireActivity(), *permissions)