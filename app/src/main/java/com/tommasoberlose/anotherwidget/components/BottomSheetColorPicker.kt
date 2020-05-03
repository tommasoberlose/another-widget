package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.GridLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.isColorDark
import kotlinx.android.synthetic.main.bottom_sheet_menu_hor.view.*
import kotlinx.android.synthetic.main.color_picker_menu_item.view.*

class BottomSheetColorPicker(
    context: Context,
    private val colors: IntArray = intArrayOf(),
    private val selected: Int? = null,
    private val header: String? = null,
    private val onColorSelected: ((selectedValue: Int) -> Unit)? = null
) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    override fun show() {
        val view = View.inflate(context, R.layout.bottom_sheet_menu_hor, null)

        // Header
        view.header.isVisible = header != null
        view.header_text.text = header ?: ""

        // Menu
        for (@ColorInt color: Int in colors) {
            val itemView = View.inflate(context, R.layout.color_picker_menu_item, null)
            itemView.color.setCardBackgroundColor(ColorStateList.valueOf(color))
            itemView.check.setColorFilter(ContextCompat.getColor(context,
                if (color.isColorDark()) android.R.color.white else android.R.color.black
            ), android.graphics.PorterDuff.Mode.MULTIPLY)
            itemView.check.isVisible = selected == color
            itemView.color.setOnClickListener {
                onColorSelected?.invoke(color)
                this.dismiss()
            }
            view.menu.addView(itemView, GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
                )
            )
        }

        setContentView(view)
        super.show()
    }

}