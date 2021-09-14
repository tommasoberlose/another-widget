package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.BottomSheetDialogBinding

typealias DialogCallback = () -> Unit

class MaterialBottomSheetDialog(
    context: Context,
    private val title: String? = null,
    private val message: String? = null
) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    private var positiveButtonLabel: String? = null
    private var negativeButtonLabel: String? = null
    private var positiveCallback: DialogCallback? = null
    private var negativeCallback: DialogCallback? = null

    private var binding = BottomSheetDialogBinding.inflate(LayoutInflater.from(context))

    fun setPositiveButton(label: String? = context.getString(android.R.string.ok), callback: DialogCallback? = null): MaterialBottomSheetDialog {
        positiveButtonLabel = label
        positiveCallback = callback
        return this
    }

    fun setNegativeButton(label: String? = context.getString(android.R.string.cancel), callback: DialogCallback? = null): MaterialBottomSheetDialog {
        negativeButtonLabel = label
        negativeCallback = callback
        return this
    }

    override fun show() {
        // Header
        binding.title.isVisible = title != null
        binding.title.text = title ?: ""

        binding.message.isVisible = message != null
        binding.message.text = message ?: ""

        binding.actionPositive.isVisible = positiveButtonLabel != null
        binding.actionPositive.text = positiveButtonLabel ?: ""
        binding.actionPositive.setOnClickListener {
            positiveCallback?.invoke()
            this.dismiss()
        }

        binding.actionNegative.isVisible = negativeButtonLabel != null
        binding.actionNegative.text = negativeButtonLabel ?: ""
        binding.actionNegative.setOnClickListener {
            negativeCallback?.invoke()
            this.dismiss()
        }

        setContentView(binding.root)
        behavior.run {
            skipCollapsed = true
            state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
        super.show()
    }

}