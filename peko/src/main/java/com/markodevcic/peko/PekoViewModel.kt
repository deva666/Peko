package com.markodevcic.peko

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel

internal class PekoViewModel : ViewModel() {
    val channel = Channel<PermissionRequestResult>(Channel.UNLIMITED)

    override fun onCleared() {
        super.onCleared()
        channel.close()
    }
}