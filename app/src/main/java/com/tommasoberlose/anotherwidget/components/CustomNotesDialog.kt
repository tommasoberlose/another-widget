package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Preferences
import kotlinx.android.synthetic.main.custom_notes_dialog_layout.view.*

class CustomNotesDialog(context: Context, callback: (() -> Unit)?) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    init {
        val view = View.inflate(context, R.layout.custom_notes_dialog_layout, null)
        view.notes.setText(Preferences.customNotes)

        view.action_positive.setOnClickListener {
            Preferences.customNotes = view.notes.text.toString()
            this.dismiss()
            callback?.invoke()
        }

        view.notes.requestFocus()

        setContentView(view)
    }
}