package com.tommasoberlose.anotherwidget.ui.adapter

import android.content.Context
import android.content.pm.ApplicationInfo
import android.support.v7.widget.RecyclerView
import android.util.EventLog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.`object`.AppInfoSavedEvent
import org.greenrobot.eventbus.EventBus
import java.io.File

/**
 * Created by tommaso on 15/10/17.
 */
class ApplicationInfoAdapter (private val context: Context, private var mDataset: ArrayList<ApplicationInfo>) : RecyclerView.Adapter<ApplicationInfoAdapter.ViewHolder>() {

    class ViewHolder(var view: View, var text: TextView, var icon: ImageView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.application_info_layout, parent, false)
        return ViewHolder(v, v.findViewById(R.id.text), v.findViewById(R.id.icon))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pm = context.packageManager
        val app = mDataset[position]
        holder.text.text = pm.getApplicationLabel(app).toString()
        try {
            holder.icon.setImageDrawable(app.loadIcon(pm))
        } catch (ignore: Exception) {
        }

        holder.view.setOnClickListener {
            EventBus.getDefault().post(AppInfoSavedEvent(app))
        }
    }

    override fun getItemCount(): Int {
        return mDataset.size
    }

    fun changeData(newData: ArrayList<ApplicationInfo>) {
        mDataset = newData
        notifyDataSetChanged()
    }
}