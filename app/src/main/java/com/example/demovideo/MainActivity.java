package com.example.demovideo;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_VIDEO = 2;
    private static final String KEY_CURRENT_POSITION = "current_position";
    private static final String KEY_CURRENT_URI = "current_uri";

    private VideoView mVideoView;
    private MediaController mediaController;
    private Uri uriVideo;
    private int position = -1;

    ActivityResultLauncher<Intent> pickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getData()!=null){
                        uriVideo=result.getData().getData();
                        mVideoView.setVideoURI(uriVideo);
                        mVideoView.start();
                    }

                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CheckPermissions();
        Button btn_addRaw = findViewById(R.id.btn_addRaw);
        Button btn_addURL = findViewById(R.id.btn_addURL);
        Button btn_addStorage = findViewById(R.id.btn_addStorage);
        Button btn_addGallery = findViewById(R.id.btn_addGallery);
        Button btn_saveGallery = findViewById(R.id.btn_saveGallery);
        Button btn_saveStorage = findViewById(R.id.btn_saveStorage);
        // Find your VideoView in your video main xml layout
        mVideoView = findViewById(R.id.videoView_main);

        // create an object of media controller class
        mediaController = new MediaController(this);
        mediaController.setAnchorView(mVideoView);
        // set the media controller for video view
        mVideoView.setMediaController(mediaController);

        // implement on completion listener on video view
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                makeToast("Thank You...!!!");
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                makeToast("Oops An Error Occur While Playing Video...!!!");
                return false;
            }
        });

        btn_addRaw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.video_sample_7;
                uriVideo = Uri.parse(videoPath);
                // set the path for the video view
                mVideoView.setVideoURI(uriVideo);
                // start a video
                mVideoView.start();
            }
        });

        btn_addURL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String videoPath = "https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4";
                uriVideo = Uri.parse(videoPath);
                // set the path for the video view
                mVideoView.setVideoURI(uriVideo);
                // start a video
                mVideoView.start();
            }
        });

        btn_addStorage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CheckPermissions()) {
                    // Access to Storage
                    Intent selectVideoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    selectVideoIntent.setType("video/*");
                    pickerLauncher.launch(selectVideoIntent);
                }
            }
        });

        btn_addGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CheckPermissions()) {
                    // Access to Gallery
                    Intent selectVideoIntent = new Intent(Intent.ACTION_PICK);
                    selectVideoIntent.setType("video/*");
                    pickerLauncher.launch(selectVideoIntent);
                }
            }
        });

        btn_saveGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uriVideo == null) {
                    makeToast("Please choose video");
                    return;
                }
                saveVideoToGallery(uriVideo);
            }
        });

        btn_saveStorage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uriVideo == null) {
                    makeToast("Please choose video");
                    return;
                }
                saveVideoToStorage(uriVideo);
            }
        });
    }

    private void saveVideoToStorage(Uri _uriVideo) {
        String videoFileName = "video_" + System.currentTimeMillis() + ".mp4";
        try {
            new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/Download/Save").mkdirs();
            File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/Download/Save/" + videoFileName);
            FileOutputStream out = new FileOutputStream(file);
            // Get the already saved video as FileInputStream from here
            InputStream in = getContentResolver().openInputStream(_uriVideo);

            byte[] buf = new byte[8192];
            int len;
            int progress = 0;
            while ((len = in.read(buf)) > 0) {
                progress = progress + len;

                out.write(buf, 0, len);
            }
            out.close();
            in.close();

            makeToast("Saved");
        } catch (Exception e) {
            makeToast("error: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private void saveVideoToGallery(Uri _uriVideo) {
        String videoFileName = "video_" + System.currentTimeMillis() + ".mp4";
        ContentValues valueVideos = new ContentValues();
        valueVideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName);

        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Uri uriSavedVideo = getContentResolver().insert(collection, valueVideos);

        try {
            OutputStream out = getContentResolver().openOutputStream(uriSavedVideo);
            // Get the already saved video as FileInputStream from here
            InputStream in = getContentResolver().openInputStream(_uriVideo);

            byte[] buf = new byte[8192];
            int len;
            int progress = 0;
            while ((len = in.read(buf)) > 0) {
                progress = progress + len;

                out.write(buf, 0, len);
            }
            out.close();
            in.close();
            getContentResolver().update(uriSavedVideo, valueVideos, null, null);
            makeToast("Saved");
        } catch (Exception e) {
            makeToast("error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (uriVideo == null)
            return;
        position = mVideoView.getCurrentPosition();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (uriVideo == null)
            return;
        outState.putInt(KEY_CURRENT_POSITION, position);
        outState.putString(KEY_CURRENT_URI, uriVideo.toString());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        int pos = savedInstanceState.getInt(KEY_CURRENT_POSITION);
        String path = savedInstanceState.getString(KEY_CURRENT_URI);
        uriVideo = Uri.parse(path);

        mVideoView.setVideoURI(uriVideo);
        mVideoView.seekTo(pos);
        mVideoView.start();
    }

    public boolean CheckPermissions() {
        // Check whether user has granted read external storage permission to this activity.
        int readExternalStoragePermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                + ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // If not grant then require read external storage permission.
        if (readExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
            String requirePermission[] = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(MainActivity.this, requirePermission, REQUEST_CODE_PERMISSION);
            return false;
        } else {
            return true;
        }
    }

    private void makeToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

