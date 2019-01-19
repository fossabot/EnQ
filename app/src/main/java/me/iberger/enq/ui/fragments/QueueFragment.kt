package me.iberger.enq.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.*
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter_extensions.drag.ItemTouchCallback
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeCallback
import com.mikepenz.fastadapter_extensions.utilities.DragDropUtil
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.iberger.enq.R
import me.iberger.enq.ui.MainActivity
import me.iberger.enq.ui.items.QueueItem
import me.iberger.enq.ui.listener.QueueUpdateCallback
import me.iberger.enq.ui.viewmodel.QueueViewModel
import me.iberger.enq.utils.changeFavoriteStatus
import me.iberger.enq.utils.setupSwipeDragActions
import me.iberger.enq.utils.toastShort
import me.iberger.jmusicbot.KEY_QUEUE
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.model.QueueEntry
import me.iberger.jmusicbot.exceptions.AuthException
import timber.log.Timber

class QueueFragment : Fragment(), SimpleSwipeCallback.ItemSwipeCallback, ItemTouchCallback {
    companion object {
        fun newInstance() = QueueFragment()
    }

    private val mViewModel by lazy { ViewModelProviders.of(this).get(QueueViewModel::class.java) }
    private val mUIScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private val diffCallback = object : DiffUtil.ItemCallback<QueueEntry>() {
        override fun areItemsTheSame(oldItem: QueueEntry, newItem: QueueEntry): Boolean =
            oldItem.song.id == newItem.song.id && oldItem.userName == newItem.userName

        override fun areContentsTheSame(oldItem: QueueEntry, newItem: QueueEntry): Boolean = oldItem == newItem

    }

    private var mAsyncDiffer: AsyncListDiffer<QueueEntry>? = null
    private var mQueueUpdateCallback: QueueUpdateCallback? = null

    private val mFastItemAdapter: FastItemAdapter<QueueItem> by lazy { FastItemAdapter<QueueItem>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("Creating Queue Fragment")
        super.onCreate(savedInstanceState)
        mViewModel.queue.observe(this, Observer {
            mQueueUpdateCallback?.currentList = it
            mAsyncDiffer?.submitList(it)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_queue, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mQueueUpdateCallback = QueueUpdateCallback(mFastItemAdapter)
        mAsyncDiffer = AsyncListDiffer(mQueueUpdateCallback!!, AsyncDifferConfig.Builder(diffCallback).build())

        queue.layoutManager = LinearLayoutManager(context).apply { reverseLayout = true }
        queue.adapter = mFastItemAdapter
        savedInstanceState?.also { mFastItemAdapter.withSavedInstanceState(it, KEY_QUEUE) }

        setupSwipeDragActions(
            context!!, queue, this, this,
            CommunityMaterial.Icon2.cmd_star, R.color.favorites,
            CommunityMaterial.Icon.cmd_delete, R.color.delete
        )
    }

    override fun itemSwiped(position: Int, direction: Int) {
        mBackgroundScope.launch {
            val entry = mFastItemAdapter.getAdapterItem(position)
            when (direction) {
                ItemTouchHelper.RIGHT -> {
                    if (!MainActivity.connected) return@launch
                    try {
                        MusicBot.instance?.dequeue(entry.song)
                    } catch (e: AuthException) {
                        Timber.e("AuthException with reason ${e.reason}")
                        withContext(Dispatchers.Main) {
                            context!!.toastShort(R.string.msg_no_permission)
                            mFastItemAdapter.notifyAdapterItemChanged(position)
                        }
                    }
                }
                ItemTouchHelper.LEFT -> {
                    changeFavoriteStatus(context!!, entry.song)
                    withContext(Dispatchers.Main) {
                        mFastItemAdapter.notifyAdapterItemChanged(position)
                    }
                }
            }
        }
    }

    override fun itemTouchOnMove(oldPosition: Int, newPosition: Int): Boolean {
        if (!MainActivity.connected) return false
        DragDropUtil.onMove(mFastItemAdapter.itemAdapter, oldPosition, newPosition)
        return true
    }

    override fun itemTouchDropped(oldPosition: Int, newPosition: Int) {
        if (!MainActivity.connected) return
        mBackgroundScope.launch {
            val entry = mFastItemAdapter.getAdapterItem(newPosition).queueEntry
            Timber.d("Moved $entry from $oldPosition to $newPosition")
            try {
                MusicBot.instance?.moveSong(entry, newPosition)
            } catch (e: Exception) {
                Timber.e(e)
                mUIScope.launch {
                    context?.toastShort(R.string.msg_no_permission)
                    DragDropUtil.onMove(mFastItemAdapter.itemAdapter, newPosition, oldPosition)
                }
            }
        }
    }
}
