package com.markodevcic.peko

import android.content.Context
import com.markodevcic.peko.rationale.PermissionRationale
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class PekoServiceTest {

    private val context = Mockito.mock(Context::class.java)
    private val rationalChecker = Mockito.mock(RationaleChecker::class.java)
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
        val channel = Channel<PermissionRequestResult>()
        Mockito.`when`(permissionRequester.resultsChannel).thenReturn(channel)

        val sut = PekoService(context, request, PermissionRationale.none, rationalChecker, permissionRequesterFactory, dispatcher)

        runBlocking {
            async {
                delay(200)
                channel.send(PermissionRequestResult(listOf("BLUETOOTH"), listOf()))
            }
            val result = sut.requestPermissions()
            Assert.assertTrue(result.grantedPermissions.size == 1)
            Assert.assertTrue(result.grantedPermissions.contains("BLUETOOTH"))
            Assert.assertTrue(result.deniedPermissions.isEmpty())
        }
    }


    @Test
    fun testRequestPermissionsGrantedAppended() {
        val request = PermissionRequest(listOf("CAMERA"), listOf("BLUETOOTH"))
        val channel = Channel<PermissionRequestResult>()
        Mockito.`when`(permissionRequester.resultsChannel).thenReturn(channel)

        val sut = PekoService(context, request, PermissionRationale.none, rationalChecker, permissionRequesterFactory, dispatcher)

        runBlocking {
            async {
                delay(200)
                channel.send(PermissionRequestResult(listOf("BLUETOOTH"), listOf()))
            }
            val result = sut.requestPermissions()
            Assert.assertTrue(result.grantedPermissions.size == 2)
            Assert.assertTrue(result.grantedPermissions.contains("BLUETOOTH"))
            Assert.assertTrue(result.grantedPermissions.contains("CAMERA"))
            Assert.assertTrue(result.deniedPermissions.isEmpty())
        }
    }

    @Test
    fun testRequestPermissionsDenied() {
        val request = PermissionRequest(granted = listOf("CAMERA"), denied = listOf("BLUETOOTH"))
        val channel = Channel<PermissionRequestResult>()
        Mockito.`when`(permissionRequester.resultsChannel).thenReturn(channel)
        Mockito.`when`(rationalChecker.checkIfRationaleShownAlready("BLUETOOTH")).thenReturn(true)

        val sut = PekoService(context, request, PermissionRationale.none, rationalChecker, permissionRequesterFactory, dispatcher)

        runBlocking {
            async {
                delay(200)
                channel.send(PermissionRequestResult(listOf(), listOf("BLUETOOTH")))
            }

            val result = sut.requestPermissions()

            Assert.assertTrue(result.grantedPermissions.size == 1)
            Assert.assertTrue(result.deniedPermissions.contains("BLUETOOTH"))
            Assert.assertTrue(result.grantedPermissions.contains("CAMERA"))
        }
    }

    @Test
    fun testRationaleRequestsPermissionAgain() {
        val request = PermissionRequest(granted = listOf("CAMERA"), denied = listOf("BLUETOOTH"))
        val channel = Channel<PermissionRequestResult>()
        Mockito.`when`(permissionRequester.resultsChannel).thenReturn(channel)
        val rationale = Mockito.mock(PermissionRationale::class.java)
        Mockito.`when`(rationalChecker.checkIfRationaleShownAlready("BLUETOOTH")).thenReturn(false)

        runBlocking {
            Mockito.`when`(rationale.shouldRequestAfterRationaleShownAsync()).thenAnswer {
                GlobalScope.launch {
                    delay(100)
                    channel.send(PermissionRequestResult(grantedPermissions = listOf("BLUETOOTH"), deniedPermissions = listOf()))
                }
                true
            }
        }

        val sut = PekoService(context, request, rationale, rationalChecker, permissionRequesterFactory, dispatcher)

        runBlocking {
            async {
                delay(400)
                channel.send(PermissionRequestResult(grantedPermissions = listOf(), deniedPermissions = listOf("BLUETOOTH")))
            }
            val result = sut.requestPermissions()
        }
        Mockito.verify(permissionRequester, Mockito.times(2)).requestPermissions(arrayOf("BLUETOOTH"))
    }
}