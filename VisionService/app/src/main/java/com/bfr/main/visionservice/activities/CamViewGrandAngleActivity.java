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
 * La classe CamViewGrandAngleActivity représente l'activité de camera "grand-angle" fournie par openCV via CameraActivity.
 * Nous utilisons cette activité pour récupérer la frame openCV "grand-angle" + infos des tags détectés
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
                    String versionRobot = getString(R.string.robot_version);
                    if(versionRobot.equals("4.2")) mOpenCvCameraViewGrandAngle.setCameraIndex(1); //pour le robot 4.2 la caméra grand-angle correspond au 1
                    else mOpenCvCameraViewGrandAngle.setCameraIndex(0);
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

        /*
         * Partie : Détection de tag
         */

        application.setArucoIdsGrandAngle(new Mat());
        application.setArucoCornersGrandAngle(new ArrayList<Mat>());

        //convert
        Imgproc.cvtColor(frameGrandAngle, frameGrandAngle, Imgproc.COLOR_RGBA2RGB);

        // Definition of dictionary and params
        Dictionary arucoDict = Aruco.getPredefinedDictionary(Aruco.DICT_APRILTAG_36h11);
        DetectorParameters arucoParams = DetectorParameters.create();

        // Detect Marker
        Aruco.detectMarkers(frameGrandAngle, arucoDict, application.getArucoCornersGrandAngle(), application.getArucoIdsGrandAngle(), arucoParams);

        // if marker detected
        if (application.getArucoCornersGrandAngle().size()>0)
        {
            Log.i("aruco", "Number of detected Markers : "+ application.getArucoCornersGrandAngle().size() ) ;

            // fora each detected marker
            for (int k=0; k<application.getArucoCornersGrandAngle().size(); k++)
            {
                Log.i("aruco", "Read values in marker " + k + " : "+ application.getArucoIdsGrandAngle().get(k,0)[0] ) ;
                // coordinates of four corners
                int x1 = (int) application.getArucoCornersGrandAngle().get(k).get(0,0)[0];
                int y1 = (int) application.getArucoCornersGrandAngle().get(k).get(0,0)[1];
                int x2 = (int) application.getArucoCornersGrandAngle().get(k).get(0,1)[0];
                int y2 = (int) application.getArucoCornersGrandAngle().get(k).get(0,1)[1];
                int x3 = (int) application.getArucoCornersGrandAngle().get(k).get(0,2)[0];
                int y3 = (int) application.getArucoCornersGrandAngle().get(k).get(0,2)[1];
                int x4 = (int) application.getArucoCornersGrandAngle().get(k).get(0,3)[0];
                int y4 = (int) application.getArucoCornersGrandAngle().get(k).get(0,3)[1];
                // draw corners
                Imgproc.circle(frameGrandAngle, new Point(x1, y1),  1, new Scalar(0,255,0)  ,5);
                Imgproc.circle(frameGrandAngle, new Point(x2, y2),  1, new Scalar(255,0,0)  ,5);
                Imgproc.circle(frameGrandAngle, new Point(x3, y3),  1, new Scalar(0,0,255)  ,5);
                Imgproc.circle(frameGrandAngle, new Point(x4, y4),  1, new Scalar(125,0,125)  ,5);

            } // next marker

            // Envoi du broadcast pour notifier les autres applications de la détection de tags
            Intent intent_tag_detected = new Intent("TAG_DETECTED_GRAND_ANGLE");
            sendBroadcast(intent_tag_detected);

        } // end if marker detected


        /*
         * Partie : Enregistrement de la frame + streaming
         */

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