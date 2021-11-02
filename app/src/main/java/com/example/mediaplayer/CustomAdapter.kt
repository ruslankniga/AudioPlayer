package com.example.mediaplayer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import java.util.*


class CustomAdapter(theSongs : ArrayList<Song>, c: Context?) : BaseAdapter() {

    private val songs = theSongs
    private val songInf = LayoutInflater.from(c)

    override fun getCount(): Int {
        return songs.size
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val songLay = songInf.inflate(R.layout.list_item, parent, false) as CardView
        val txtView = songLay.
        findViewById<TextView>(R.id.txtSong)
        val currSong = songs[position]

        txtView.text = (position + 1).toString() +  ") " + currSong.title + " - " + currSong.artist

        songLay.setTag(position)
        return songLay
    }
}