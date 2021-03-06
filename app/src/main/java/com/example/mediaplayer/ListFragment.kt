package com.example.mediaplayer

import android.app.Activity
import android.content.*
import android.database.Cursor
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.mediaplayer.Notification.CreateNotification
import com.example.mediaplayer.Notification.OnClearFromRecentService
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import java.io.File
import java.lang.Thread.sleep


class ListFragment : Fragment() {

    //Variable
    private var songList : ArrayList<Song>? = null
    private var musicSrv: MusicService? = null
    private var playIntent: Intent? = null
    private var musicBound = false
    private var paused = false
    private var playbackPaused = false
    private var currentPosition: Int? = null
    private var updateSeekBar : Thread? = null

    //UI
    private var listView : ListView? = null
    private var btnPlay : Button? = null
    private var btnNext : Button? = null
    private var btnPrevious : Button? = null
    private var textSong : TextView? = null
    private var textStart : TextView? = null
    private var textEnd : TextView? = null
    private var seekBar : SeekBar? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        songList = ArrayList<Song>()

        runtimePermission()

        requireContext().registerReceiver(broadcastReceiver, IntentFilter("TRACKS_TRACKS"))
        requireContext().startService(Intent(requireActivity().baseContext, OnClearFromRecentService::class.java))
    }


    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val view: View = inflater.inflate(R.layout.fragment_list, container, false)

        //setContentView(R.layout.activity_main)
        listView = view.findViewById(R.id.listView)
        btnNext = view.findViewById(R.id.btnNext)
        btnNext?.setOnClickListener {
            playNext()
            setSeekBar()
        }
        btnPlay = view.findViewById(R.id.btnPlay)
        btnPlay?.setOnClickListener {
            if(!playbackPaused && musicSrv!!.checkPlayer()){
                musicSrv?.pausePlayer()
                playbackPaused = true
                btnPlay?.setBackgroundResource(R.drawable.pause)
            }
            else if(musicSrv!!.checkPlayer()){
                musicSrv?.go()
                playbackPaused = false
                btnPlay?.setBackgroundResource(R.drawable.play)
            }
        }
        btnPrevious = view.findViewById(R.id.btnPrevious)
        btnPrevious?.setOnClickListener {
            playPrev()
            setSeekBar()
        }

        textSong = view.findViewById(R.id.openSong)
        textStart = view.findViewById(R.id.txtSongStart)
        textEnd = view.findViewById(R.id.txtSongEnd)

        seekBar = view.findViewById(R.id.seekBar)

        val songAdpt = CustomAdapter(songList!!, context)
        listView!!.adapter = songAdpt

        listView!!.setOnItemClickListener { parent, view, position, id ->
            //Start SOng!
            musicSrv!!.setSong(position)
            musicSrv!!.playSong()

            btnPlay?.setBackgroundResource(R.drawable.play)
            currentPosition = position
            textSong?.text = songList!![currentPosition!!].title
            setSeekBar()
        }

        return view
    }

    fun runtimePermission(){
        Dexter.withContext(context).withPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    getSongList()
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {

                }

                override fun onPermissionRationaleShouldBeShown(
                        p0: PermissionRequest?,
                        p1: PermissionToken?
                ) {
                    p1!!.continuePermissionRequest()
                }
            }).check()
    }

    fun getSongList(){

        val musicResolver = context?.contentResolver
        val musicUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val musicCursor: Cursor? = musicResolver!!.query(musicUri, null, null, null, null)

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            val titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)

            //add songs to list
            do {
                val thisId = musicCursor.getLong(idColumn)
                var thisTitle = musicCursor.getString(titleColumn)
                var thisArtist = musicCursor.getString(artistColumn)
                if (thisArtist == null){
                    thisArtist = "<unknown artist>"
                }
                if (thisTitle == null){
                    thisTitle = "<unknown title>"
                }
                songList?.add(Song(thisId, thisTitle, thisArtist))
            } while (musicCursor.moveToNext())
        }
    }

    override fun onStart() {
        super.onStart()
        if(playIntent == null){
            playIntent = Intent(context, MusicService().javaClass)
            context?.bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE)
            context?.startService(playIntent)
        }
    }
    private val musicConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            //get service
            musicSrv = binder.service
            //pass list
            musicSrv!!.setList(songList!!, context!!)
            musicBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            musicBound = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_end -> {
                context?.stopService(playIntent)
                musicSrv = null
                System.exit(0)
            }

            R.id.action_load -> {
                openFileChoosen()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        context?.stopService(playIntent)
        musicSrv?.onUnbind(playIntent)
        musicSrv = null
        requireContext().unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    fun setTextStart(currentPosition: Int){
        GlobalScope.launch(Dispatchers.Main) {
            textStart?.text =createTime(currentPosition)
        }
    }
    fun setTextEnd(totalDuration: Int){
        GlobalScope.launch(Dispatchers.Main) {
            textEnd?.text =createTime(totalDuration)
        }
    }
    fun setSeekBar(){

        updateSeekBar = Thread(){
            val totalDuration = musicSrv?.getDur()
            var currentPosition = 0

            setTextEnd(totalDuration!!)

            while (currentPosition < totalDuration){
                sleep(500)
                currentPosition = musicSrv?.getPosn()!!
                seekBar?.setProgress(currentPosition)
                setTextStart(currentPosition)

                if (textStart!!.text == textEnd!!.text){
                    GlobalScope.launch(Dispatchers.Main) {
                        playNext()
                        setSeekBar()
                        updateSeekBar = null
                    }
                }
            }
        }
        seekBar?.max = musicSrv!!.getDur()
        updateSeekBar?.start()
        seekBar?.progressDrawable?.setColorFilter(resources.getColor(R.color.purple_700), PorterDuff.Mode.MULTIPLY)
        seekBar?.thumb?.setColorFilter(resources.getColor(R.color.purple_700), PorterDuff.Mode.SRC_IN)
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                musicSrv?.seek(seekBar?.progress!!)
            }
        })
    }

    fun createTime(duration: Int): String {

        var time: String = ""
        val min = duration/1000/60
        val sec = duration/1000%60

        time = "$time$min:"

        if (sec < 10) {
            time += "0"
        }

        time += sec

        return time
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode == Activity.RESULT_OK){
            val uri = data?.data

            musicSrv?.playLoadSong(uri!!)
            textSong?.text = "From Files"
            btnPlay?.setBackgroundResource(R.drawable.play)
            playbackPaused = false
            setSeekBar()
        }
    }

    fun openFileChoosen(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, 1)
    }


    //Controller

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onResume() {
        super.onResume()
        if(paused){
            paused = false
        }
    }


    fun playNext(){
        if (currentPosition != null) {
            if (currentPosition!! < songList!!.size-1) {
                musicSrv!!.playNext()
                currentPosition = currentPosition!! +1
                if (playbackPaused) {
                    playbackPaused = false
                }
                textSong?.text = songList!![currentPosition!!].title
                btnPlay?.setBackgroundResource(R.drawable.play)
            }
        }
        else{
            currentPosition = 0
            if (currentPosition!! < songList!!.size-1) {
                musicSrv!!.playNext()
                currentPosition = currentPosition!! +1
                if (playbackPaused) {
                    playbackPaused = false
                }
                textSong?.text = songList!![currentPosition!!].title
                btnPlay?.setBackgroundResource(R.drawable.play)
            }
        }
    }
    fun playPrev() {
        if(currentPosition != null) {
            if (currentPosition!! > 0) {
                musicSrv!!.playPrev()
                currentPosition = currentPosition!! -1
                if (playbackPaused) {
                    playbackPaused = false
                }
                textSong?.text = songList!![currentPosition!!].title
                btnPlay?.setBackgroundResource(R.drawable.play)
            }
        }
        else{
            currentPosition = 0
            if (currentPosition!! > 0) {
                musicSrv!!.playPrev()
                currentPosition = currentPosition!! -1
                if (playbackPaused) {
                    playbackPaused = false
                }
                textSong?.text = songList!![currentPosition!!].title
                btnPlay?.setBackgroundResource(R.drawable.play)
            }
        }
    }

    //Notification
    var broadcastReceiver : BroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent!!.extras!!.getString("actionname")
            val createNotification = CreateNotification()
            when(action){
                createNotification.ACTION_PREV ->{
                    playPrev()
                    setSeekBar()
                }
                createNotification.ACTION_NEXT ->{
                    playNext()
                    setSeekBar()
                }
                createNotification.ACTION_PLAY ->{
                    if(!playbackPaused && musicSrv!!.checkPlayer()){
                        musicSrv?.pausePlayer()
                        playbackPaused = true
                        btnPlay?.setBackgroundResource(R.drawable.pause)
                    }
                    else if(musicSrv!!.checkPlayer()){
                        musicSrv?.go()
                        playbackPaused = false
                        btnPlay?.setBackgroundResource(R.drawable.play)
                    }
                }
            }

        }
    }
}