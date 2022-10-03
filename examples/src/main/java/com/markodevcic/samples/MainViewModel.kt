package com.markodevcic.samples

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markodevcic.peko.PekoPermissionRequester
import com.markodevcic.peko.PermissionResult
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
	val pekoRequester = PekoPermissionRequester()

	val liveData = MutableLiveData<PermissionResult>()
	fun requestPermissions(vararg permission: String) {
		viewModelScope.launch {
			pekoRequester.flowPermissions(*permission)
				.onEach { liveData.value = it }
				.collect()
		}
	}
}