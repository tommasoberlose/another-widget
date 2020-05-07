package com.tommasoberlose.anotherwidget.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tommasoberlose.anotherwidget.ui.fragments.*

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> CalendarSettingsFragment.newInstance()
            2 -> WeatherSettingsFragment.newInstance()
            3 -> ClockSettingsFragment.newInstance()
            else -> GeneralSettingsFragment.newInstance()
        }
    }
}