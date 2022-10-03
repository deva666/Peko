package com.markodevcic.peko

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import java.util.concurrent.atomic.AtomicReference

object Peko {

    private val serviceReference = AtomicReference<PekoService?>(null)

    /**
     * Resumes a request that was previously canceled with [ActivityRotatingException]
     * @throws [IllegalStateException] if there is no request in progress
     */
    fun resumeRequest(): Flow<PermissionResults> {
        try {
//            val service = serviceReference.get() ?: throw IllegalStateException("there is no request in progress")
//            val result = service.resumeRequest()
//            serviceReference.set(null)
//            return resultVkk
            Log.d("Peko", "resume")
            return serviceReference.get()!!.flowPermissions()
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
    fun requestPermissionsAsync(context: Context, vararg permissions: String): Flow<PermissionResults> {

        if (isTargetSdkUnderAndroidM(context)) {
            return flowOf(PermissionResults.AllGranted(permissions.map { p -> PermissionResult.Granted(p) }))
        }

        val request = checkPermissions(context, permissions)
        if (request.denied.isNotEmpty()) {
            val service = PekoService(context, request)
            check(serviceReference.compareAndSet(null, service)) { "Can't request permission while another request in progress" }
            serviceReference.set(service)


            try {
                return service.flowPermissions().onCompletion {
                    serviceReference.set(null)
                }
            } catch (e: ActivityRotatingException) {
                throw e
            }

        } else {
            return flowOf(PermissionResults.AllGranted(request.granted.map { p -> PermissionResult.Granted(p) }))
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
        return PermissionRequest(granted, listOf(), denied)
    }

    /**
     * Checks if there is a request in progress.
     * If true is returned, resume the existing request by calling [resumeRequest]
     * Otherwise requesting a new permission while another one is in progress will result in [IllegalStateException]
     */
    fun isRequestInProgress(): Boolean =  serviceReference.get() != null


    /**
     * Checks if all permissions are granted
     */
    fun areGranted(activity: Activity, vararg permissions: String) : Boolean {
        val request = checkPermissions(activity, permissions)
        return request.denied.isEmpty()
    }
}