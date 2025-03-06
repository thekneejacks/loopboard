package com.alexkang.loopboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioPlaybackCaptureConfiguration
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    //private final ArrayList<ImportedSample> importedSamples = new ArrayList<>();
    private val recordedSamples = ArrayList<RecordedSample>()
    private lateinit var recorder: Recorder
    private val sampleListAdapter = SampleListAdapter(this, recordedSamples)

    //private AudioPlaybackCaptureConfiguration audioPlaybackCaptureConfiguration;
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private lateinit var topRightMenu: Menu
    private var isAudioCaptureMode = false


    // ------- Activity lifecycle methods -------
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaProjectionManager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        }

        //executorService = Executors.newCachedThreadPool();
        LoopboardApplication.getApplication(this).executorService = Executors.newCachedThreadPool()
        checkPermissions() //initialize recorder

        // Retrieve UI elements.
        val sampleList = findViewById<ListView>(R.id.sound_list)
        val recordButton = findViewById<Button>(R.id.record_button)

        // Initialize the sample list.
        sampleList.adapter = sampleListAdapter

        // Add some footer space at the bottom of the sample list.
        val footer = View(this)
        footer.layoutParams =
            AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, FOOTER_SIZE_DP)
        footer.setBackgroundColor(resources.getColor(android.R.color.transparent))
        sampleList.addFooterView(footer, null, false)

        // Define the record button behavior. Tap and hold to record, release to stop and save.
        recordButton.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            // Just in case we tap the record button too early
            if (!::recorder.isInitialized) return@setOnTouchListener true

            val action = motionEvent.action

            if (action == MotionEvent.ACTION_DOWN) {
                view.isPressed = true

                // Make sure we haven't hit our maximum number of recordings before proceeding.
                if (recordedSamples.size > Utils.MAX_SAMPLES) {
                    displaySnackbarDialog("Cannot create any more samples")
                } else {
                    recorder.startRecording { recordedBytes: ByteArray ->
                        saveRecording(
                            recordedBytes
                        )
                    }
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.isPressed = false
                recorder.stopRecording()
            }
            true
        }
    }

    public override fun onStart() {
        super.onStart()
        //refreshRecordings();
    }

    public override fun onPause() {
        super.onPause()
        //stopAllSamples();
    }

    public override fun onDestroy() {
        super.onDestroy()

        shutdownSamples()

        stopMediaProjectionService()

        recorder.shutdown()
        LoopboardApplication.getApplication(this).shutdown()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        topRightMenu = menu
        hideSwitchButtons()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_delete -> {
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.confirm_delete))
                    .setPositiveButton(
                        R.string.yes
                    ) { dialog: DialogInterface?, which: Int -> deleteAllRecordings() }
                    .setNegativeButton(
                        R.string.no
                    ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                    .show()
                return true
            }
            R.id.action_stop -> {
                // Stop all currently playing samples.
                stopAllSamples()
                return true
            }
            R.id.action_playAll -> {
                playAllSamples()
                return true
            }
            R.id.action_reRecordAll -> {
                reRecordAllSamples()
                return true
            }
            R.id.action_switchToMicrophoneMode -> {
                switchToMicrophoneMode()
                return true
            }
            R.id.action_switchToAudioCaptureMode -> {
                switchToAudioCaptureMode()
                return true
            }
            R.id.action_grantAudioCapture -> {
                grantAudioCapture()
                return true
            }
            else -> return true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST_CODE || grantResults.size != PERMISSIONS.size) {
            return
        }

        // Make sure the audio recording permission was granted.
        for (i in permissions.indices) {
            if (permissions[i] == Manifest.permission.RECORD_AUDIO &&
                grantResults[i] == PackageManager.PERMISSION_DENIED
            ) {
                Toast.makeText(this, R.string.error_permission, Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }

        // All permissions have been granted. Proceed with the app. Also remember to refresh the
        // recorder in case the record audio permission was recently granted.
        // recorder.refresh();
        //refreshRecordings();
    }


    // ------- Private methods -------
    private fun checkPermissions() {
        // Check all permissions to see if they're granted.
        var permissionsGranted = true
        for (permission in PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_DENIED
            ) {
                permissionsGranted = false
                break
            }
        }

        // If any permissions aren't granted, make a request.
        if (!permissionsGranted) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
        }


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            displaySnackbarDialog("Internal audio capture requires Android 10 or above.")
            LoopboardApplication.getApplication(this).audioPlaybackCaptureConfiguration =
                getApplicationAudioPlaybackCaptureConfiguration(mediaProjection)
            recorder = Recorder(this, isAudioCaptureMode) //Microphone mode
            hideSwitchButtons()
        } else {
            // create a prompt to ask for permission to capture audio
            grantAudioCapture()
        }
    }

    private fun updateTutorialVisibility() {
        //if (importedSamples.size() > 0 || recordedSamples.size() > 0)
        if (recordedSamples.size > 0) {
            // If any samples already exist, remove the tutorial text.
            findViewById<View>(R.id.tutorial).visibility = View.GONE
        } else {
            findViewById<View>(R.id.tutorial).visibility = View.VISIBLE
        }
    }

    private fun refreshRecordings() {
        shutdownSamples()

        // First, add user imported audio files to the top of our sample list. Also, create the
        // LoopBoard directory if it doesn't already exist.
        /*try {
            File importedDir = new File(Utils.IMPORTED_SAMPLE_PATH);
            importedDir.mkdirs();
            for (File file : importedDir.listFiles()) {
                if (Utils.isSupportedSampleFile(file)) {
                    importedSamples.add(new ImportedSample(this, file));
                }
            }
        } catch (NullPointerException e) {
            // No-op. This means that external storage permission was not granted.
        }*/

        // Next, add samples recorded from this app.
        val samplesList = ArrayList<String>()
        samplesList.addAll(listOf(*fileList()))
        samplesList.sort()
        //Log.d("debug:","now if you'll excuse me...");
        //for (String x: samplesList) Log.d("debug:",x);
        for (x in samplesList) {
            val recordedSample = RecordedSample.openSavedSample(this, x, isAudioCaptureMode)
            if (recordedSample != null) {
                recordedSamples.add(recordedSample)
            }
        }

        // Tell the ListView to refresh.
        sampleListAdapter.notifyDataSetChanged()
        updateTutorialVisibility()
    }

    private fun saveRecording(recordedBytes: ByteArray) {
        val name = String.format(Locale.ENGLISH, "Sample %d", recordedSamples.size + 1)
        if (Utils.saveRecording(baseContext, name, recordedBytes)) {
            runOnUiThread {
                RecordedSample
                    .openSavedSample(this, name, isAudioCaptureMode)?.let {
                        recordedSamples
                            .add(
                                it
                            )
                    }
                recordedSamples.sortWith { e1: RecordedSample, e2: RecordedSample ->
                    e1.name.compareTo(
                        e2.name
                    )
                }
                //Log.d("debug:","now if you'll excuse me...");
                //for (RecordedSample x: recordedSamples) Log.d("debug:",x.getName());
                sampleListAdapter.notifyDataSetChanged()
                updateTutorialVisibility()
            }
        } else {
            displaySnackbarDialog("An error occurred while saving")
        }
    }

    private fun stopAllSamples() {
        /*for (Sample sample : importedSamples) {
            sample.stop();
        }*/
        for (sample in recordedSamples) {
            sample.stop()
            sample.stopRandomMod()
            sample.stopSineMod()
            sample.stopSawMod()
            sample.stopReRecording()
        }

        // Refresh the list to update button states.
        sampleListAdapter.notifyDataSetChanged()
    }

    private fun playAllSamples() {
        for (sample in recordedSamples) {
            if (!sample.isLooping) {
                sample.play(true)
            }
        }
        sampleListAdapter.notifyDataSetChanged()
    }

    private fun reRecordAllSamples() {
        val doDelay = false //todo
        val delay = 250 //todo

        val reRecordThread = Thread {
            for (sample in recordedSamples) {
                if (!sample.isReRecording) {
                    sample.startReRecording(baseContext)
                }
                runOnUiThread { sampleListAdapter.notifyDataSetChanged() }

                if (doDelay) {
                    try {
                        Thread.sleep(delay.toLong())
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                }
            }
        }
        reRecordThread.start()
    }

    private fun shutdownSamples() {
        // First, stop all currently playing samples.
        stopAllSamples()

        // Call shutdown on each sample.
        /*for (Sample sample : importedSamples) {
            sample.shutdown();
        }*/
        for (sample in recordedSamples) {
            sample.shutdown()
        }

        // Clear the lists.
        //importedSamples.clear();
        recordedSamples.clear()
    }

    private fun deleteAllRecordings() {
        // Stop playing all samples.
        shutdownSamples()

        // Delete all recordings.
        for (fileName in fileList()) {
            deleteFile(fileName)
        }

        // Update the UI.
        refreshRecordings()
        displaySnackbarDialog("All recorded samples removed")
    }

    //Audio capture code
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (::recorder.isInitialized) recorder.shutdown()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            //should not happen
            return
        }
        if (resultCode == RESULT_CANCELED) {
            displaySnackbarDialog("Audio capture not granted. Switching to Microphone only mode.")
            //mediaProjection is null; recorder will automatically use microphone
            LoopboardApplication.getApplication(this).audioPlaybackCaptureConfiguration =
                getApplicationAudioPlaybackCaptureConfiguration(mediaProjection)
            recorder = Recorder(this, false)
            for (sample in recordedSamples) {
                sample.setAudioPlaybackCaptureConfiguration(this)
                sample.setIsCapturingAudio(false)
            }
            hideSwitchButtons()
        } else if (resultCode == RESULT_OK) {
            startMediaProjectionService()
            Handler(Looper.getMainLooper()).postDelayed({
                if (isMyServiceRunning) {
                    mediaProjection =
                        data?.let { mediaProjectionManager!!.getMediaProjection(resultCode, it) }!!
                    LoopboardApplication.getApplication(this).audioPlaybackCaptureConfiguration =
                        getApplicationAudioPlaybackCaptureConfiguration(mediaProjection)
                    if (mediaProjection != null) {
                        displaySnackbarDialog("Audio capture granted.")
                        recorder = Recorder(this, true)
                        for (sample in recordedSamples) {
                            sample.setAudioPlaybackCaptureConfiguration(this)
                            sample.setIsCapturingAudio(true)
                        }
                    } else {
                        displaySnackbarDialog("Failed to grant audio capture. falling back to Microphone mode.")
                        recorder = Recorder(this, false)
                        for (sample in recordedSamples) {
                            sample.setAudioPlaybackCaptureConfiguration(this)
                            sample.setIsCapturingAudio(false)
                        }
                    }

                    hideSwitchButtons()
                }
            }, 200)
        }
    }

    private fun displaySnackbarDialog(text: String) {
        Snackbar.make(
            findViewById(R.id.root_layout),
            text,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun startMediaProjectionService() {
        val startIntent = Intent(this, MediaProjectionService::class.java)
        //Intent startIntent = getIntent();
        startIntent.setAction(Utils.ACTION.STARTFOREGROUND_ACTION)
        ContextCompat.startForegroundService(this, startIntent)
    }

    private fun stopMediaProjectionService() {
        val stopIntent = Intent(this@MainActivity, MediaProjectionService::class.java)
        stopIntent.setAction(Utils.ACTION.STOPFOREGROUND_ACTION)
        startService(stopIntent)
    }

    private fun switchToMicrophoneMode() {
        stopAllSamples() //to stop rerecording
        //May only switch modes if user granted permission to capture audio at startup
        if (mediaProjection == null) return

        for (sample in recordedSamples) {
            sample.setIsCapturingAudio(false) //Microphone mode
        }
        recorder.isCapturingAudio = false
        hideSwitchButtons()
        displaySnackbarDialog("Switching to Microphone mode.")
    }

    private fun switchToAudioCaptureMode() {
        stopAllSamples() //to stop rerecording
        //May only switch modes if user granted permission to capture audio at startup
        if (mediaProjection == null) return

        for (sample in recordedSamples) {
            sample.setIsCapturingAudio(true) //Audio Capture Mode
        }
        recorder.isCapturingAudio = true
        hideSwitchButtons()
        displaySnackbarDialog("Switching to Audio Capture mode.")
    }

    private fun grantAudioCapture() {
        val intent = mediaProjectionManager!!.createScreenCaptureIntent()
        this.startActivityForResult(intent, PERMISSION_REQUEST_CODE)
    }

    private fun hideSwitchButtons() {
        val microphone = topRightMenu.findItem(R.id.action_switchToMicrophoneMode)
        val audioCapture = topRightMenu.findItem(R.id.action_switchToAudioCaptureMode)
        val grantAudioCapture = topRightMenu.findItem(R.id.action_grantAudioCapture)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            //No audio capture capability; no switch or grant permission buttons
            microphone.setVisible(false)
            audioCapture.setVisible(false)
            grantAudioCapture.setVisible(false)
            isAudioCaptureMode = false
        } else if (mediaProjection == null) {
            //User did not grant permission to capture audio; hide switch buttons, show button to ask for permission
            microphone.setVisible(false)
            audioCapture.setVisible(false)
            grantAudioCapture.setVisible(true)
            isAudioCaptureMode = false
        } else if (!recorder.isCapturingAudio) {
            //User has switched to microphone mode; hide switch to microphone mode button
            microphone.setVisible(false)
            audioCapture.setVisible(true)
            grantAudioCapture.setVisible(false)
            isAudioCaptureMode = false
        } else {
            //User has switched to audio capture mode; hide switch to audio capture mode button
            microphone.setVisible(true)
            audioCapture.setVisible(false)
            grantAudioCapture.setVisible(false)
            isAudioCaptureMode = true
        }
        invalidateOptionsMenu()
    }

    private fun getApplicationAudioPlaybackCaptureConfiguration(mediaProjection: MediaProjection?): AudioPlaybackCaptureConfiguration? {
        var audioPlaybackCaptureConfiguration: AudioPlaybackCaptureConfiguration? = null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || mediaProjection == null) return null
        try {
            val audioPlaybackCaptureConfigurationBuilder =
                AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            audioPlaybackCaptureConfiguration =
                audioPlaybackCaptureConfigurationBuilder.build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return audioPlaybackCaptureConfiguration
    }

    private val isMyServiceRunning: Boolean
        get() {
            val manager =
                getSystemService(ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (MediaProjectionService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 0
        private const val FOOTER_SIZE_DP = 360
        private val PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO)
    }
}
