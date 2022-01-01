package com.markodevcic.peko

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito

@RunWith(JUnit4::class)
class PermissionsLiveDataTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val sut = PermissionsLiveData()

    private val owner: LifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

    @Test(expected = IllegalStateException::class)
    fun testThrows_ownerIsNull() {
        sut.checkPermissions("some permission")
    }

    @Test
    fun testObserve_ownerDestroyed() {
        val observer = Observer<PermissionResult> {
            Assert.fail("should not get here")
        }

        Mockito.`when`(owner.lifecycle).thenReturn(DestroyedLifecycle())

        sut.observe(owner, observer)

        sut.checkPermissions("some permission")
    }
}

class DestroyedLifecycle : Lifecycle() {
    override fun addObserver(observer: LifecycleObserver) {
    }

    override fun removeObserver(observer: LifecycleObserver) {
    }

    override fun getCurrentState(): State {
        return State.DESTROYED
    }

}