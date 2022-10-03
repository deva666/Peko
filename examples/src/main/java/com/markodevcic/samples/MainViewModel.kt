package com.markodevcic.samples

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.markodevcic.peko.IPermissionRequester
import com.markodevcic.peko.PermissionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainViewModel(val permissionRequester: IPermissionRequester) : ViewModel() {

	val liveData = MutableLiveData<PermissionResult>()

	fun flowPermissions(vararg permission: String): Flow<PermissionResult> {
		return permissionRequester.flowPermissions(*permission)
	}

	fun requestPermissions(vararg permission: String) {
		viewModelScope.launch {
			permissionRequester.flowPermissions(*permission)
				.onEach { liveData.value = it }
				.collect()
		}
	}

	suspend fun isPermissionGranted(permission: String): Boolean {
		return permissionRequester.flowPermissions(permission)
			.first() is PermissionResult.Granted
	}
}

class MainViewModelFactory(private val requester: IPermissionRequester): ViewModelProvider.Factory {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		return MainViewModel(requester) as T
	}
}