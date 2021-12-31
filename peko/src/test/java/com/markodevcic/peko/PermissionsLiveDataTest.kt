package com.markodevcic.peko

import androidx.lifecycle.LifecycleOwner
import org.junit.Test
import org.mockito.Mockito

class PermissionsLiveDataTest {
    val sut = PermissionsLiveData()
    private val owner: LifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

    @Test(expected = IllegalStateException::class)
    fun testThrows_ownerIsNull() {
        sut.checkPermissions("some permission")
    }

}