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

	private lateinit var permissionChannel: Channel<PermissionResult>

	private lateinit var sut: PermissionRequester

	@Before
	fun setup() {
		permissionChannel = Channel()

		PermissionRequester.requesterFactory = requesterFactory
		PermissionRequester.requestBuilder = requestBuilder
		PermissionRequester.initialize(context)

		Mockito.`when`(requesterFactory.getRequesterAsync(context)).thenReturn(CompletableDeferred(nativeRequester))
		Mockito.`when`(nativeRequester.resultsChannel).thenReturn(permissionChannel)

		sut = PermissionRequester.instance
	}

	@Test
	fun testGranted() {
		val permission = "CONTACTS"

		Mockito.`when`(requestBuilder.createPermissionRequest(context, permission)).thenReturn(
			PermissionRequest(
				listOf(), listOf(
					permission
				)
			)
		)


		runBlocking {
			launch {
				delay(200)
				permissionChannel.send(PermissionResult.Granted(permission))
				permissionChannel.close()
			}
			Assert.assertTrue(sut.flowPermissions(permission).allGranted())
		}
	}
}