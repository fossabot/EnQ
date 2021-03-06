package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import android.widget.SearchView
import androidx.annotation.ContentView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.ivoberger.enq.R
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.ui.fragments.parents.TabbedResultsFragment
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.listener.ConnectionChangeListener
import com.ivoberger.jmusicbot.model.MusicBotPlugin
import kotlinx.android.synthetic.main.fragment_results.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@ContentView(R.layout.fragment_results)
class SearchFragment : TabbedResultsFragment(), ConnectionChangeListener {

    private val mSearchView: SearchView by lazy { (activity as MainActivity).searchView }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProviderPlugins = mBackgroundScope.async { JMusicBot.getProvider() }
        JMusicBot.connectionChangeListeners.add(this@SearchFragment)
        mBackgroundScope.launch {
            mProviderPlugins.await() ?: return@launch
            mConfig.lastProvider?.also {
                if (mProviderPlugins.await()!!.contains(it)) mSelectedPlugin = it
            }
        }

        mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            private var oldQuery = ""
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query == oldQuery) return true
                query?.also {
                    oldQuery = it
                    search(it)
                }
                return true
            }

            override fun onQueryTextChange(newQuery: String?): Boolean {
                if (newQuery == oldQuery) return true
                newQuery?.also { oldQuery = it }
                // debounce
                mBackgroundScope.launch {
                    delay(300)
                    if (oldQuery != newQuery) return@launch
                    search(oldQuery)
                }
                return true
            }
        })
    }

    override fun initializeTabs() {
        mBackgroundScope.launch {
            mProviderPlugins.await() ?: return@launch
            mFragmentPagerAdapter = async {
                SearchFragmentPager(childFragmentManager, mProviderPlugins.await()!!)
            }
            mMainScope.launch { view_pager.adapter = mFragmentPagerAdapter.await() }
        }
    }

    fun search(query: String) {
        mBackgroundScope.launch {
            if (query.isNotBlank()) (mFragmentPagerAdapter.await() as SearchFragmentPager).search(query)
        }
    }

    override fun onTabSelected(position: Int) {
        mBackgroundScope.launch { mFragmentPagerAdapter.await().onTabSelected(position) }
    }

    override fun onConnectionLost(e: Exception?) {
        activity?.supportFragmentManager?.popBackStack()
    }

    override fun onConnectionRecovered() {}

    override fun onDestroy() {
        super.onDestroy()
        mConfig.lastProvider = mSelectedPlugin
        JMusicBot.connectionChangeListeners.remove(this)
    }

    inner class SearchFragmentPager(fm: FragmentManager, provider: List<MusicBotPlugin>) :
        TabbedResultsFragment.SongListFragmentPager(fm, provider) {

        override fun getItem(position: Int): Fragment {
            val fragment = SearchResultsFragment.newInstance(provider[position].id)
            resultFragments.add(position, fragment)
            return fragment
        }

        fun search(query: String) {
            Timber.d("Searching for $query")
            resultFragments.forEach { (it as SearchResultsFragment).setQuery(query) }
            (resultFragments[view_pager.currentItem] as SearchResultsFragment).startSearch()
        }

        override fun onTabSelected(position: Int) {
            super.onTabSelected(position)
            (resultFragments[position] as SearchResultsFragment).startSearch()
        }
    }
}
