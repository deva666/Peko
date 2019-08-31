package com.markodevcic.samples

import android.Manifest
import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.markodevcic.peko.PermissionResult
import kotlinx.android.synthetic.main.activity_live_data.*

class LiveDataActivity : AppCompatActivity() {

	private lateinit var viewModel: LiveDataViewModel

	@TargetApi(Build.VERSION_CODES.O)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_live_data)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		viewModel = ViewModelProviders.of(this).get(LiveDataViewModel::class.java)

		viewModel.permissionLiveData.observe(this, Observer { r: PermissionResult ->
			textContactsResult.text = when (r) {
				is PermissionResult.Granted -> "GRANTED"
				is PermissionResult.Denied -> if (r.deniedPermissions.contains(Manifest
								.permission.READ_CONTACTS)) "DENIED" else ""
				else -> ""
			}
			textPhoneResult.text = when (r) {
				is PermissionResult.Granted -> "GRANTED"
				is PermissionResult.Denied -> if (r.deniedPermissions.contains(Manifest
								.permission.ANSWER_PHONE_CALLS)) "DENIED" else ""
				else -> ""
			}
		})

		btnContacts.setOnClickListener {
			viewModel.checkPermissions(Manifest.permission.READ_CONTACTS)
		}

		btnPhone.setOnClickListener {
			viewModel.checkPermissions(Manifest.permission.ANSWER_PHONE_CALLS)
		}

		btnAll.setOnClickListener {
			viewModel.checkPermissions(Manifest
					.permission.READ_CONTACTS, Manifest
					.permission.ANSWER_PHONE_CALLS)
		}
	}

}
