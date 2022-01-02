package com.markodevcic.peko

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        Mockito.`when`(permissionRequesterFactory.getRequesterAsync(context))
            .thenReturn(CompletableDeferred(permissionRequester))
    }

    @Test
    fun testRequestPermissionsGranted() {
        val request = PermissionRequest(listOf(), listOf("BLUETOOTH"))
        val channel = Channel<PermissionResult>()
        Mockito.`when`(permissionRequester.resultsChannel).thenReturn(channel)

        val sut = PekoService(context, request, permissionRequesterFactory, dispatcher)

        runBlocking {
            launch {
                delay(200)
                channel.send(PermissionResult.Granted(listOf("BLUETOOTH")))
            }
            val result = sut.requestPermissions()
            val grantedResult = result as? PermissionResult.Granted
                ?: throw IllegalStateException("result should be Granted")
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
            launch {
                delay(200)
                channel.send(PermissionResult.Granted(listOf("BLUETOOTH")))
            }
            val result = sut.requestPermissions()
            val grantedResult = result as? PermissionResult.Granted
                ?: throw IllegalStateException("result should be Granted")
            Assert.assertTrue(grantedResult.grantedPermissions.size == 2)
            Assert.assertTrue(grantedResult.grantedPermissions.contains("BLUETOOTH"))
            Assert.assertTrue(grantedResult.grantedPermissions.contains("CAMERA"))
        }
    }

    @Test
    fun testRequestPermissionsDenied() {
        val request = PermissionRequest(granted = listOf("CAMERA"), denied = listOf("BLUETOOTH"))
        val channel = Channel<PermissionResult>()
        Mockito.`when`(permissionRequester.resultsChannel).thenReturn(channel)

        val sut = PekoService(context, request, permissionRequesterFactory, dispatcher)

        runBlocking {
            launch {
                delay(200)
                channel.send(PermissionResult.Denied.JustDenied(listOf("BLUETOOTH")))
            }

            val result = sut.requestPermissions()
            val deniedResult = result as? PermissionResult.Denied
                ?: throw IllegalStateException("result should be Denied")
            Assert.assertTrue(deniedResult.deniedPermissions.contains("BLUETOOTH"))
        }
    }

    @Test
    fun testRequestPermissionCancelled() {
        val request = PermissionRequest(granted = listOf("CAMERA"), denied = listOf("BLUETOOTH"))
        val channel = Channel<PermissionResult>()
        Mockito.`when`(permissionRequester.resultsChannel).thenReturn(channel)

        val sut = PekoService(context, request, permissionRequesterFactory, dispatcher)

        runBlocking {
            launch {
                delay(200)
                channel.send(PermissionResult.Cancelled)
            }

            val result = sut.requestPermissions()
            Assert.assertSame("result should be Cancelled", PermissionResult.Cancelled, result)
        }
    }
}