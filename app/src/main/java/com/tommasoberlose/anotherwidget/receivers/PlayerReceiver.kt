package com.tommasoberlose.anotherwidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class PlayerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ciao", "player ok")

//        val cmd = intent.getStringExtra("command")
//        Log.v("tag ", "$action / $cmd")
//        val artist = intent.getStringExtra("artist")
//        val album = intent.getStringExtra("album")
//        val track = intent.getStringExtra("track")
//        Log.v("tag", "$artist:$album:$track")

    }
}