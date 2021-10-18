package com.bfr.main.visionservice.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.bfr.main.visionservice.R;
import com.bfr.main.visionservice.application.VisionServiceApplication;
import com.bfr.main.visionservice.observer.IDBObserver;
import com.bfr.main.visionservice.utils.Utils;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


/**
 * La classe CamViewZoomActivity représente l'activité de camera "Zoom" fournie par openCV via CameraActivity.
 * Nous utilisons cette activité pour récupérer la frame openCV "Zoom"
 * Cette activité se lance de façon transparente sur les autres applications
 * (n'est pas visible + absence de focus pour ne pas bloquer les touches sur les autres applications)
 */
public class CamViewZoomActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2, IDBObserver {

    private static final String TAG = "SERVICE_VISION_CamViewZoomActivity";

    private CameraBridgeViewBase mOpenCvCameraViewZoom;
    private VisionServiceApplication application;
    private boolean streamZoom = true; // Indique s'il faut lancer le streaming de frames zoom [Ecriture sur mémoire partagée] ou non

    /**
     * Callback d'initialisation openCV
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully!");
                    //Choix de caméra Zoom puis activation du preview
                    mOpenCvCameraViewZoom.setCameraIndex(1);
                    mOpenCvCameraViewZoom.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    /**
     * Initialisations
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Désactivation du focus sur l'activité pour ne pas bloquer les touches sur les autres applications
        getWindow().addFlags( WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        //Liaison au layout
        setContentView(R.layout.activity_cam_view_zoom);

        //Récupération du contexte d'Application
        application = (VisionServiceApplication) getApplicationContext();

        //Enregistrement de l'Observer (pour recevoir les notifications)
        application.registerObserver(this);

        // initialize implementation of CNNExtractorService
        // configure camera listener
        mOpenCvCameraViewZoom = (CameraBridgeViewBase) findViewById(R.id.CameraViewZoom);
        mOpenCvCameraViewZoom.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraViewZoom.setCvCameraViewListener(this);

    }

    /**
     *  Lancement d'initialisation openCV
     */
    @Override
    public void onResume() {
        super.onResume();
        // OpenCV manager initialization
        OpenCVLoader.initDebug();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    /**
     * Désinscription de l'Observer
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        application.removeObserver(this);
    }


    /**
     * Implementation des méthodes openCV
     */

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraViewZoom);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {}

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // take a picture from the camera
        Mat frameZoom = inputFrame.rgba();

        // dos stuff with the frame

        //Enregistrer la frame Zoom sur la classe Application
        application.setFrameZoom(frameZoom);
        application.setFrameZoomCaptured(true);

        /*
         * Conversion de la frame (Mat) en BitMap puis en byte[] pour écriture sur la mémoire partagée (streaming Zoom)
         * Ensuite, envoi d'un broadcast pour notifier la présence d'une nouvelle frame Zoom
         */
        if(streamZoom){
            try {
                // Mat to Bitmap
                Bitmap frameZoomInBitmap = Bitmap.createBitmap(frameZoom.cols(), frameZoom.rows(), Bitmap.Config.ARGB_8888);
                org.opencv.android.Utils.matToBitmap(frameZoom, frameZoomInBitmap,true);

                // Bitmap to byte[]
                byte[] frameZoomInBytes = Utils.getBytesFromBitmap(frameZoomInBitmap);

                // Ecriture sur la mémoire partagée
                application.getSharedMemoryOfStreamFramesZoom().writeBytes(frameZoomInBytes, 0, 0, frameZoomInBytes.length);
                Log.i(TAG, "Ecriture frame Zoom sur la mémoire partagée");

                // Envoi du broadcast pour notifier les autres applications de la présence d'une nouvelle frame Zoom sur la mémoire partagée
                Intent intent_new_frame_zoom = new Intent("NEW_FRAME_OPENCV_IS_WRITTEN_ZOOM");
                sendBroadcast(intent_new_frame_zoom);

            }
            catch (Exception e){
                Log.e(TAG, "Erreur lors de la conversion de la frame zoom et l'écriture sur la mémoire partagée : "+e);
            }
        }

        return frameZoom;
    }

    @Override
    public void onCameraViewStopped() {}


    /**
     * Implementation de méthode IDBObserver
     */

    @Override
    public void update(String message) throws IOException {
        if(message != null){

            // Lancer l'écriture des frames Zoom sur la mémoire partagée
            if(message.equals("startStreamZoom")){
                this.streamZoom = true;
            }

            // Arrêter l'écriture des frames Zoom sur la mémoire partagée
            else if(message.equals("stopStreamZoom")){
                this.streamZoom = false;
            }

            // Fermer l'activité
            else if(message.equals("finishCamZoom")){
                finish();
            }

        }
    }
}