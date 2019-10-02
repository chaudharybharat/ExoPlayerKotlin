package com.example.exoplayer

import android.net.Uri
import android.os.Handler
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.google.android.exoplayer2.util.Util

import java.io.File
import java.util.Formatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var exoPlayer: SimpleExoPlayer? = null
    private val eventListener = object : ExoPlayer.EventListener {
        override fun onTimelineChanged(timeline: Timeline, manifest: Any?) {
            Log.i(TAG, "onTimelineChanged")
        }

        override fun onTracksChanged(
            trackGroups: TrackGroupArray,
            trackSelections: TrackSelectionArray
        ) {
            Log.i(TAG, "onTracksChanged")
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            Log.i(TAG, "onLoadingChanged")
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.i(
                TAG, "onPlayerStateChanged: playWhenReady = " + playWhenReady.toString()
                        + " playbackState = " + playbackState
            )
            when (playbackState) {
                ExoPlayer.STATE_ENDED -> {
                    Log.i(TAG, "Playback ended!")
                    //Stop playback and return to start position
                    setPlayPause(false)
                    exoPlayer!!.seekTo(0)
                }
                ExoPlayer.STATE_READY -> {
                    Log.i(
                        TAG, "ExoPlayer ready! pos: " + exoPlayer!!.currentPosition
                                + " max: " + stringForTime(exoPlayer!!.duration.toInt())
                    )
                    setProgress()
                }
                ExoPlayer.STATE_BUFFERING -> Log.i(TAG, "Playback buffering!")
                ExoPlayer.STATE_IDLE -> Log.i(TAG, "ExoPlayer idle!")
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            Log.i(TAG, "onPlaybackError: " + error.message)
        }

        override fun onPositionDiscontinuity() {
            Log.i(TAG, "onPositionDiscontinuity")
        }
    }

    private var seekPlayerProgress: SeekBar? = null
    private var handler: Handler? = null
    private var btnPlay: ImageButton? = null
    private var txtCurrentTime: TextView? = null
    private var txtEndTime: TextView? = null
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val f_ext_files_dir = getExternalFilesDir(null)

        val fileDialog = FileDialog(this, f_ext_files_dir!!, "")
        fileDialog.addFileListener(object : FileDialog.FileSelectedListener {
            override fun fileSelected(file: File?) {
                Log.i("File selected:", file!!.absolutePath)
                prepareExoPlayerFromFileUri(Uri.fromFile(file))

                /*
                try {
                    FileInputStream inputStream = new FileInputStream(file);
                    byte[] fileData = new byte[(int)file.length()];
                    Log.i(TAG,"Data before read: "+fileData.length);
                    int bytesRead = inputStream.read(fileData);
                    Log.i(TAG,"Bytes read: "+bytesRead);
                    if(bytesRead>0) {
                        prepareExoPlayerFromByteArray(fileData);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                */
            }
        })
        //fileDialog.showDialog();

        //prepareExoPlayerFromRawResourceUri(RawResourceDataSource.buildRawResourceUri(R.raw.audio));

        // prepareExoPlayerFromURL(Uri.parse("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"));
        prepareExoPlayerFromURL(Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"))
    }

    //TODO
    private fun prepareExoPlayerFromByteArray(data: ByteArray) {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(
            this,
            DefaultTrackSelector(null),
            DefaultLoadControl()
        )
        exoPlayer!!.addListener(eventListener)

        val byteArrayDataSource = MByteArrayDataSource(data)
        Log.i(TAG, "ByteArrayDataSource constructed.")
        /*
        DataSpec dataSpec = new DataSpec(byteArrayDataSource.getUri());
        try {
            byteArrayDataSource.open(dataSpec);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

        val factory = DataSource.Factory { byteArrayDataSource }
        Log.i(TAG, "DataSource.Factory constructed.")

        val audioSource = ExtractorMediaSource(
            byteArrayDataSource.uri,
            factory, DefaultExtractorsFactory(), null, null
        )
        Log.i(TAG, "Audio source constructed.")
        exoPlayer!!.prepare(audioSource)
        initMediaControls()
    }

    /**
     * Prepares exoplayer for audio playback from a local file
     * @param uri
     */
    private fun prepareExoPlayerFromFileUri(uri: Uri) {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(
            this,
            DefaultTrackSelector(null),
            DefaultLoadControl()
        )
        exoPlayer!!.addListener(eventListener)

        val dataSpec = DataSpec(uri)
        val fileDataSource = FileDataSource()
        try {
            fileDataSource.open(dataSpec)
        } catch (e: FileDataSource.FileDataSourceException) {
            e.printStackTrace()
        }

        val factory = DataSource.Factory { fileDataSource }
        val audioSource = ExtractorMediaSource(
            fileDataSource.uri,
            factory, DefaultExtractorsFactory(), null, null
        )

        exoPlayer!!.prepare(audioSource)
        initMediaControls()
    }


    /**
     * Prepares exoplayer for audio playback from a remote URL audiofile. Should work with most
     * popular audiofile types (.mp3, .m4a,...)
     * @param uri Provide a Uri in a form of Uri.parse("http://blabla.bleble.com/blublu.mp3)
     */
    private fun prepareExoPlayerFromURL(uri: Uri) {

        val trackSelector = DefaultTrackSelector()

        val loadControl = DefaultLoadControl()

        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl)

        val dataSourceFactory =
            DefaultDataSourceFactory(this, Util.getUserAgent(this, "exoplayer2example"), null)
        val extractorsFactory = DefaultExtractorsFactory()
        val audioSource =
            ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null)
        exoPlayer!!.addListener(eventListener)

        exoPlayer!!.prepare(audioSource)
        initMediaControls()
    }

    private fun prepareExoPlayerFromRawResourceUri(uri: Uri) {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(
            this,
            DefaultTrackSelector(null),
            DefaultLoadControl()
        )
        exoPlayer!!.addListener(eventListener)

        val dataSpec = DataSpec(uri)
        val rawResourceDataSource = RawResourceDataSource(this)
        try {
            rawResourceDataSource.open(dataSpec)
        } catch (e: RawResourceDataSource.RawResourceDataSourceException) {
            e.printStackTrace()
        }

        val factory = DataSource.Factory { rawResourceDataSource }

        val audioSource = ExtractorMediaSource(
            rawResourceDataSource.uri,
            factory, DefaultExtractorsFactory(), null, null
        )

        exoPlayer!!.prepare(audioSource)
        initMediaControls()
    }

    private fun initMediaControls() {
        initPlayButton()
        initSeekBar()
        initTxtTime()
    }

    private fun initPlayButton() {
        btnPlay = findViewById<View>(R.id.btnPlay) as ImageButton
        btnPlay!!.requestFocus()
        btnPlay!!.setOnClickListener { setPlayPause(!isPlaying) }
    }

    /**
     * Starts or stops playback. Also takes care of the Play/Pause button toggling
     * @param play True if playback should be started
     */
    private fun setPlayPause(play: Boolean) {
        isPlaying = play
        exoPlayer!!.playWhenReady = play
        if (!isPlaying) {
            btnPlay!!.setImageResource(android.R.drawable.ic_media_play)
        } else {
            setProgress()
            btnPlay!!.setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    private fun initTxtTime() {
        txtCurrentTime = findViewById<View>(R.id.time_current) as TextView
        txtEndTime = findViewById<View>(R.id.player_end_time) as TextView
    }

    private fun stringForTime(timeMs: Int): String {
        val mFormatBuilder: StringBuilder
        val mFormatter: Formatter
        mFormatBuilder = StringBuilder()
        mFormatter = Formatter(mFormatBuilder, Locale.getDefault())
        val totalSeconds = timeMs / 1000

        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600

        mFormatBuilder.setLength(0)
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    private fun setProgress() {
        // seekPlayerProgress.setProgress(0);
        seekPlayerProgress!!.max = exoPlayer!!.duration.toInt() / 1000
        txtCurrentTime!!.text = stringForTime(exoPlayer!!.currentPosition.toInt())
        txtEndTime!!.text = stringForTime(exoPlayer!!.duration.toInt())

        if (handler == null) handler = Handler()
        //Make sure you update Seekbar on UI thread
        handler!!.post(object : Runnable {
            override fun run() {
                if (exoPlayer != null && isPlaying) {
                    seekPlayerProgress!!.max = exoPlayer!!.duration.toInt() / 1000
                    val mCurrentPosition = exoPlayer!!.currentPosition.toInt() / 1000
                    seekPlayerProgress!!.progress = mCurrentPosition
                    txtCurrentTime!!.text = stringForTime(exoPlayer!!.currentPosition.toInt())
                    txtEndTime!!.text = stringForTime(exoPlayer!!.duration.toInt())

                    handler!!.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun initSeekBar() {
        seekPlayerProgress = findViewById<View>(R.id.mediacontroller_progress) as SeekBar
        seekPlayerProgress!!.requestFocus()

        seekPlayerProgress!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) {
                    // We're not interested in programmatically generated changes to
                    // the progress bar's position.
                    return
                }

                exoPlayer!!.seekTo((progress * 1000).toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

        seekPlayerProgress!!.max = 0
        seekPlayerProgress!!.max = exoPlayer!!.duration.toInt() / 1000

    }

    companion object {

        private val TAG = "MainActivity"
    }
}
