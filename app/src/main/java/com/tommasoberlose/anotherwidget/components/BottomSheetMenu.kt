package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.BottomSheetMenuBinding
import com.tommasoberlose.anotherwidget.databinding.BottomSheetMenuItemBinding

/**
 * [BottomSheetDialogFragment] that uses a custom
 * theme which sets a rounded background to the dialog
 * and doesn't dim the navigation bar
 */
open class BottomSheetMenu<T>(context: Context, private val header: String? = null, private val message: String? = null, private val isMessageWarning: Boolean = false, private val isMultiSelection: Boolean = false) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    private val items: ArrayList<MenuItem<T>> = ArrayList()
    private var selectedRes: ArrayList<T> = ArrayList()
    private var callback: ((selectedValue: T) -> Unit)? = null
    private var multipleSelectionCallback: ((selectedValues: ArrayList<T>) -> Unit)? = null

    private var binding = BottomSheetMenuBinding.inflate(LayoutInflater.from(context))

    fun setSelectedValue(res: T): BottomSheetMenu<T> {
        selectedRes = ArrayList(listOf(res))
        return this
    }

    fun setSelectedValues(res: List<T>): BottomSheetMenu<T> {
        selectedRes = ArrayList(res)
        return this
    }

    fun addItem(title: String, value: T? = null, renderCallback: ((view: TextView) -> Unit)? = null): BottomSheetMenu<T> {
        items.add(MenuItem(title, value, renderCallback))
        return this
    }

    fun addOnSelectItemListener(callback: (selectedValue: T) -> Unit): BottomSheetMenu<T> {
        this.callback = callback
        return this
    }

    fun addOnMultipleSelectItemListener(multipleSelectionCallback: (selectedValues: ArrayList<T>) -> Unit): BottomSheetMenu<T> {
        this.multipleSelectionCallback = multipleSelectionCallback
        return this
    }

    override fun show() {
        // Header
        binding.header.isVisible = header != null
        binding.headerText.text = header ?: ""

        binding.warningText.isVisible = message != null
        binding.warningText.text = message ?: ""
        binding.warningText.setTextColor(ContextCompat.getColor(context, if (isMessageWarning) R.color.warningColorText else R.color.colorSecondaryText))

        // Menu
        for (item in items) {
            val itemBinding = BottomSheetMenuItemBinding.inflate(LayoutInflater.from(context))
            if (item.value != null) {
                itemBinding.label.text = item.title
                if (isMultiSelection) {
                    itemBinding.iconCheck.isVisible = selectedRes.contains(item.value)
                    itemBinding.label.setTextColor(
                        if (selectedRes.contains(item.value)) ContextCompat.getColor(
                            context,
                            R.color.colorPrimaryText
                        ) else ContextCompat.getColor(context, R.color.colorSecondaryText)
                    )
                } else {
                    itemBinding.root.isSelected = selectedRes.contains(item.value)
                }
                itemBinding.root.setOnClickListener {
                    if (!isMultiSelection) {
                        callback?.invoke(item.value)
                        this.dismiss()
                    } else {
                        if (selectedRes.contains(item.value)) {
                            selectedRes.remove(item.value)
                        } else {
                            selectedRes.add(item.value)
                        }

                        multipleSelectionCallback?.invoke(selectedRes)
                        itemBinding.iconCheck.isVisible = selectedRes.contains(item.value)
                        itemBinding.label.setTextColor(
                            if (selectedRes.contains(item.value)) ContextCompat.getColor(
                                context,
                                R.color.colorPrimaryText
                            ) else ContextCompat.getColor(context, R.color.colorSecondaryText)
                        )
                    }

                    item.renderCallback?.invoke(itemBinding.label)
                }

                binding.menu.addView(itemBinding.root)
            } else {
                itemBinding.label.text = item.title
                binding.menu.addView(itemBinding.root)
            }
        }
        setContentView(binding.root)
        super.show()
    }

    class MenuItem<T>(val title: String, val value: T? = null, val renderCallback: ((view: TextView) -> Unit)? = null)

}