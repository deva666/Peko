package com.markodevcic.peko

import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel

internal class PekoViewModel : ViewModel() {
    val channel = Channel<PermissionRequestResult>(Channel.UNLIMITED)

    override fun onCleared() {
        super.onCleared()
        channel.close()
    }
}