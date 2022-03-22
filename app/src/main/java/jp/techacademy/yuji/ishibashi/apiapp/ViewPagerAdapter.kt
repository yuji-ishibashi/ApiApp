package jp.techacademy.yuji.ishibashi.apiapp

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity): FragmentStateAdapter(fragmentActivity) {
    private val TAG: String = "ViewPagerAdapter"

    val titleIds = listOf(R.string.tab_title_api, R.string.tab_title_favorite)

    val fragments = listOf(ApiFragment(), FavoriteFragment())

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount start")
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        Log.d(TAG, "createFragment start")
        return fragments[position]
    }
}