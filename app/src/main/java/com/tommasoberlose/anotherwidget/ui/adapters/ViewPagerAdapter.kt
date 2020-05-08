package com.tommasoberlose.anotherwidget.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tommasoberlose.anotherwidget.ui.fragments.*

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> CalendarTabFragment.newInstance()
            2 -> WeatherTabFragment.newInstance()
            3 -> ClockTabFragment.newInstance()
            4 -> MusicTabFragment.newInstance()
            else -> GeneralTabFragment.newInstance()
        }
    }
}