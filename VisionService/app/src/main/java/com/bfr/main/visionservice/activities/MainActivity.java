package com.bfr.main.visionservice.activities;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bfr.main.visionservice.R;

/**
 * MainActivity est l'activité principale du projet, elle permet de gérer les permissions nécessaires pour le fonctionnement du service par la suite.
 * Les permissions demandées :
 *    + L'appareil photo [CAMERA]
 *    + Le stockage ---- [WRITE_EXTERNAL_STORAGE , READ_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE]
 */
public class MainActivity extends Activity {
    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)
        ){
            finish();
        }
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    private boolean checkPermission(@NonNull int[] grantResults){
        return grantResults[0] != PackageManager.PERMISSION_GRANTED
                || grantResults[1] != PackageManager.PERMISSION_GRANTED
                || grantResults[2] != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQ_ID && checkPermission(grantResults)) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            getApplicationContext(),
                            "Need permissions :"
                                    + Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    + "/"
                                    + Manifest.permission.READ_EXTERNAL_STORAGE
                                    + "/"
                                    + Manifest.permission.CAMERA,
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
            finish();
            return;
        }
        finish();
    }

}