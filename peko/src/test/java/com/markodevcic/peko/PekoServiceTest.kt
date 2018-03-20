package com.markodevcic.peko

import android.content.Context
import android.content.SharedPreferences
import com.markodevcic.peko.rationale.PermissionRationale
import org.junit.Test
import org.mockito.Mockito

class PekoServiceTest {

	private val context = Mockito.mock(Context::class.java)
	private val sharedPrefs = Mockito.mock(SharedPreferences::class.java)
	private val permissionRequesterFactory = Mockito.mock(PermissionRequesterFactory::class.java)

	@Test
	fun testRequestPermissions() {
		val request = PermissionRequest(listOf(), listOf("BLUETOOTH"))
		val sut = PekoService(context, request, PermissionRationale.EMPTY, sharedPrefs, permissionRequesterFactory)

		sut.requestPermissions()
	}

}