package com.markodevcic.samples

import android.Manifest
import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.markodevcic.peko.PermissionResults
import kotlinx.android.synthetic.main.activity_live_data.*

class LiveDataActivity : AppCompatActivity() {

    private lateinit var viewModel: LiveDataViewModel

    @TargetApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel = ViewModelProvider(this).get(LiveDataViewModel::class.java)

//        viewModel.permissionLiveData.observe(this) { r: PermissionResults ->
//            textContactsResult.text = when (r) {
//                is PermissionResults.Cancelled -> "CANCELLED"
//                is PermissionResults.Granted -> "GRANTED"
//                is PermissionResults.Denied -> if (r.deniedReasons.contains(Manifest
//                        .permission.READ_CONTACTS)) "DENIED" else ""
//                else -> ""
//            }
//            textPhoneResult.text = when (r) {
//                is PermissionResults.Cancelled -> "CANCELLED"
//                is PermissionResults.Granted -> "GRANTED"
//                is PermissionResults.Denied -> if (r.deniedReasons.contains(Manifest
//                        .permission.ANSWER_PHONE_CALLS)) "DENIED" else ""
//                else -> ""
//            }
//        }

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
