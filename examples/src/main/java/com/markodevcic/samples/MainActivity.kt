package com.markodevcic.samples

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.markodevcic.peko.PermissionRequester
import com.markodevcic.peko.PermissionResult
import com.markodevcic.peko.PermissionState
import com.markodevcic.peko.allGranted
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

	private lateinit var viewModel: MainViewModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		PermissionRequester.initialize(applicationContext)

		viewModel = ViewModelProvider(
				this@MainActivity,
				MainViewModelFactory(PermissionRequester.instance())
		)[MainViewModel::class.java]

		setContentView(R.layout.activity_main)
		setSupportActionBar(toolbar)

		lifecycleScope.launchWhenStarted {
			viewModel.permissionsFlow
					.collect { setResult(it) }
		}

        lifecycleScope.launchWhenStarted {
            viewModel.permissionStateFlow.collect {
                    setState(it)
            }
        }

		btnContacts.setOnClickListener {
			requestPermission(Manifest.permission.READ_CONTACTS)
		}
		btnFineLocation.setOnClickListener {
			requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
		}
		btnFile.setOnClickListener {
			requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
		}
		btnCamera.setOnClickListener {
			requestPermission(Manifest.permission.CAMERA)
		}
		btnAll.setOnClickListener {
			viewModel.requestPermissions(
					Manifest.permission.WRITE_EXTERNAL_STORAGE,
					Manifest.permission.CAMERA,
					Manifest.permission.ACCESS_FINE_LOCATION,
					Manifest.permission.READ_CONTACTS
			)
        }

        btnContactsState.setOnClickListener {
            viewModel.permissionState(Manifest.permission.READ_CONTACTS)
        }
        btnFineLocationState.setOnClickListener {
            viewModel.permissionState(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        btnFileState.setOnClickListener {
            viewModel.permissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        btnCameraState.setOnClickListener {
            viewModel.permissionState(Manifest.permission.CAMERA)
        }
        btnAllSates.setOnClickListener {
            viewModel.permissionState(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_CONTACTS
            )
        }
    }

	private fun checkAllGranted(vararg permissions: String) {
		lifecycleScope.launch {
			val allGranted = viewModel.flowPermissions(*permissions).allGranted()
		}
	}

	private fun requestPermission(vararg permissions: String) {
		viewModel.requestPermissions(*permissions)
	}

    private fun setResult(result: PermissionResult) {
        if (result is PermissionResult.Granted) {

            val granted = "GRANTED"
            if (Manifest.permission.ACCESS_FINE_LOCATION == result.permission) {
                textLocationResult.text = granted
                textLocationResult.setTextColor(Color.GREEN)
            }
            if (Manifest.permission.WRITE_EXTERNAL_STORAGE == result.permission) {
                textFileResult.text = granted
                textFileResult.setTextColor(Color.GREEN)
            }
            if (Manifest.permission.CAMERA == result.permission) {
                textCameraResult.text = granted
                textCameraResult.setTextColor(Color.GREEN)
            }
            if (Manifest.permission.READ_CONTACTS == result.permission) {
                textContactsResult.text = granted
                textContactsResult.setTextColor(Color.GREEN)
            }
        } else if (result is PermissionResult.Denied) {
            if (Manifest.permission.ACCESS_BACKGROUND_LOCATION == result.permission) {
                textLocationResult.text = deniedReasonText(result)
                textLocationResult.setTextColor(Color.RED)
            }
            if (Manifest.permission.WRITE_EXTERNAL_STORAGE == result.permission) {
                textFileResult.text = deniedReasonText(result)
                textFileResult.setTextColor(Color.RED)
            }
            if (Manifest.permission.CAMERA == result.permission) {
                textCameraResult.text = deniedReasonText(result)
                textCameraResult.setTextColor(Color.RED)
            }
            if (Manifest.permission.READ_CONTACTS == result.permission) {
                textContactsResult.text = deniedReasonText(result)
                textContactsResult.setTextColor(Color.RED)
            }
        } else if (result is PermissionResult.Cancelled) {
            textLocationResult.text = cancelled
            textLocationResult.setTextColor(Color.RED)
            textFileResult.text = cancelled
            textFileResult.setTextColor(Color.RED)
            textCameraResult.text = cancelled
            textCameraResult.setTextColor(Color.RED)
            textContactsResult.text = cancelled
            textContactsResult.setTextColor(Color.RED)
        }
    }

    private fun setState(result: PermissionState) {
        if (result is PermissionState.Granted) {

            val granted = "GRANTED"
            if (Manifest.permission.ACCESS_FINE_LOCATION == result.permission) {
                textLocationState.text = granted
                textLocationState.setTextColor(Color.GREEN)
            }
            else if (Manifest.permission.WRITE_EXTERNAL_STORAGE == result.permission) {
                textFileState.text = granted
                textFileState.setTextColor(Color.GREEN)
            }
            else if (Manifest.permission.CAMERA == result.permission) {
                textCameraState.text = granted
                textCameraState.setTextColor(Color.GREEN)
            }
            else if (Manifest.permission.READ_CONTACTS == result.permission) {
                textContactsState.text = granted
                textContactsState.setTextColor(Color.GREEN)
            }
        } else if (result is PermissionState.NeedsRationale) {
            val denied = "NEEDS RATIONALE"
            if (Manifest.permission.ACCESS_FINE_LOCATION == result.permission) {
                textLocationState.text = denied
                textLocationState.setTextColor(Color.RED)
            }
            else if (Manifest.permission.WRITE_EXTERNAL_STORAGE == result.permission) {
                textFileState.text = denied
                textFileState.setTextColor(Color.RED)
            }
            else if (Manifest.permission.CAMERA == result.permission) {
                textCameraState.text = denied
                textCameraState.setTextColor(Color.RED)
            }
            else if (Manifest.permission.READ_CONTACTS == result.permission) {
                textContactsState.text = denied
                textContactsState.setTextColor(Color.RED)
            }
        } else if (result is PermissionState.NeverAskedOrDeniedPermanently) {
            val condition = "Never Asked Or Denied Permanently"

            if (Manifest.permission.ACCESS_FINE_LOCATION == result.permission) {
                textLocationState.text = condition
                textLocationState.setTextColor(Color.BLACK)
            }
            else if (Manifest.permission.WRITE_EXTERNAL_STORAGE == result.permission) {
                textFileState.text = condition
                textFileState.setTextColor(Color.BLACK)
            }
            else if (Manifest.permission.CAMERA == result.permission) {
                textCameraState.text = condition
                textCameraState.setTextColor(Color.BLACK)
            }
            else  if (Manifest.permission.READ_CONTACTS == result.permission) {
                textContactsState.text = condition
                textContactsState.setTextColor(Color.BLACK)
            }

        }
    }

	private fun deniedReasonText(result: PermissionResult): String {
		return when (result) {
			is PermissionResult.Denied.NeedsRationale -> "NEEDS RATIONALE"
			is PermissionResult.Denied.DeniedPermanently -> "DENIED PERMANENTLY"
			else -> ""
		}
	}


	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.menu_main, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_settings -> true
			else -> super.onOptionsItemSelected(item)
		}
	}
}

private const val cancelled = "CANCELLED"