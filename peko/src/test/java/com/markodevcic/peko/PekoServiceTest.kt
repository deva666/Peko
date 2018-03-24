package com.markodevcic.peko

import android.content.Context
import android.content.SharedPreferences
import com.markodevcic.peko.rationale.PermissionRationale
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class PekoServiceTest {

	private val context = Mockito.mock(Context::class.java)
	private val sharedPrefs = Mockito.mock(SharedPreferences::class.java)
	private val permissionRequesterFactory = Mockito.mock(PermissionRequesterFactory::class.java)
	private val dispatcher = Unconfined
	private val permissionRequester = Mockito.mock(PermissionRequester::class.java)


	@Before
	fun setup() {
		Mockito.`when`(permissionRequesterFactory.getRequester(context)).thenReturn(CompletableDeferred(permissionRequester))
	}

	@Test
	fun testRequestPermissions() {
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

}