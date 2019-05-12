package com.markodevcic.samples

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.markodevcic.peko.ActivityRotatingException
import com.markodevcic.peko.Peko
import com.markodevcic.peko.PermissionResult
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    private var job = CompletableDeferred<Any>()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (Peko.isRequestInProgress()) {
            launch {
                setResults(Peko.resumeRequest())
            }
        }

        btnFineLocation.setOnClickListener {
            clearResults()
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        btnFile.setOnClickListener {
            clearResults()
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        btnCamera.setOnClickListener {
            clearResults()
            requestPermission(Manifest.permission.CAMERA)
        }
        btnAll.setOnClickListener {
            clearResults()
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestPermission(vararg permissions: String) {
        launch {
            val result = Peko.requestPermissionsAsync(this@MainActivity, *permissions)
            setResults(result)
        }
    }

    private fun setResults(result: PermissionResult) {
        Log.d("Peko", "PermissionResult: $result")
        if (result is PermissionResult.Granted) {

            if (Manifest.permission.ACCESS_FINE_LOCATION in result.grantedPermissions) {
                textLocationResult.text = "GRANTED"
                textLocationResult.setTextColor(Color.GREEN)
            }
            if (Manifest.permission.WRITE_EXTERNAL_STORAGE in result.grantedPermissions) {
                textFileResult.text = "GRANTED"
                textFileResult.setTextColor(Color.GREEN)
            }
            if (Manifest.permission.CAMERA in result.grantedPermissions) {
                textCameraResult.text = "GRANTED"
                textCameraResult.setTextColor(Color.GREEN)
            }
        } else if (result is PermissionResult.Denied) {
            if (Manifest.permission.ACCESS_FINE_LOCATION in result.deniedPermissions) {
                textLocationResult.text = "DENIED"
                textLocationResult.setTextColor(Color.RED)
            }
            if (Manifest.permission.WRITE_EXTERNAL_STORAGE in result.deniedPermissions) {
                textFileResult.text = "DENIED"
                textFileResult.setTextColor(Color.RED)
            }
            if (Manifest.permission.CAMERA in result.deniedPermissions) {
                textCameraResult.text = "DENIED"
                textCameraResult.setTextColor(Color.RED)
            }
        }
    }

    private fun clearResults() {
        textCameraResult.text = ""
        textFileResult.text = ""
        textLocationResult.text = ""
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

    override fun onDestroy() {
        super.onDestroy()
        if (isChangingConfigurations) {
            job.completeExceptionally(ActivityRotatingException())
        } else {
            job.cancel()
        }
    }
}
