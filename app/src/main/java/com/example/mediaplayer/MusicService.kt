package com.example.mediaplayer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.TextView
import java.util.*


class MusicService() : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private var player: MediaPlayer? = null
    private var songs: ArrayList<Song>? = null
    private var songPosn = 0
    private val musicBind: IBinder = MusicBinder()
    private var context : Context? = null
    private val NOTIFY_ID = 1


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

    fun setSong(songIndex: Int){
        songPosn = songIndex
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
        val notIntent = Intent(this, MainActivity::class.java)
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        /*
        val pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = Notification.Builder(this)

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songs?.get(songPosn)!!.title)
                .setOngoing(true)
                //.setContentTitle(Playing)
                .setContentText(songs?.get(songPosn)!!.title)


        startForeground(NOTIFY_ID, builder.build())*/
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