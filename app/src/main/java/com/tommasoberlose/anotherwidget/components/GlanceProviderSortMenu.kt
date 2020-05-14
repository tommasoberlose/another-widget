package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.isColorDark
import com.tommasoberlose.anotherwidget.helpers.GlanceProviderHelper
import com.tommasoberlose.anotherwidget.models.GlanceProvider
import kotlinx.android.synthetic.main.glance_provider_sort_bottom_menu.view.*
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter
import java.util.*
import kotlin.collections.ArrayList

class GlanceProviderSortMenu(
    context: Context
) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    private lateinit var adapter: SlimAdapter

    override fun show() {
        val view = View.inflate(context, R.layout.glance_provider_sort_bottom_menu, null)

        // Header
        view.header_text.text = context.getString(R.string.settings_sort_glance_providers_title)

        // List
        adapter = SlimAdapter.create()

        view.menu.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        view.menu.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<GlanceProvider>(R.layout.glance_provider_item) { item, injector ->
                injector
                    .text(R.id.title, item.title)
                    .with<ImageView>(R.id.icon) {
                        it.setImageDrawable(ContextCompat.getDrawable(context, item.icon))
                    }
            }
            .attachTo(view.menu)

        val mIth = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
                ): Boolean {
                    val fromPos = viewHolder.adapterPosition
                    val toPos = target.adapterPosition
                    // move item in `fromPos` to `toPos` in adapter.
                    adapter.notifyItemMoved(fromPos, toPos)

                    val list = GlanceProviderHelper.getGlanceProviders(context)
                    Collections.swap(list, fromPos, toPos)
                    GlanceProviderHelper.saveGlanceProviderOrder(list)
                    return true
                }

                override fun onSwiped(
                    viewHolder: RecyclerView.ViewHolder,
                    direction: Int
                ) {
                    // remove from adapter
                }
            })

        mIth.attachToRecyclerView(view.menu)

        adapter.updateData(
            GlanceProviderHelper.getGlanceProviders(context)
                .mapNotNull { GlanceProviderHelper.getGlanceProviderById(context, it) }
        )

        setContentView(view)
        super.show()
    }
}

