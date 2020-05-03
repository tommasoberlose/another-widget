package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.GridLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.helpers.ColorHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.isColorDark
import com.tommasoberlose.anotherwidget.utils.expand
import com.tommasoberlose.anotherwidget.utils.reveal
import kotlinx.android.synthetic.main.bottom_sheet_menu_hor.*
import kotlinx.android.synthetic.main.bottom_sheet_menu_hor.view.*
import kotlinx.android.synthetic.main.bottom_sheet_menu_hor.view.color_loader
import kotlinx.android.synthetic.main.color_picker_menu_item.view.*
import kotlinx.coroutines.*
import java.lang.Exception
import java.util.prefs.Preferences

class BottomSheetColorPicker(
    context: Context,
    private val colors: IntArray = intArrayOf(),
    private val selected: Int? = null,
    private val header: String? = null,
    private val onColorSelected: ((selectedValue: Int) -> Unit)? = null
) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    private val loadingJob: Job? = null

    override fun show() {
        val view = View.inflate(context, R.layout.bottom_sheet_menu_hor, null)

        // Header
        view.header.isVisible = header != null
        view.header_text.text = header ?: ""

        val itemViews: ArrayList<View> = ArrayList()

        GlobalScope.launch(Dispatchers.IO) {
            for (@ColorInt color: Int in colors) {
                val itemView = View.inflate(context, R.layout.color_picker_menu_item, null)
                itemView.color.setCardBackgroundColor(ColorStateList.valueOf(color))
                itemView.check.setColorFilter(ContextCompat.getColor(context,
                    if (color.isColorDark()) android.R.color.white else android.R.color.black
                ), android.graphics.PorterDuff.Mode.MULTIPLY)
                itemView.check.isVisible = selected == color
                itemView.color.setOnClickListener {
                    onColorSelected?.invoke(color)
                    this@BottomSheetColorPicker.dismiss()
                }
                itemViews.add(itemView)
            }

            withContext(Dispatchers.Main) {
                itemViews.forEach {
                    view.menu.addView(it, GridLayout.LayoutParams(
                        GridLayout.spec(GridLayout.UNDEFINED, 1f),
                        GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    ))
                }
                color_loader.isVisible = false
                view.menu.isVisible = true
                this@BottomSheetColorPicker.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        // Menu

        setContentView(view)
        super.show()
    }

    override fun onStop() {
        loadingJob?.cancel()
        super.onStop()
    }

}