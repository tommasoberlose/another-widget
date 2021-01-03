package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.SeekBar
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.helpers.ColorHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.isColorDark
import com.tommasoberlose.anotherwidget.utils.expand
import com.tommasoberlose.anotherwidget.utils.isDarkTheme
import com.tommasoberlose.anotherwidget.utils.reveal
import com.tommasoberlose.anotherwidget.utils.toPixel
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.android.synthetic.main.bottom_sheet_menu_hor.*
import kotlinx.android.synthetic.main.bottom_sheet_menu_hor.view.*
import kotlinx.android.synthetic.main.bottom_sheet_menu_hor.view.color_loader
import kotlinx.android.synthetic.main.color_picker_menu_item.view.*
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter
import java.lang.Exception
import java.util.prefs.Preferences

class BottomSheetColorPicker(
    context: Context,
    private val colors: IntArray = intArrayOf(),
    private val getSelected: (() -> Int)? = null,
    private val header: String? = null,
    private val onColorSelected: ((selectedValue: Int) -> Unit)? = null,
    private val showAlphaSelector: Boolean = false,
    private val alpha: Int = 0,
    private val onAlphaChangeListener: ((alpha: Int) -> Unit)? = null
) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    private var loadingJobs: ArrayList<Job> = ArrayList()
    private lateinit var adapter: SlimAdapter

    override fun show() {
        val view = View.inflate(context, R.layout.bottom_sheet_menu_hor, null)

        window?.setDimAmount(0f)

        // Header
        view.header.isVisible = header != null
        view.header_text.text = header ?: ""

        // Alpha
        view.alpha_selector_container.isVisible = showAlphaSelector
        view.alpha_selector.setProgress(alpha.toFloat())
        view.text_alpha.text = "%s: %s%%".format(context.getString(R.string.alpha), alpha)
        view.alpha_selector.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams?) {
                seekParams?.let {
                    view.text_alpha.text = "%s: %s%%".format(context.getString(R.string.alpha), it.progress)
                    onAlphaChangeListener?.invoke(it.progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {
            }
        }

        // List

        adapter = SlimAdapter.create()

        loadingJobs.add(GlobalScope.launch(Dispatchers.IO) {
            val listView = View.inflate(context, R.layout.bottom_sheet_menu_list, null) as RecyclerView
            listView.setHasFixedSize(true)
            val mLayoutManager = GridLayoutManager(context, 6)
            listView.layoutManager = mLayoutManager

            adapter
                .register<Int>(R.layout.color_picker_menu_item) { item, injector ->
                    injector
                        .with<MaterialCardView>(R.id.color) {
                            it.setCardBackgroundColor(ColorStateList.valueOf(item))
                            it.strokeWidth = if ((colors.indexOf(item) == 0 && !context.isDarkTheme()) || (colors.indexOf(item) == 10 && context.isDarkTheme())) 2 else 0
                        }
                        .with<AppCompatImageView>(R.id.check) {
                            if (getSelected?.invoke() == item) {
                                it.setColorFilter(
                                    ContextCompat.getColor(
                                        context,
                                        if (item.isColorDark()) android.R.color.white else android.R.color.black
                                    ),
                                    android.graphics.PorterDuff.Mode.MULTIPLY
                                )
                                it.isVisible = true
                            } else {
                                it.isVisible = false
                            }
                        }
                        .clicked(R.id.color) {
                            adapter.notifyItemChanged(adapter.data.indexOf(getSelected?.invoke()))
                            onColorSelected?.invoke(item)
                            val position = adapter.data.indexOf(item)
                            adapter.notifyItemChanged(position)
                            (listView.layoutManager as GridLayoutManager).scrollToPositionWithOffset(position,0)
                        }
                }
                .attachTo(listView)

            adapter.updateData(colors.toList())

            withContext(Dispatchers.Main) {
                view.color_loader.isVisible = false
                view.list_container.addView(listView)
                this@BottomSheetColorPicker.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                view.list_container.isVisible = true
            }
        })

        setContentView(view)
        super.show()
    }

    override fun onStop() {
        loadingJobs.forEach { it.cancel() }
        super.onStop()
    }

}