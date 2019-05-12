package com.markodevcic.samples

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.markodevcic.peko.rationale.PermissionRationale
import kotlinx.android.synthetic.main.fragment_rationale.*
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FragmentRationale : Fragment(), PermissionRationale {

    var startCallback: (() -> Unit)? = null
    private lateinit var continuation: CancellableContinuation<Boolean>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rationale, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        btnRequestAgain.setOnClickListener {
            continuation.resume(true)
            activity?.supportFragmentManager?.popBackStack()
        }
        btnCancel.setOnClickListener {
            continuation.resume(false)
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    override suspend fun shouldRequestAfterRationaleShownAsync(): Boolean {
        return suspendCancellableCoroutine { c ->
            startCallback?.invoke()
            continuation = c
        }
    }
}