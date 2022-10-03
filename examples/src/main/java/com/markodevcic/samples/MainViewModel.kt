package com.markodevcic.samples

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markodevcic.peko.PekoRequester
import com.markodevcic.peko.PermissionResult
import com.markodevcic.peko.PermissionResults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
	val pekoRequester = PekoRequester()

	val liveData = MutableLiveData<PermissionResult>()
	fun requestPermissions(vararg permission: String) {
		viewModelScope.launch {
			pekoRequester.flowPermissions(*permission)
				.onEach { liveData.value = it }
				.collect()
		}
	}
}