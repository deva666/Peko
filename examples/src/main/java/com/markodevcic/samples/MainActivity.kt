package com.markodevcic.samples

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.markodevcic.peko.ActivityRotatingException
import com.markodevcic.peko.Peko
import com.markodevcic.peko.PermissionRequestResult
import com.markodevcic.peko.rationale.AlertDialogPermissionRationale
import com.markodevcic.peko.rationale.SnackBarRationale
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    private var job = CompletableDeferred<Any>()

    private var snackBarRationale: SnackBarRationale? = null

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
        btnAllSnackBarRationale.setOnClickListener {
            clearResults()
            requestPermissionWithSnackBarRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
        }
        btnFragmentRationale.setOnClickListener {
            clearResults()
            requestFileWithFragmentRationale()
        }
    }

    private fun requestPermission(vararg permissions: String) {
        clearRationaleSharedPrefs()
        launch {
            val rationale = AlertDialogPermissionRationale(this@MainActivity) {
                this.setTitle("Need permissions")
                this.setMessage("Please give permissions to use this feature")
            }
            val result = Peko.requestPermissionsAsync(this@MainActivity, *permissions, rationale = rationale)
            setResults(result)
        }
    }

    private fun requestPermissionWithSnackBarRationale(vararg permissions: String) {
        clearRationaleSharedPrefs()
        val snackBar = Snackbar.make(rootView, "Permissions needed to continue", Snackbar.LENGTH_LONG)
        snackBarRationale = SnackBarRationale(snackBar, "Request again")
        launch {
            val result = Peko.requestPermissionsAsync(this@MainActivity, *permissions, rationale = snackBarRationale!!)
            setResults(result)
        }
    }

    private fun requestFileWithFragmentRationale() {
        clearRationaleSharedPrefs()
        val fragmentRationale = FragmentRationale()
        fragmentRationale.startCallback = {
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, fragmentRationale)
                    .addToBackStack("fr")
                    .commit()
        }

        launch {
            val result = Peko.requestPermissionsAsync(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE, rationale = fragmentRationale)
            setResults(result)
        }
    }

    private fun clearRationaleSharedPrefs() {
        this.getSharedPreferences("PekoSharedPrefs", Context.MODE_PRIVATE)
                .edit()
                .remove("RATIONALE_SHOWED_SET_KEY")
                .apply()

    }

    private fun setResults(result: PermissionRequestResult) {
        val (grantedPermissions, deniedPermissions) = result

        if (Manifest.permission.ACCESS_FINE_LOCATION in grantedPermissions) {
            textLocationResult.text = "GRANTED"
            textLocationResult.setTextColor(Color.GREEN)
        }
        if (Manifest.permission.WRITE_EXTERNAL_STORAGE in grantedPermissions) {
            textFileResult.text = "GRANTED"
            textFileResult.setTextColor(Color.GREEN)
        }
        if (Manifest.permission.CAMERA in grantedPermissions) {
            textCameraResult.text = "GRANTED"
            textCameraResult.setTextColor(Color.GREEN)
        }

        if (Manifest.permission.ACCESS_FINE_LOCATION in deniedPermissions) {
            textLocationResult.text = "DENIED"
            textLocationResult.setTextColor(Color.RED)
        }
        if (Manifest.permission.WRITE_EXTERNAL_STORAGE in deniedPermissions) {
            textFileResult.text = "DENIED"
            textFileResult.setTextColor(Color.RED)
        }
        if (Manifest.permission.CAMERA in deniedPermissions) {
            textCameraResult.text = "DENIED"
            textCameraResult.setTextColor(Color.RED)
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
            snackBarRationale?.cancel()
            job.completeExceptionally(ActivityRotatingException())
        } else {
            job.cancel()
        }
    }
}
