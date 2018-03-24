package com.markodevcic.peko

import android.content.Context
import android.content.SharedPreferences
import com.markodevcic.peko.rationale.PermissionRationale
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.mockito.Mockito

class PekoServiceTest {

	private val context = Mockito.mock(Context::class.java)
	private val sharedPrefs = Mockito.mock(SharedPreferences::class.java)
	private val permissionRequesterFactory = Mockito.mock(PermissionRequesterFactory::class.java)
	private val dispatcher = Unconfined
	private val permissionRequester = Mockito.mock(PermissionRequester::class.java)


	@Before
	fun setup() {
		Mockito.`when`(permissionRequesterFactory.getRequester(Mockito.any(Context::class.java))).thenReturn(CompletableDeferred(permissionRequester))
	}


	fun testRequestPermissions() {
		val request = PermissionRequest(listOf(), listOf("BLUETOOTH"))
		val channel = Channel<PermissionRequestResult>()
		Mockito.`when`(permissionRequester.resultsChannel).thenReturn(channel)
		val sut = PekoService(context, request, PermissionRationale.EMPTY, sharedPrefs, permissionRequesterFactory, dispatcher)
		sut.requestPermissions()
		runBlocking {
			channel.send(PermissionRequestResult(listOf("BLUETOOTH"), listOf()))
		}
	}

}