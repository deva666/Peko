package com.markodevcic.samples

import androidx.lifecycle.ViewModel
import com.markodevcic.peko.PermissionsLiveData

class LiveDataViewModel : ViewModel() {
	val permissionLiveData = PermissionsLiveData()

	fun checkPermissions(vararg permissions: String) {
		permissionLiveData.checkPermissions(*permissions)
	}
}