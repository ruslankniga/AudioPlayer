package com.example.mediaplayer

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.*


class MusicService() : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private var player: MediaPlayer? = null
    private var songs: ArrayList<Song>? = null
    private var songPosn = 0
    private val musicBind: IBinder = MusicBinder()
    private var context : Context? = null


    override fun onCreate(){
        super.onCreate()
        player = MediaPlayer()
        initMusicPlayer()
    }

    fun playSong(){
        player?.reset()

        val playSong = songs!!.get(songPosn)
        val currSong = playSong.id
        val trackUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong)
        try {
            player = MediaPlayer.create(context, trackUri)
        }
        catch (e: Exception){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player?.start()
        //player?.prepareAsync()
    }

    fun playLoadSong(uri: Uri){

        player?.reset()
        try {
            player = MediaPlayer.create(context, uri)
        }
        catch (e: Exception){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        player?.start()
    }

    fun setSong(songIndex: Int){
        songPosn = songIndex
    }

    fun checkPlayer() : Boolean{
        return player!=null
    }

    fun initMusicPlayer(){
        player!!.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        player!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
        player!!.setOnPreparedListener(this)
        player!!.setOnCompletionListener(this)
        player!!.setOnErrorListener(this)

    }

    fun setList(theSong: ArrayList<Song>, c: Context){
        songs = theSong
        context = c
    }

    override fun onBind(intent: Intent?): IBinder {
        return musicBind
    }

    override fun onUnbind(intent: Intent?): Boolean {
        player!!.stop()
        player!!.release()
        return false
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mp!!.start()
    }

    fun notification(str : String) {
        val intent = Intent(context!!.applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(context!!.applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notificationManager = context!!.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notBuilder = NotificationCompat.Builder(context!!.applicationContext, "CHANNEL_ID")
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.sym_def_app_icon)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .setContentTitle(str)

        val notificationChannel = NotificationChannel("CHANNEL_ID", "CHANNEL_ID", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(notificationChannel)

        notificationManager.notify(1, notBuilder.build())
    }


    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mp!!.reset()
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if(player!!.currentPosition > 0){
            mp!!.reset()
            playNext()
        }
    }

    fun getPosn(): Int {
        return player!!.currentPosition
    }

    fun getDur(): Int {
        return player!!.duration
    }

    fun isPng(): Boolean {
        return player!!.isPlaying
    }

    fun pausePlayer() {
        player!!.pause()
    }

    fun seek(posn: Int) {
        player!!.seekTo(posn)
    }

    fun go() {
        player!!.start()
    }

    fun playPrev(){
        if (songPosn > 0){
            songPosn--
            playSong()
        }
    }

    fun playNext(){
        if (songPosn < songs!!.size){
            songPosn++
            playSong()
        }
    }

    override fun onDestroy() {
        stopForeground(true)
    }

    class MusicBinder() : Binder() {
        val service: MusicService
            get() = MusicService()
    }
}