package com.markodevcic.peko

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class PermissionRequesterTest {
	private val requesterFactory = Mockito.mock(NativeRequesterFactory::class.java)
	private val context = Mockito.mock(Context::class.java)
	private val nativeRequester = Mockito.mock(NativeRequester::class.java)
	private val requestBuilder = Mockito.mock(PermissionRequestBuilder::class.java)

	private lateinit var sut: PermissionRequester

	@Before
	fun setup() {
		PermissionRequester.requesterFactory = requesterFactory
		PermissionRequester.requestBuilder = requestBuilder
		PermissionRequester.initialize(context)

		sut = PermissionRequester.instance
	}

	@Test
	fun testGranted() {
		Mockito.`when`(requesterFactory.getRequesterAsync(context)).thenReturn(CompletableDeferred(nativeRequester))
		Mockito.`when`(requestBuilder.createPermissionRequest(context, "CONTACTS")).thenReturn(PermissionRequest(listOf(), listOf("CONTACTS")))
		val permissionChannel = Channel<PermissionResult>()
		Mockito.`when`(nativeRequester.resultsChannel).thenReturn(permissionChannel)

		runBlocking {
			launch {
				delay(200)
				permissionChannel.send(PermissionResult.Granted("CONTACTS"))
				permissionChannel.close()
			}
			Assert.assertTrue(sut.flowPermissions("CONTACTS").allGranted())
		}
	}
}