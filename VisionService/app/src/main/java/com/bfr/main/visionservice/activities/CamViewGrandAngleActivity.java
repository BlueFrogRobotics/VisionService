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
 * La classe CamViewGrandAngleActivity représente l'activité de camera "grand-angle" fournie par openCV via CameraActivity.
 * Nous utilisons cette activité pour récupérer la frame openCV "grand-angle"
 * Cette activité se lance de façon transparente sur les autres applications
 * (n'est pas visible + absence de focus pour ne pas bloquer les touches sur les autres applications)
 */
public class CamViewGrandAngleActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2, IDBObserver {

    private static final String TAG = "SERVICE_VISION_CamViewGrandAngleActivity";

    private CameraBridgeViewBase mOpenCvCameraViewGrandAngle;
    private VisionServiceApplication application;
    private boolean streamGrandAngle = true; // Indique s'il faut lancer le streaming de frames grand-Angle [Ecriture sur mémoire partagée] ou non

    /**
     * Callback d'initialisation openCV
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully!");
                    //Choix de caméra Grand angle puis activation du preview
                    mOpenCvCameraViewGrandAngle.setCameraIndex(0);
                    mOpenCvCameraViewGrandAngle.enableView();
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
        setContentView(R.layout.activity_cam_view_grand_angle);

        //Récupération du contexte d'Application
        application = (VisionServiceApplication) getApplicationContext();

        //Enregistrement de l'Observer (pour recevoir les notifications)
        application.registerObserver(this);

        // initialize implementation of CNNExtractorService
        // configure camera listener
        mOpenCvCameraViewGrandAngle = (CameraBridgeViewBase) findViewById(R.id.CameraViewGrandAngle);
        mOpenCvCameraViewGrandAngle.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraViewGrandAngle.setCvCameraViewListener(this);

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
        return Collections.singletonList(mOpenCvCameraViewGrandAngle);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {}

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // take a picture from the camera
        Mat frameGrandAngle = inputFrame.rgba();

        // dos stuff with the frame

        //Enregistrer la frame Grand-Angle sur la classe Application
        application.setFrameGrandAngle(frameGrandAngle);
        application.setFrameGrandAngleCaptured(true);

        /*
         * Conversion de la frame (Mat) en BitMap puis en byte[] pour écriture sur la mémoire partagée (streaming Grand-Angle)
         * Ensuite, envoi d'un broadcast pour notifier la présence d'une nouvelle frame Grand-Angle
         */
        if(streamGrandAngle){
            try {
                // Mat to Bitmap
                Bitmap frameGrandAngleInBitmap = Bitmap.createBitmap(frameGrandAngle.cols(), frameGrandAngle.rows(), Bitmap.Config.ARGB_8888);
                org.opencv.android.Utils.matToBitmap(frameGrandAngle, frameGrandAngleInBitmap,true);

                // Bitmap to byte[]
                byte[] frameGrandAngleInBytes = Utils.getBytesFromBitmap(frameGrandAngleInBitmap);

                // Ecriture sur la mémoire partagée
                application.getSharedMemoryOfStreamFramesGrandAngle().writeBytes(frameGrandAngleInBytes, 0, 0, frameGrandAngleInBytes.length);
                Log.i(TAG, "Ecriture frame Grand-Angle sur la mémoire partagée");

                // Envoi du broadcast pour notifier les autres applications de la présence d'une nouvelle frame Grand-Angle sur la mémoire partagée
                Intent intent_new_frame_grand_angle = new Intent("NEW_FRAME_OPENCV_IS_WRITTEN_GRAND_ANGLE");
                sendBroadcast(intent_new_frame_grand_angle);

            }
            catch (Exception e){
                Log.e(TAG, "Erreur lors de la conversion de la frame grand-angle et l'écriture sur la mémoire partagée : "+e);
            }
        }

        return frameGrandAngle;
    }

    @Override
    public void onCameraViewStopped() {}


    /**
     * Implementation de méthode IDBObserver
     */

    @Override
    public void update(String message) throws IOException {
        if(message != null){

            // Lancer l'écriture des frames Grand-Angle sur la mémoire partagée
            if(message.equals("startStreamGrandAngle")){
                this.streamGrandAngle = true;
            }

            // Arrêter l'écriture des frames Grand-Angle sur la mémoire partagée
            else if(message.equals("stopStreamGrandAngle")){
                this.streamGrandAngle = false;
            }

            // Fermer l'activité
            else if(message.equals("finishCamGrandAngle")){
                finish();
            }

        }
    }
}