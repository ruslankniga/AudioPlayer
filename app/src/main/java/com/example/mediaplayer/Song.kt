package com.example.mediaplayer

import android.os.Parcel
import android.os.Parcelable
import java.io.File

class Song(Id: Long, Title: String, Artist: String) {

    val id = Id
    val title = Title
    val artist = Artist
}