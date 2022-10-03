package com.markodevcic.samples

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.markodevcic.peko.PekoPermissonRequester
import com.markodevcic.peko.PermissionResult
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

	private lateinit var viewModel: MainViewModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		PekoPermissonRequester.initialize(applicationContext)
		viewModel = ViewModelProvider(this@MainActivity).get(MainViewModel::class.java)
		setContentView(R.layout.activity_main)
		setSupportActionBar(toolbar)
		viewModel.liveData.observe(this) {
			setResult(it)
		}

		btnContacts.setOnClickListener {
			requestPermission(Manifest.permission.READ_CONTACTS)
		}
		btnFineLocation.setOnClickListener {
			requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
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
				Manifest.permission.ACCESS_COARSE_LOCATION,
				Manifest.permission.READ_CONTACTS
			)
		}
	}

	private fun requestPermission(vararg permissions: String) {
		viewModel.requestPermissions(*permissions)
	}

	private fun setResult(result: PermissionResult) {
		if (result is PermissionResult.Granted) {

			val granted = "GRANTED"
			if (Manifest.permission.ACCESS_COARSE_LOCATION == result.permission) {
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
			if (Manifest.permission.ACCESS_COARSE_LOCATION == result.permission) {
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
		}
	}

	private fun deniedReasonText(result: PermissionResult): String {
		return when (result) {
			is PermissionResult.Denied.NeedsRationale -> "NEEDS RATIONALE"
			is PermissionResult.Denied.PermanentlyDenied -> "DENIED PERMANENTLY"
			is PermissionResult.Denied.JustDenied -> "JUST DENIED"
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
