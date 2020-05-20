package com.tommasoberlose.anotherwidget.components

import android.view.View
import android.widget.CompoundButton

class MenuItem (
    val icon: Int,
    val getIcon: (() -> Int)? = null,
    val title: String,
    val label: String = "",
    val getLabel: (() -> String)? = null,
    val isEnabled: (() -> Boolean) = fun (): Boolean { return true },
    val onClick: View.OnClickListener? = null,
    val onLongClick: View.OnLongClickListener? = null,
    val showToggle: Boolean = false,
    val toggleValue: (() -> Boolean) = fun (): Boolean { return false },
    val onToggle: CompoundButton.OnCheckedChangeListener? = null,
    val showPermission: (() -> Boolean) = fun (): Boolean { return false },
    val onPermissionClickListener: View.OnClickListener? = null,
    val render: ((view: View) -> Unit)? = null
)