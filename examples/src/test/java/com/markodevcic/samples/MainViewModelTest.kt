package com.markodevcic.samples

import com.markodevcic.peko.PermissionRequester
import com.markodevcic.peko.PermissionResult
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito

class MainViewModelTest {

	private val requester = Mockito.mock(PermissionRequester::class.java)

	private val sut = MainViewModel(requester)

	@Test
	fun testSingleGranted() {
		Mockito.`when`(requester.request("A")).thenReturn(flowOf(PermissionResult.Granted("A")))
		Mockito.`when`(requester.request("B")).thenReturn(flowOf(PermissionResult.Denied.DeniedPermanently("B")))

		runBlocking {
			assert(sut.isPermissionGranted("A"))
			assert(!sut.isPermissionGranted("B"))
		}
	}
}