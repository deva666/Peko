package com.markodevcic.peko

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import java.util.concurrent.atomic.AtomicReference

object Peko {

    private val serviceReference = AtomicReference<PekoService?>(null)

    /**
     * Resumes a request that was previously canceled with [ActivityRotatingException]
     * @throws [IllegalStateException] if there is no request in progress
     */
    suspend fun resumeRequest(): PermissionResults {
        try {
            val service = serviceReference.get() ?: throw IllegalStateException("there is no request in progress")
            val result = service.resumeRequest()
            serviceReference.set(null)
            return result
        } catch (e: ActivityRotatingException) {
            throw e
        }
    }

    /**
     * Requests permissions asynchronously. The function suspends only if request contains permissions that are denied.
     * Should be called from a coroutine which has a UI (Main) Dispatcher as context.
     * If the parent job is cancelled with [ActivityRotatingException], ongoing request will be retained and can be resumed with [resumeRequest] function.
     * @return [PermissionResults]
     * @throws [IllegalStateException] if called while another request has not completed yet
     */
    suspend fun requestPermissionsAsync(context: Context, vararg permissions: String): PermissionResults {

        if (isTargetSdkUnderAndroidM(context)) {
            return PermissionResults.Granted(permissions.toList())
        }

        val request = checkPermissions(context, permissions)
        if (request.denied.isNotEmpty()) {
            val service = PekoService(context, request)

            check(serviceReference.compareAndSet(null, service)) { "Can't request permission while another request in progress" }

            try {
                val result = service.requestPermissions()
                serviceReference.set(null)
                return result
            } catch (e: ActivityRotatingException) {
                throw e
            }

        } else {
            return PermissionResults.Granted(request.granted)
        }
    }

    private fun isTargetSdkUnderAndroidM(context: Context): Boolean {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val targetSdkVersion = info.applicationInfo.targetSdkVersion
            targetSdkVersion < Build.VERSION_CODES.M
        } catch (fail: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun checkPermissions(context: Context, permissions: Array<out String>): PermissionRequest {
        val permissionsGroup = permissions.groupBy { p -> ActivityCompat.checkSelfPermission(context, p) }
        val denied = permissionsGroup[PackageManager.PERMISSION_DENIED] ?: listOf()
        val granted = permissionsGroup[PackageManager.PERMISSION_GRANTED] ?: listOf()
        return PermissionRequest(granted, denied)
    }

    /**
     * Checks if there is a request in progress.
     * If true is returned, resume the existing request by calling [resumeRequest]
     * Otherwise requesting a new permission while another one is in progress will result in [IllegalStateException]
     */
    fun isRequestInProgress(): Boolean = serviceReference.get() != null


    /**
     * Checks if all permissions are granted
     */
    fun areGranted(activity: Activity, vararg permissions: String) : Boolean {
        val request = checkPermissions(activity, permissions)
        return request.denied.isEmpty()
    }
}