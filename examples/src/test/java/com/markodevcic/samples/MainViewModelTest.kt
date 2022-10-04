package com.markodevcic.samples

import com.markodevcic.peko.IPermissionRequester
import com.markodevcic.peko.PermissionResult
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito

class MainViewModelTest {

	private val requester = Mockito.mock(IPermissionRequester::class.java)

	private val sut = MainViewModel(requester)

	@Test
	fun testSingleGranted() {
		Mockito.`when`(requester.flowPermissions("A")).thenReturn(flowOf(PermissionResult.Granted("A")))
		Mockito.`when`(requester.flowPermissions("B")).thenReturn(flowOf(PermissionResult.Denied.JustDenied("B")))

		runBlocking {
			assert(sut.isPermissionGranted("A"))
			assert(!sut.isPermissionGranted("B"))
		}
	}
}