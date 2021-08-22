package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.BottomSheetMenuHorBinding
import com.tommasoberlose.anotherwidget.databinding.BottomSheetMenuListBinding
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.copyToClipboard
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.isClipboardColor
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.isColorDark
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.pasteFromClipboard
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.toIntValue
import com.tommasoberlose.anotherwidget.utils.isDarkTheme
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter

class BottomSheetColorPicker(
    context: Context,
    private val colors: IntArray = intArrayOf(),
    private val getSelected: (() -> Int)? = null,
    private val header: String? = null,
    private val onColorSelected: ((selectedValue: Int) -> Unit)? = null,
    private val showAlphaSelector: Boolean = false,
    private val alpha: Int = 0,
    private val onAlphaChangeListener: ((alpha: Int) -> Unit)? = null,
    private val hideCopyPaste: Boolean = false,
) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    private var loadingJobs: ArrayList<Job> = ArrayList()
    private lateinit var adapter: SlimAdapter
    private var alphaDebouncing: Job? = null

    private var binding: BottomSheetMenuHorBinding = BottomSheetMenuHorBinding.inflate(LayoutInflater.from(context))
    private var listBinding: BottomSheetMenuListBinding = BottomSheetMenuListBinding.inflate(LayoutInflater.from(context))

    override fun show() {
        window?.setDimAmount(0f)

        // Header
        binding.header.isVisible = header != null
        binding.headerText.text = header ?: ""

        if (hideCopyPaste) {
            binding.actionContainer.isVisible = false
        } else {
            binding.actionContainer.isVisible = true
            binding.actionCopy.setOnClickListener {
                context.copyToClipboard(getSelected?.invoke(), alpha)
            }
            binding.actionPaste.setOnClickListener {
                context.pasteFromClipboard { color, alpha ->
                    binding.alphaSelector.setProgress(alpha.toIntValue().toFloat())

                    adapter.notifyItemChanged(adapter.data.indexOf(getSelected?.invoke()))
                    onColorSelected?.invoke(Color.parseColor(color))
                    val idx = colors.toList().indexOf(getSelected?.invoke())
                    adapter.notifyItemChanged(idx)
                    (listBinding.root.layoutManager as GridLayoutManager).scrollToPositionWithOffset(idx,0)
                }
            }
            binding.actionPaste.isVisible = context.isClipboardColor()
        }

        // Alpha
        binding.alphaSelectorContainer.isVisible = showAlphaSelector
        binding.alphaSelector.setProgress(alpha.toFloat())
        binding.textAlpha.text = "%s: %s%%".format(context.getString(R.string.alpha), alpha)
        binding.alphaSelector.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams?) {
                seekParams?.let {
                    binding.textAlpha.text =
                        "%s: %s%%".format(context.getString(R.string.alpha), it.progress)
                    alphaDebouncing?.cancel()
                    alphaDebouncing = GlobalScope.launch(Dispatchers.IO) {
                        delay(150)
                        withContext(Dispatchers.Main) {
                            onAlphaChangeListener?.invoke(it.progress)
                        }
                    }
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
            listBinding.root.setHasFixedSize(true)
            val mLayoutManager = GridLayoutManager(context, 6)
            listBinding.root.layoutManager = mLayoutManager

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
                            (listBinding.root.layoutManager as GridLayoutManager).scrollToPositionWithOffset(position,0)
                        }
                }
                .attachTo(listBinding.root)

            adapter.updateData(colors.toList())

            withContext(Dispatchers.Main) {
                binding.loader.isVisible = false
                binding.listContainer.addView(listBinding.root)
                this@BottomSheetColorPicker.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                binding.listContainer.isVisible = true

                val idx = colors.toList().indexOf(getSelected?.invoke())
                (listBinding.root.layoutManager as GridLayoutManager).scrollToPositionWithOffset(idx,0)
            }
        })

        setContentView(binding.root)
        super.show()
    }

    override fun onStop() {
        loadingJobs.forEach { it.cancel() }
        super.onStop()
    }

}