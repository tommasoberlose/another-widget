package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tommasoberlose.anotherwidget.R
import kotlinx.android.synthetic.main.bottom_sheet_dialog.view.*

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
        val view = View.inflate(context, R.layout.bottom_sheet_dialog, null)

        // Header
        view.title.isVisible = title != null
        view.title.text = title ?: ""

        view.message.isVisible = message != null
        view.message.text = message ?: ""

        view.action_positive.isVisible = positiveButtonLabel != null
        view.action_positive.text = positiveButtonLabel ?: ""
        view.action_positive.setOnClickListener {
            positiveCallback?.invoke()
            this.dismiss()
        }

        view.action_negative.isVisible = negativeButtonLabel != null
        view.action_negative.text = negativeButtonLabel ?: ""
        view.action_negative.setOnClickListener {
            negativeCallback?.invoke()
            this.dismiss()
        }

        setContentView(view)
        super.show()
    }

}