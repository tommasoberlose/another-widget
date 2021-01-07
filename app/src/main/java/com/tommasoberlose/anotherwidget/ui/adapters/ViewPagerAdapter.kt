package com.tommasoberlose.anotherwidget.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tommasoberlose.anotherwidget.ui.fragments.tabs.*

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> CalendarFragment.newInstance()
            2 -> WeatherFragment.newInstance()
            3 -> ClockFragment.newInstance()
            4 -> GlanceTabFragment.newInstance()
            else -> LayoutFragment.newInstance()
        }
    }
}