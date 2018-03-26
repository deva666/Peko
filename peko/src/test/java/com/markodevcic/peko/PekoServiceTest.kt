package com.markodevcic.peko

import android.content.Context
import android.content.SharedPreferences
import com.markodevcic.peko.rationale.PermissionRationale
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.Executors

class PekoServiceTest {

	private val context = Mockito.mock(Context::class.java)
	private val sharedPrefs = Mockito.mock(SharedPreferences::class.java)
	private val permissionRequesterFactory = Mockito.mock(PermissionRequesterFactory::class.java)
	private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
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

		val sut = PekoService(context, request, PermissionRationale.EMPTY, sharedPrefs, permissionRequesterFactory, dispatcher)
		val deferred = sut.requestPermissions()

		runBlocking {
			channel.send(PermissionRequestResult(listOf("BLUETOOTH"), listOf()))
			val result = deferred.await()
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

		val sut = PekoService(context, request, PermissionRationale.EMPTY, sharedPrefs, permissionRequesterFactory, dispatcher)
		val deferred = sut.requestPermissions()

		runBlocking {
			channel.send(PermissionRequestResult(listOf("BLUETOOTH"), listOf()))
			val result = deferred.await()
			Assert.assertTrue(result.grantedPermissions.size == 2)
			Assert.assertTrue(result.grantedPermissions.contains("BLUETOOTH"))
			Assert.assertTrue(result.grantedPermissions.contains("CAMERA"))
			Assert.assertTrue(result.deniedPermissions.isEmpty())
		}
	}

	@Test
	fun testRequestPermissionsDenied() {
		val request = PermissionRequest(listOf("CAMERA"), listOf("BLUETOOTH"))
		val channel = Channel<PermissionRequestResult>()
		Mockito.`when`(permissionRequester.resultsChannel).thenReturn(channel)
		Mockito.`when`(sharedPrefs.getStringSet(RATIONALE_SHOWED_SET_KEY, setOf())).thenReturn(setOf("BLUETOOTH"))

		val sut = PekoService(context, request, PermissionRationale.EMPTY, sharedPrefs, permissionRequesterFactory, dispatcher)
		val deferred = sut.requestPermissions()

		runBlocking {
			channel.send(PermissionRequestResult(listOf(), listOf("BLUETOOTH")))
			val result = deferred.await()
			Assert.assertTrue(result.grantedPermissions.size == 1)
			Assert.assertTrue(result.deniedPermissions.contains("BLUETOOTH"))
			Assert.assertTrue(result.grantedPermissions.contains("CAMERA"))
		}
	}
}