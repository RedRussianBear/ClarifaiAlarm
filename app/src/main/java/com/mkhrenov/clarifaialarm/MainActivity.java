package com.mkhrenov.clarifaialarm;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_TAKE_PHOTO = 1;
    private String mCurrentPhotoPath = null;
    private String object;
    private ClarifaiChecker checker;


    private class ClarifaiTask extends AsyncTask<File, Integer, Boolean> {
        protected Boolean doInBackground(File... images) {
            for (File image : images)
                if (checker.imageContains(image, object))
                    return true;
            return false;
        }

        protected void onPostExecute(Boolean result) {
            TextView response = findViewById(R.id.response);

            if (result) {
                response.setText("Hooray!");
            } else {
                response.setText("Nope");
            }
            (new File(mCurrentPhotoPath)).delete();
            mCurrentPhotoPath = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checker = new ClarifaiChecker();
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getFilesDir();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void dispatchTakePictureIntent(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.mkhrenov.clarifaialarm.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EditText objectView = findViewById(R.id.object);
        object = objectView.getText().toString();
        if (mCurrentPhotoPath != null) {
            new ClarifaiTask().execute(new File(mCurrentPhotoPath));
        }
    }


}
