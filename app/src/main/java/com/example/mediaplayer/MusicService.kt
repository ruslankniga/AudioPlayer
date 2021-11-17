package com.example.mediaplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.example.mediaplayer.Notification.CreateNotification
import java.util.*


class MusicService() : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private var player: MediaPlayer? = null
    private var songs: ArrayList<Song>? = null
    private var songPosn = 0
    private val musicBind: IBinder = MusicBinder()
    private var context : Context? = null
    private var createNotification = CreateNotification()
    private var notificationManager: NotificationManager? = null
    private var playSong: Song? = null


    override fun onCreate(){
        super.onCreate()
        player = MediaPlayer()
        initMusicPlayer()
    }

    fun playSong(){
        player?.reset()

        playSong = songs!!.get(songPosn)
        val currSong = playSong!!.id
        val trackUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong)
        try {
            player = MediaPlayer.create(context, trackUri)
        }
        catch (e: Exception){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player?.start()

        createChanel()
        createNotification.createNotification(context!!.applicationContext, playSong!!, R.drawable.ic_baseline_pause_24, songPosn, songs!!.size)

        //player?.prepareAsync()
    }

    fun createChanel(){
        val chanel = NotificationChannel(createNotification.CHANNEL_ID,
                "Kod dev", NotificationManager.IMPORTANCE_LOW)

        notificationManager = context!!.applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(chanel)
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
        createNotification.createNotification(context!!.applicationContext, playSong!!, R.drawable.ic_baseline_play_arrow_24, songPosn, songs!!.size)
    }

    fun seek(posn: Int) {
        player!!.seekTo(posn)
    }

    fun go() {
        player!!.start()
        createNotification.createNotification(context!!.applicationContext, playSong!!, R.drawable.ic_baseline_pause_24, songPosn, songs!!.size)
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
        notificationManager?.cancelAll()
        stopForeground(true)
    }

    class MusicBinder() : Binder() {
        val service: MusicService
            get() = MusicService()
    }
}