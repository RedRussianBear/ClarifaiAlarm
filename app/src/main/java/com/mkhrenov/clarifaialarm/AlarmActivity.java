package com.mkhrenov.clarifaialarm;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.List;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;

public class AlarmActivity extends AppCompatActivity {
    static final int REQUEST_TAKE_PHOTO = 1;
    private String photoPath = null;
    private String object;
    MediaPlayer mediaPlayer;
    TextView info;

    private class ClarifaiTask extends AsyncTask<File, Integer, Boolean> {


        protected Boolean doInBackground(File... images) {
            info.setText("Processing...");

            ClarifaiClient client = new ClarifaiBuilder("YOUR_API_TOKEN").buildSync();
            List<ClarifaiOutput<Concept>> predictionResults;

            for (File image : images) {
                predictionResults = client.getDefaultModels().generalModel().predict()
                        .withInputs(ClarifaiInput.forImage(image))
                        .executeSync()
                        .get();

                for (ClarifaiOutput<Concept> result : predictionResults) {
                    for (Concept datum : result.data()) {

                        if (datum.name().contains(object.toLowerCase()))
                            return true;
                    }
                }
            }

            return false;
        }

        protected void onPostExecute(Boolean result) {
            // Delete photo
            (new File(photoPath)).delete();
            photoPath = null;

            // If image contained object, close the AlarmActivity
            if (result) {
                info.setText("Success!");
                finish();
            } else info.setText("Try again...");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        object = getIntent().getStringExtra(MainActivity.EXTRA_MESSAGE);
        info = findViewById(R.id.infoText);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Uri myUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE);
        mediaPlayer = new MediaPlayer();

        class Listener implements MediaPlayer.OnPreparedListener {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        }
        mediaPlayer.setOnPreparedListener(new Listener());

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(getApplicationContext(), myUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.prepareAsync();

        info.setText("Snap a " + object + "!");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void takePicture(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            try {
                File storageDir = getFilesDir();
                photoFile = File.createTempFile(
                        "SNAPSHOT",
                        ".jpg",
                        storageDir
                );

                photoPath = photoFile.getAbsolutePath();
            } catch (IOException ex) {
                return;
            }

            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.mkhrenov.clarifaialarm.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (photoPath != null) {
            new ClarifaiTask().execute(new File(photoPath));
        }
    }

}
