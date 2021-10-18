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
 * La classe CamViewActivity représente l'activité de camera fournie par openCV via CameraActivity.
 * Nous utilisons cette activité pour récupérer la frame openCV.
 * Cette activité se lance de façon transparente sur les autres applications
 * (n'est pas visible + absence de focus pour ne pas bloquer les touches sur les autres applications)
 */
public class CamViewActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2, IDBObserver {

    private static final String TAG = "SERVICE_VISION_CamViewActivity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private VisionServiceApplication application;
    private int idCamera = 0; //  0: grand-angle  ,  1: Zoom
    private boolean stream = true; // Indique s'il faut lancer le streaming de frames [Ecriture sur mémoire partagée] ou non

    /**
     * Callback d'initialisation openCV
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully!");
                    //Choix de caméra (Grand angle ou Zoom) puis activation du preview
                    mOpenCvCameraView.setCameraIndex(idCamera);
                    mOpenCvCameraView.enableView();
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
        setContentView(R.layout.activity_cam_view);

        //Récupération du contexte d'Application
        application = (VisionServiceApplication) getApplicationContext();

        //Enregistrement de l'Observer (pour recevoir les notifications)
        application.registerObserver(this);

        //Récupération de l'index de caméra à utiliser (grand-angle ou Zoom)
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) idCamera = bundle.getInt("idCamera");

        // initialize implementation of CNNExtractorService
        // configure camera listener
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.CameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

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
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {}

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // take a picture from the camera
        Mat frame = inputFrame.rgba();

        // dos stuff with the frame

        //Enregistrer la frame sur la classe Application
        application.setOpenCVFrame(frame);
        application.setOpenCVFrameCaptured(true);

        /*
         * Conversion de la frame (Mat) en BitMap puis en byte[] pour écriture sur la mémoire partagée (streaming)
         * Ensuite, envoi d'un broadcast pour notifier la présence d'une nouvelle frame
         */
        if(stream){
            try {
                // Mat to Bitmap
                Bitmap frameInBitmap = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
                org.opencv.android.Utils.matToBitmap(frame, frameInBitmap,true);

                // Bitmap to byte[]
                byte[] frameInBytes = Utils.getBytesFromBitmap(frameInBitmap);

                // Ecriture sur la mémoire partagée
                application.getSharedMemoryOfStreamFrames().writeBytes(frameInBytes, 0, 0, frameInBytes.length);
                Log.i(TAG, "Ecriture sur la mémoire partagée");

                // Envoi du broadcast pour notifier les autres applications de la présence d'une nouvelle frame sur la mémoire partagée
                Intent intent_new_frame = new Intent("NEW_FRAME_OPENCV_IS_WRITTEN");
                sendBroadcast(intent_new_frame);

            }
            catch (Exception e){
                Log.e(TAG, "Erreur lors de la conversion de la frame et l'écriture sur la mémoire partagée : "+e);
            }
        }

        return frame;
    }

    @Override
    public void onCameraViewStopped() {}


    /**
     * Implementation de méthode IDBObserver
     */

    @Override
    public void update(String message) throws IOException {
        if(message != null){

            // Lancer l'écriture des frames sur la mémoire partagée
            if(message.equals("startStreamCamOpenCV")){
                this.stream = true;
            }

            // Arrêter l'écriture des frames sur la mémoire partagée
            else if(message.equals("stopStreamCamOpenCV")){
                this.stream = false;
            }

            // Fermer l'activité
            else if(message.equals("finishCamOpenCV")){
                finish();
            }

        }
    }
}