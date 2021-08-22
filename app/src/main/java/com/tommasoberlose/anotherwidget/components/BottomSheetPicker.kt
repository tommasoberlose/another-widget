package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.BottomSheetMenuHorBinding
import com.tommasoberlose.anotherwidget.databinding.BottomSheetMenuListBinding
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter

class BottomSheetPicker<T>(
    context: Context,
    private val items: List<MenuItem<T>> = arrayListOf(),
    private val getSelected: (() -> T)? = null,
    private val header: String? = null,
    private val onItemSelected: ((selectedValue: T?) -> Unit)? = null,
) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    private var loadingJobs: ArrayList<Job> = ArrayList()
    private lateinit var adapter: SlimAdapter

    private var binding: BottomSheetMenuHorBinding = BottomSheetMenuHorBinding.inflate(
        LayoutInflater.from(context))
    private var listBinding: BottomSheetMenuListBinding = BottomSheetMenuListBinding.inflate(
        LayoutInflater.from(context))

    override fun show() {
        window?.setDimAmount(0f)

        // Header
        binding.header.isVisible = header != null
        binding.headerText.text = header ?: ""

        // Alpha
        binding.alphaSelectorContainer.isVisible = false
        binding.actionContainer.isVisible = false

        // List
        adapter = SlimAdapter.create()

        loadingJobs.add(GlobalScope.launch(Dispatchers.IO) {
            listBinding.root.setHasFixedSize(true)
            val mLayoutManager = LinearLayoutManager(context)
            listBinding.root.layoutManager = mLayoutManager

            adapter
                .register<Int>(R.layout.bottom_sheet_menu_item) { position, injector ->
                    val item = items[position]
                    val isSelected = item.value == getSelected?.invoke()
                    injector
                        .text(R.id.label, item.title)
                        .textColor(R.id.label, ContextCompat.getColor(context, if (isSelected) R.color.colorAccent else R.color.colorSecondaryText))
                        .selected(R.id.item, isSelected)
                        .clicked(R.id.item) {
                            val oldIdx = items.toList().indexOfFirst { it.value == getSelected?.invoke() }
                            onItemSelected?.invoke(item.value)
                            adapter.notifyItemChanged(position)
                            adapter.notifyItemChanged(oldIdx)
                            (listBinding.root.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position,0)
                        }
                }
                .attachTo(listBinding.root)

            adapter.updateData((items.indices).toList())

            withContext(Dispatchers.Main) {
                binding.loader.isVisible = false
                binding.listContainer.addView(listBinding.root)
                this@BottomSheetPicker.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                binding.listContainer.isVisible = true

                val idx = items.toList().indexOfFirst { it.value == getSelected?.invoke() }
                (listBinding.root.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(idx,0)
            }
        })

        setContentView(binding.root)
        super.show()
    }

    override fun onStop() {
        loadingJobs.forEach { it.cancel() }
        super.onStop()
    }

    class MenuItem<T>(val title: String, val value: T? = null)

}