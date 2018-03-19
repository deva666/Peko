package com.markodevcic.samples

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.markodevcic.peko.Peko
import com.markodevcic.peko.PermissionRequestResult
import com.markodevcic.peko.rationale.AlertDialogPermissionRationale
import com.markodevcic.peko.rationale.SnackBarRationale
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class MainActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		setSupportActionBar(toolbar)

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
			requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
		}
		btnAllSnackBarRationale.setOnClickListener {
			requestPermissionWithSnackBarRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
		}
	}

	private fun requestPermission(vararg permissions: String) {
		launch(UI) {
			val rationale = AlertDialogPermissionRationale(this@MainActivity) {
				this.setTitle("Need permissions")
				this.setMessage("Please give permissions to use this feature")
			}
			val result = Peko.requestPermissionsAsync(this@MainActivity, *permissions, rationale = rationale).await()
			setResults(result)
		}
	}

	private fun requestPermissionWithSnackBarRationale(vararg permissions: String) {
		val snackBar = Snackbar.make(rootView, "Permissions needed to continue", Snackbar.LENGTH_LONG)
		val snackBarRationale = SnackBarRationale(snackBar, "Request again")
		launch(UI) {
			val result = Peko.requestPermissionsAsync(this@MainActivity, *permissions, rationale = snackBarRationale).await()
			setResults(result)
		}
	}

	private fun setResults(result: PermissionRequestResult) {
		if (result.grantedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
			textLocationResult.text = "GRANTED"
			textLocationResult.setTextColor(Color.GREEN)
		}
		if (result.grantedPermissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			textFileResult.text = "GRANTED"
			textFileResult.setTextColor(Color.GREEN)
		}
		if (result.grantedPermissions.contains(Manifest.permission.CAMERA)) {
			textCameraResult.text = "GRANTED"
			textCameraResult.setTextColor(Color.GREEN)
		}

		if (result.deniedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
			textLocationResult.text = "DENIED"
			textLocationResult.setTextColor(Color.RED)
		}
		if (result.deniedPermissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			textFileResult.text = "DENIED"
			textFileResult.setTextColor(Color.RED)
		}
		if (result.deniedPermissions.contains(Manifest.permission.CAMERA)) {
			textCameraResult.text = "DENIED"
			textCameraResult.setTextColor(Color.RED)
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
