package com.markodevcic.peko

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap

internal class PekoViewModel : ViewModel() {
	private val channels = ConcurrentHashMap<Int, Channel<PermissionResult>>()

	fun putChannel(channel: Pair<Int, Channel<PermissionResult>>) = channels.put(channel.first, channel.second)

	fun getChannel(executor: Int): Channel<PermissionResult>? = channels.remove(executor)

	private fun closeAllChannels() {
		channels.forEach { channel ->
			channel.value.close()
		}
	}

	override fun onCleared() {
		super.onCleared()
		closeAllChannels()
	}
}