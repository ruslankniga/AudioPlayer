package com.example.mediaplayer.Notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mediaplayer.R
import com.example.mediaplayer.Song

class CreateNotification {

    val CHANNEL_ID = "chanel1"

    val ACTION_PLAY = "play"
    val ACTION_NEXT = "next"
    val ACTION_PREV = "prev"

    var notification: Notification? = null

    fun createNotification(context: Context, song: Song, playbutton: Int, pos: Int, size: Int){

        val notificationManagerCompar = NotificationManagerCompat.from(context)
        val mediaSessionCompat = MediaSessionCompat(context, "tag")

        //prev
        val pendingIntentPrev: PendingIntent?
        val drw_prev: Int
        if (pos == 0){
            pendingIntentPrev = null
            drw_prev = 0
        }
        else{
            val intentPrev = Intent(context, NotificationActionService::class.java)
                    .setAction(ACTION_PREV)
            pendingIntentPrev = PendingIntent.getBroadcast(context, 0, intentPrev, PendingIntent.FLAG_UPDATE_CURRENT)
            drw_prev = R.drawable.ic_baseline_skip_previous_24
        }

        //next
        val pendingIntentNext: PendingIntent?
        val drw_next: Int
        if (pos == size-1){
            pendingIntentNext = null
            drw_next = 0
        }
        else{
            val intentNext = Intent(context, NotificationActionService::class.java)
                    .setAction(ACTION_NEXT)
            pendingIntentNext = PendingIntent.getBroadcast(context, 0, intentNext, PendingIntent.FLAG_UPDATE_CURRENT)
            drw_next = R.drawable.ic_baseline_skip_next_24
        }

        //play
        val intentPlay = Intent(context, NotificationActionService::class.java)
                .setAction(ACTION_PLAY)
        val pendingIntentPlay = PendingIntent.getBroadcast(context, 0, intentPlay, PendingIntent.FLAG_UPDATE_CURRENT)



        notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_music_note_24)
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .addAction(drw_prev, "Previous", pendingIntentPrev)
                .addAction(playbutton, "Play", pendingIntentPlay)
                .addAction(drw_next, "Next", pendingIntentNext)
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                        //.setMediaSession(mediaSessionCompat.sessionToken))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

        notificationManagerCompar.notify(1, notification!!)
    }
}