package com.alexkang.loopboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 0;
    private static final int FOOTER_SIZE_DP = 360;
    private static final String[] PERMISSIONS =
            {Manifest.permission.RECORD_AUDIO};

    //private final ArrayList<ImportedSample> importedSamples = new ArrayList<>();
    private final ArrayList<RecordedSample> recordedSamples = new ArrayList<>();
    private Recorder recorder;
    private final SampleListAdapter sampleListAdapter =
            new SampleListAdapter(this, recordedSamples);
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private ExecutorService executorService;


    // ------- Activity lifecycle methods -------

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }

        executorService = Executors.newCachedThreadPool();
        checkPermissions(); //initialize recorder

        // Retrieve UI elements.
        ListView sampleList = findViewById(R.id.sound_list);
        Button recordButton = findViewById(R.id.record_button);

        // Initialize the sample list.
        sampleList.setAdapter(sampleListAdapter);

        // Add some footer space at the bottom of the sample list.
        View footer = new View(this);
        footer.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, FOOTER_SIZE_DP));
        footer.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        sampleList.addFooterView(footer, null, false);

        // Define the record button behavior. Tap and hold to record, release to stop and save.
        recordButton.setOnTouchListener((view, motionEvent) -> {
            // Just in case we tap the record button too early
            if(recorder == null) return true;

            int action = motionEvent.getAction();

            if (action == MotionEvent.ACTION_DOWN) {
                view.setPressed(true);

                // Make sure we haven't hit our maximum number of recordings before proceeding.
                if (recordedSamples.size() > Utils.MAX_SAMPLES) {
                    displaySnackbarDialog("Cannot create any more samples");
                } else {
                    recorder.startRecording(recordedBytes -> saveRecording(recordedBytes));
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.setPressed(false);
                recorder.stopRecording();
            }

            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        //refreshRecordings();
    }

    @Override
    public void onPause() {
        super.onPause();
        //stopAllSamples();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        shutdownSamples();

        stopMediaProjectionService();

        recorder.shutdown();
        executorService.shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_delete){
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.confirm_delete))
                    .setPositiveButton(R.string.yes, (dialog, which) -> deleteAllRecordings())
                    .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                    .show();
            return true;
        }
        else if(id == R.id.action_stop){
            // Stop all currently playing samples.
            stopAllSamples();
            return true;
        }
        else if(id == R.id.action_playAll){
            playAllSamples();
            return true;
        }
        else if(id == R.id.action_reRecordAll){
            reRecordAllSamples();
            return true;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSION_REQUEST_CODE || grantResults.length != PERMISSIONS.length) {
            return;
        }

        // Make sure the audio recording permission was granted.
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(Manifest.permission.RECORD_AUDIO) &&
                    grantResults[i] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.error_permission, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        // All permissions have been granted. Proceed with the app. Also remember to refresh the
        // recorder in case the record audio permission was recently granted.
        // recorder.refresh();
        //refreshRecordings();
    }


    // ------- Private methods -------

    private void checkPermissions() {
        // Check all permissions to see if they're granted.
        boolean permissionsGranted = true;
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_DENIED) {
                permissionsGranted = false;
                break;
            }
        }

        // If any permissions aren't granted, make a request.
        if (!permissionsGranted) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
        }


        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            displaySnackbarDialog("Internal audio capture requires Android 10 or above.");
            recorder = new Recorder(null,executorService); //Microphone mode
        } else {
            // create a prompt to ask for permission to capture audio
            Intent intent = mediaProjectionManager.createScreenCaptureIntent();
            this.startActivityForResult(intent, PERMISSION_REQUEST_CODE);
        }
    }

    private void updateTutorialVisibility() {
        //if (importedSamples.size() > 0 || recordedSamples.size() > 0)
        if (recordedSamples.size() > 0) {
            // If any samples already exist, remove the tutorial text.
            findViewById(R.id.tutorial).setVisibility(View.GONE);
        } else {
            findViewById(R.id.tutorial).setVisibility(View.VISIBLE);
        }
    }

    private void refreshRecordings() {
        shutdownSamples();
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
        ArrayList<String> samplesList = new ArrayList<>();
        samplesList.addAll(Arrays.asList(fileList()));
        Collections.sort(samplesList);
        //Log.d("debug:","now if you'll excuse me...");
        //for (String x: samplesList) Log.d("debug:",x);
        for (String x : samplesList) {
            //Log.d("debug",x);
            RecordedSample recordedSample = RecordedSample.openSavedSample(this, x, mediaProjection,executorService);
            if (recordedSample != null) {
                recordedSamples.add(recordedSample);
            }
        }

        // Tell the ListView to refresh.
        sampleListAdapter.notifyDataSetChanged();
        updateTutorialVisibility();
    }

    private void saveRecording(byte[] recordedBytes) {
        String name = String.format(Locale.ENGLISH, "Sample %d", recordedSamples.size() + 1);
        if (Utils.saveRecording(getBaseContext(), name, recordedBytes)) {
            runOnUiThread(() -> {
                recordedSamples
                        .add(RecordedSample
                                .openSavedSample(this, name, mediaProjection,executorService));
                Collections.sort(recordedSamples, (e1, e2) -> e1.getName().compareTo(e2.getName()));
                //Log.d("debug:","now if you'll excuse me...");
                //for (RecordedSample x: recordedSamples) Log.d("debug:",x.getName());
                sampleListAdapter.notifyDataSetChanged();
                updateTutorialVisibility();
            });
        } else {
            displaySnackbarDialog("An error occurred while saving");
        }


    }

    private void stopAllSamples() {
        /*for (Sample sample : importedSamples) {
            sample.stop();
        }*/
        for (Sample sample : recordedSamples) {
            sample.stop();
            sample.stopRandomMod();
            sample.stopSineMod();
            sample.stopSawMod();
            sample.stopReRecording();
        }

        // Refresh the list to update button states.
        sampleListAdapter.notifyDataSetChanged();
    }

    private void playAllSamples() {
        for (Sample sample : recordedSamples) {
            if(!sample.isLooping()) {
                sample.play(true);
            }
        }
        sampleListAdapter.notifyDataSetChanged();
    }

    private void reRecordAllSamples() {
        boolean doDelay = false; //todo
        int delay = 250; //todo

        Thread reRecordThread = new Thread(() -> {
            for (Sample sample : recordedSamples) {
                if(!sample.isReRecording()) {
                    sample.startReRecording(getBaseContext());
                }
                runOnUiThread(sampleListAdapter::notifyDataSetChanged);

                if(doDelay) {
                    try {
                        Thread.sleep(delay);

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        });
        reRecordThread.start();
    }

    private void shutdownSamples() {
        // First, stop all currently playing samples.
        stopAllSamples();

        // Call shutdown on each sample.
        /*for (Sample sample : importedSamples) {
            sample.shutdown();
        }*/
        for (Sample sample : recordedSamples) {
            sample.shutdown();
        }

        // Clear the lists.
        //importedSamples.clear();
        recordedSamples.clear();
    }

    private void deleteAllRecordings() {
        // Stop playing all samples.
        shutdownSamples();

        // Delete all recordings.
        for (String fileName : fileList()) {
            deleteFile(fileName);
        }

        // Update the UI.
        refreshRecordings();
        displaySnackbarDialog("All recorded samples removed");
    }

    //Audio capture code
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            //should not happen
            return;
        }
        if (resultCode == RESULT_CANCELED) {
            displaySnackbarDialog("Audio capture not granted. Switching to Microphone mode.");
            //mediaProjection is null; recorder will automatically use microphone
            recorder = new Recorder(null, executorService);

        } else if (resultCode == RESULT_OK) {
            startMediaProjectionService();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isMyServiceRunning()) {
                    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                    if (mediaProjection != null) {
                        displaySnackbarDialog("Audio capture granted.");
                        recorder = new Recorder(mediaProjection, executorService);
                    } else {
                        displaySnackbarDialog("Failed to grant audio capture. falling back to Microphone mode.");
                        recorder = new Recorder(null, executorService);
                    }
                }
            }, 200);
        }
    }

    private void displaySnackbarDialog(String text){
        Snackbar.make(
                findViewById(R.id.root_layout),
                text,
                Snackbar.LENGTH_SHORT).show();
    }

    private void startMediaProjectionService(){
        Intent startIntent = new Intent(this, MediaProjectionService.class);
        //Intent startIntent = getIntent();
        startIntent.setAction(Utils.ACTION.STARTFOREGROUND_ACTION);
        ContextCompat.startForegroundService(this, startIntent);
    }

    private void stopMediaProjectionService(){
        Intent stopIntent = new Intent(MainActivity.this, MediaProjectionService.class);
        stopIntent.setAction(Utils.ACTION.STOPFOREGROUND_ACTION);
        startService(stopIntent);
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MediaProjectionService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
