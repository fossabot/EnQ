package com.ivoberger.enq.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.listener.ConnectionChangeListener
import com.ivoberger.jmusicbot.model.PlayerState
import com.ivoberger.jmusicbot.model.QueueEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel : ViewModel(), ConnectionChangeListener {

    val mMainScope = CoroutineScope(Dispatchers.Main)
    val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    var playerCollapsed = false
    var bottomNavCollapsed = false

    val playerState: LiveData<PlayerState> by lazy { JMusicBot.getPlayerState() }
    val queue: LiveData<List<QueueEntry>>by lazy { JMusicBot.getQueue() }

    init {
        JMusicBot.connectionChangeListeners.add(this)
    }

    override fun onConnectionLost(e: Exception?) {
        mBackgroundScope.launch {
            Timber.w(e)
            JMusicBot.stopPlayerUpdates()
            JMusicBot.stopQueueUpdates()
            if (JMusicBot.user != null) JMusicBot.recoverConnection()
        }
    }

    override fun onConnectionRecovered() {
        mBackgroundScope.launch {
            JMusicBot.startPlayerUpdates()
            JMusicBot.startQueueUpdates()
        }
    }

    override fun onCleared() {
        super.onCleared()
        JMusicBot.connectionChangeListeners.remove(this)
    }
}
