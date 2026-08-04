package com.markodevcic.peko

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class PermissionRequesterTest {
	private val activityFactory = Mockito.mock(NativeActivityFactory::class.java)
	private val context = Mockito.mock(Context::class.java)
	private val nativeActivity = Mockito.mock(NativeActivity::class.java)
	private val permissionGrouper = Mockito.mock(PermissionGrouper::class.java)

	private lateinit var sut: PermissionRequester

	@Before
	fun setup() {
		PermissionRequester.activityFactory = activityFactory
		PermissionRequester.permissionGrouper = permissionGrouper
		PermissionRequester.initialize(context)

		Mockito.`when`(activityFactory.getActivityAsync(context)).thenReturn(CompletableDeferred(nativeActivity))

		sut = PermissionRequester.instance()
	}

	@Test
	fun testGranted() {
		val permission = "CONTACTS"

		Mockito.`when`(permissionGrouper.group(context, permission)).thenReturn(
			PermissionGroup(
				listOf(), listOf(
					permission
				)
			)
		)

		Mockito.doAnswer { invocation ->
			val channel = invocation.getArgument<Pair<Int, kotlinx.coroutines.channels.Channel<PermissionResult>>>(1).second
			channel.trySend(PermissionResult.Granted(permission))
			channel.close()
			null
		}.`when`(nativeActivity).requestPermissions(Mockito.any(), Mockito.any())

		runBlocking {
			Assert.assertTrue(sut.request(permission).allGranted())
		}
	}

	@Test
	fun testAlreadyGranted() {
		val permission = "CONTACTS"

		Mockito.`when`(permissionGrouper.group(context, permission)).thenReturn(
			PermissionGroup(
				listOf(permission), listOf()
			)
		)


		runBlocking {
			Assert.assertTrue(sut.request(permission).allGranted())
		}
	}

	@Test
	fun testRequestChecksPermissionStateOnCollection() {
		val permission = "CONTACTS"
		val result = sut.request(permission)

		Mockito.verifyNoInteractions(permissionGrouper)
		Mockito.`when`(permissionGrouper.group(context, permission)).thenReturn(
			PermissionGroup(listOf(permission), listOf())
		)

		runBlocking {
			Assert.assertTrue(result.allGranted())
		}
	}

	@Test
	fun testRequestFinishesActivityWhenCollectionIsCancelled() {
		val permission = "CONTACTS"
		val requestStarted = CompletableDeferred<Unit>()
		Mockito.`when`(permissionGrouper.group(context, permission)).thenReturn(
			PermissionGroup(listOf(), listOf(permission))
		)
		Mockito.doAnswer {
			requestStarted.complete(Unit)
			null
		}.`when`(nativeActivity).requestPermissions(Mockito.any(), Mockito.any())

		runBlocking {
			val job = launch { sut.request(permission).collect {} }
			requestStarted.await()
			job.cancelAndJoin()
		}

		Mockito.verify(nativeActivity).finish()
	}

	@Test
	fun testCheckPermissionsState() {
		val permission = "CONTACTS"
		Mockito.`when`(permissionGrouper.group(context, permission)).thenReturn(
			PermissionGroup(listOf(), listOf(permission))
		)
		Mockito.doAnswer { invocation ->
			val channel = invocation.getArgument<Channel<PermissionState>>(1)
			channel.trySend(PermissionState.NeedsRationale(permission))
			channel.close()
			null
		}.`when`(nativeActivity).checkStateOfDeniedPermissions(Mockito.any(), Mockito.any())

		val states = runBlocking {
			sut.checkPermissionsState(permission).toList()
		}

		Assert.assertEquals(listOf(PermissionState.NeedsRationale(permission)), states)
		Mockito.verify(nativeActivity).finish()
	}

	@Test
	fun testAnyGranted() {
		val denied = "COARSE_LOCATION"
		val granted = "FUSED_LOCATION"

		Mockito.`when`(permissionGrouper.group(context, denied, granted)).thenReturn(
			PermissionGroup(
				granted = listOf(granted),
				denied = listOf(denied)
			)
		)

		val isAnyGranted = sut.isAnyGranted(denied, granted)

		Assert.assertTrue(isAnyGranted)
	}

	@Test
	fun testAreGranted() {
		val denied = "COARSE_LOCATION"
		val granted = "FUSED_LOCATION"

		Mockito.`when`(permissionGrouper.group(context, denied, granted)).thenReturn(
			PermissionGroup(
				granted = listOf(granted),
				denied = listOf(denied)
			)
		)

		val areGranted = sut.areGranted(denied, granted)

		Assert.assertFalse(areGranted)
	}
}
