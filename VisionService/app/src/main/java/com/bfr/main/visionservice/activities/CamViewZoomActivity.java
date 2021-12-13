package com.bfr.main.visionservice.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * La classe CamViewZoomActivity représente l'activité de camera "Zoom" fournie par openCV via CameraActivity.
 * Nous utilisons cette activité pour récupérer la frame openCV "Zoom" + infos des tags détectés
 * Cette activité se lance de façon transparente sur les autres applications
 * (n'est pas visible + absence de focus pour ne pas bloquer les touches sur les autres applications)
 */
public class CamViewZoomActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2, IDBObserver {

    private static final String TAG = "SERVICE_VISION_CamViewZoomActivity";

    private CameraBridgeViewBase mOpenCvCameraViewZoom;
    private VisionServiceApplication application;
    private boolean streamZoom = true; // Indique s'il faut lancer le streaming de frames zoom [Ecriture sur mémoire partagée] ou non
    private View decorView;

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
                    String versionRobot = getString(R.string.robot_version);
                    if(versionRobot.equals("4.2")) mOpenCvCameraViewZoom.setCameraIndex(0); //pour le robot 4.2 la caméra zoom correspond au 0
                    else mOpenCvCameraViewZoom.setCameraIndex(1);
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

        //cacher les barres systemUI
        application.hideSystemUI(this);
        decorView=getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if(visibility==0){
                    decorView.setSystemUiVisibility(application.hideSystemUI(CamViewZoomActivity.this));
                }
            }
        });

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


        /*
         * Partie : Détection de tag
         */

        application.setArucoIdsZoom(new Mat());
        application.setArucoCornersZoom(new ArrayList<Mat>());

        //convert
        Imgproc.cvtColor(frameZoom, frameZoom, Imgproc.COLOR_RGBA2RGB);

        // Definition of dictionary and params
        Dictionary arucoDict = Aruco.getPredefinedDictionary(Aruco.DICT_APRILTAG_36h11);
        DetectorParameters arucoParams = DetectorParameters.create();

        // Detect Marker
        Aruco.detectMarkers(frameZoom, arucoDict, application.getArucoCornersZoom(), application.getArucoIdsZoom(), arucoParams);

        // if marker detected
        if (application.getArucoCornersZoom().size()>0)
        {
            Log.i("aruco", "Number of detected Markers : "+ application.getArucoCornersZoom().size() ) ;

            // fora each detected marker
            for (int k=0; k<application.getArucoCornersZoom().size(); k++)
            {
                Log.i("aruco", "Read values in marker " + k + " : "+ application.getArucoIdsZoom().get(k,0)[0] ) ;
                // coordinates of four corners
                int x1 = (int) application.getArucoCornersZoom().get(k).get(0,0)[0];
                int y1 = (int) application.getArucoCornersZoom().get(k).get(0,0)[1];
                int x2 = (int) application.getArucoCornersZoom().get(k).get(0,1)[0];
                int y2 = (int) application.getArucoCornersZoom().get(k).get(0,1)[1];
                int x3 = (int) application.getArucoCornersZoom().get(k).get(0,2)[0];
                int y3 = (int) application.getArucoCornersZoom().get(k).get(0,2)[1];
                int x4 = (int) application.getArucoCornersZoom().get(k).get(0,3)[0];
                int y4 = (int) application.getArucoCornersZoom().get(k).get(0,3)[1];
                // draw corners
                Imgproc.circle(frameZoom, new Point(x1, y1),  1, new Scalar(0,255,0)  ,5);
                Imgproc.circle(frameZoom, new Point(x2, y2),  1, new Scalar(255,0,0)  ,5);
                Imgproc.circle(frameZoom, new Point(x3, y3),  1, new Scalar(0,0,255)  ,5);
                Imgproc.circle(frameZoom, new Point(x4, y4),  1, new Scalar(125,0,125)  ,5);

            } // next marker

            // Envoi du broadcast pour notifier les autres applications de la détection de tags
            Intent intent_tag_detected = new Intent("TAG_DETECTED_ZOOM");
            sendBroadcast(intent_tag_detected);

        } // end if marker detected


        /*
         * Partie : Enregistrement de la frame + streaming
         */

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


    /**
     *  Cacher les barres systemUI
     */

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            application.hideSystemUI(this);
        }
    }
}