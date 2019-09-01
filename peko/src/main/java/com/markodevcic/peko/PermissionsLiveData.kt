package com.markodevcic.peko

import android.app.Activity
import androidx.core.app.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class PermissionsLiveData : LiveData<PermissionResult>() {

	private val requester = LiveDataRequester(this)

	override fun observe(owner: LifecycleOwner, observer: Observer<in PermissionResult>) {
		super.observe(owner, observer)
		requester.onObserve(owner)
	}

	fun checkPermissions(vararg permissions: String) {
		requester.checkPermissions(*permissions)
	}

	fun postResult(value: PermissionResult) {
		super.postValue(value)
	}
}

private class LiveDataRequester(private val liveData: PermissionsLiveData) {
	private lateinit var lifecycleOwnerScope: LifecycleOwnerScope
	var owner: LifecycleOwner? = null

	fun checkPermissions(vararg permissions: String) {
		val ownerCopy = owner ?: throw IllegalStateException("Lifecycle owner not registered")
		if (ownerCopy.lifecycle.currentState == Lifecycle.State.DESTROYED) {
			return
		}
		else if (!Peko.isRequestInProgress()) {
			when (ownerCopy) {
				is ComponentActivity,
				is Fragment -> {
					lifecycleOwnerScope = LifecycleOwnerScope(ownerCopy) { owner = null }
					lifecycleOwnerScope.launch {
						val activity = if (ownerCopy is Fragment)
							(ownerCopy.activity
									?: throw IllegalStateException("Fragment should be in a state" +
											" where activity is not null"))
						else ownerCopy as Activity
						val result = Peko.requestPermissionsAsync(activity, *permissions)
						liveData.postResult(result)
					}
				}
				else -> throw IllegalArgumentException("Unsupported lifecycle owner")
			}
		} else {
			resumeRequest(ownerCopy)
		}
	}

	fun onObserve(owner: LifecycleOwner) {
		this.owner = owner
		if (Peko.isRequestInProgress()) {
			resumeRequest(owner)
		}
	}

	private fun resumeRequest(owner: LifecycleOwner) {
		lifecycleOwnerScope = LifecycleOwnerScope(owner) { this.owner = null }
		lifecycleOwnerScope.launch {
			val result = Peko.resumeRequest()
			liveData.postResult(result)
		}
	}
}

private class LifecycleOwnerScope(private val lifecycleOwner: LifecycleOwner,
								  private val onDestroyCallback: () -> Unit) : CoroutineScope {
	private val job = Job()
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main + job

	init {
		val observer = object : LifecycleObserver {

			@OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
			fun onDestroy() {
				if (isChangingConfig()) {
					job.cancel(ActivityRotatingException())
				} else {
					job.cancel()
				}
				onDestroyCallback()
				lifecycleOwner.lifecycle.removeObserver(this)
			}

			private fun isChangingConfig(): Boolean {
				return when (lifecycleOwner) {
					is Activity -> lifecycleOwner.isChangingConfigurations
					is Fragment -> lifecycleOwner.activity?.isChangingConfigurations == true
					else -> false
				}
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
	}
}