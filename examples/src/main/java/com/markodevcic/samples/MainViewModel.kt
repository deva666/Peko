package com.markodevcic.samples

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.markodevcic.peko.PermissionRequester
import com.markodevcic.peko.PermissionResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val permissionRequester: PermissionRequester) : ViewModel() {

	val liveData = MutableLiveData<PermissionResult>()

	private val permissionChannel: Channel<PermissionResult> = Channel()

	val permissionsFlow: Flow<PermissionResult> = permissionChannel.receiveAsFlow()

	fun flowPermissions(vararg permission: String): Flow<PermissionResult> {
		return permissionRequester.flowPermissions(*permission)
	}

	fun requestPermissions(vararg permission: String) {
		viewModelScope.launch {
			permissionRequester.flowPermissions(*permission)
					.onEach {
						liveData.value = it
						permissionChannel.send(it)
					}
					.collect()
		}
	}

	suspend fun isPermissionGranted(permission: String): Boolean {
		return permissionRequester.flowPermissions(permission)
				.first() is PermissionResult.Granted
	}

	override fun onCleared() {
		super.onCleared()
		permissionChannel.close()
	}
}

class MainViewModelFactory(private val requester: PermissionRequester) : ViewModelProvider.Factory {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		return MainViewModel(requester) as T
	}
}