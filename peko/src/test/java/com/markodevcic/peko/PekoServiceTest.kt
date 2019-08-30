package com.markodevcic.peko

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class PekoServiceTest {

    private val context = Mockito.mock(Context::class.java)
    private val permissionRequesterFactory = Mockito.mock(PermissionRequesterFactory::class.java)
    private val dispatcher = Dispatchers.Unconfined
    private val permissionRequester = Mockito.mock(PermissionRequester::class.java)

    @Before
    fun setup() {
        Mockito.`when`(permissionRequesterFactory.getRequester(context)).thenReturn(CompletableDeferred(permissionRequester))
    }

    @Test
    fun testRequestPermissionsGranted() {
        val request = PermissionRequest(listOf(), listOf("BLUETOOTH"))
        val channel = Channel<PermissionResult>()
        Mockito.`when`(permissionRequester.resultsChannel).thenReturn(channel)

        val sut = PekoService(context, request, permissionRequesterFactory, dispatcher)

        runBlocking {
            async {
                delay(200)
                channel.send(PermissionResult.Granted(listOf("BLUETOOTH")))
            }
            val result = sut.requestPermissions()
            val grantedResult = result as? PermissionResult.Granted ?: throw IllegalStateException("result should be Granted")
            Assert.assertTrue(grantedResult.grantedPermissions.size == 1)
            Assert.assertTrue(grantedResult.grantedPermissions.contains("BLUETOOTH"))
        }
    }


    @Test
    fun testRequestPermissionsGrantedAppended() {
        val request = PermissionRequest(listOf("CAMERA"), listOf("BLUETOOTH"))
        val channel = Channel<PermissionResult>()
        Mockito.`when`(permissionRequester.resultsChannel).thenReturn(channel)

        val sut = PekoService(context, request, permissionRequesterFactory, dispatcher)

        runBlocking {
            async {
                delay(200)
                channel.send(PermissionResult.Granted(listOf("BLUETOOTH")))
            }
            val result = sut.requestPermissions()
            val grantedResult = result as? PermissionResult.Granted ?: throw IllegalStateException("result should be Granted")
            Assert.assertTrue(result.grantedPermissions.size == 2)
            Assert.assertTrue(result.grantedPermissions.contains("BLUETOOTH"))
            Assert.assertTrue(result.grantedPermissions.contains("CAMERA"))
        }
    }

    @Test
    fun testRequestPermissionsDenied() {
        val request = PermissionRequest(granted = listOf("CAMERA"), denied = listOf("BLUETOOTH"))
        val channel = Channel<PermissionResult>()
        Mockito.`when`(permissionRequester.resultsChannel).thenReturn(channel)

        val sut = PekoService(context, request, permissionRequesterFactory, dispatcher)

        runBlocking {
            async {
                delay(200)
                channel.send(PermissionResult.Denied.DeniedInitially(listOf("BLUETOOTH")))
            }

            val result = sut.requestPermissions()
            val deniedResult = result as? PermissionResult.Denied ?: throw IllegalStateException("result should be Denied")
            Assert.assertTrue(result.deniedPermissions.contains("BLUETOOTH"))
        }
    }
}