package com.bfr.main.poctestvisionservice.activities;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bfr.main.poctestvisionservice.R;
import com.bfr.main.poctestvisionservice.models.ObjectExample;
import com.bfr.main.visionservice.IVisionService;
import com.newtronlabs.sharedmemory.IRemoteSharedMemory;
import com.newtronlabs.sharedmemory.RemoteMemoryAdapter;

import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * La classe MainActivity est l'activité principale de ce POC.
 * Tout d'abord, elle permet de gérer les permissions nécessaires pour l'accès au stockage local afin
 * de récupérer les fichiers contenant les byte[] des images fournies par le service VisionService.
 * Les permissions demandées :
 *      + Le stockage [WRITE_EXTERNAL_STORAGE , READ_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE]
 *
 * Ensuite, elle présente 7 boutons :
 *
 *  GrandAngle :
 *      + startStream : pour démarrer le streaming des images grand-angle capturées par le service VisionService. [utilisation de mémoire partagée + AIDL]
 *      + stopStream : pour arrêter le streaming des images grand-angle capturées par le service VisionService. [utilisation de mémoire partagée + AIDL]
 *      + getImage : pour récupérer l'image grand-angle capturée par le service VisionService. [utilisation du stockage locale + AIDL]
 *  ---------------------------------------------------------------------------------------------------------------------------------------------------------------
 *  Zoom :
 *      + startStream : pour démarrer le streaming des images zoom capturées par le service VisionService. [utilisation de mémoire partagée + AIDL]
 *      + stopStream : pour arrêter le streaming des images zoom capturées par le service VisionService. [utilisation de mémoire partagée + AIDL]
 *      + getImage : pour récupérer l'image zoom capturée par le service VisionService. [utilisation du stockage locale + AIDL]
 * ---------------------------------------------------------------------------------------------------------------------------------------------------------------
 *      + getObjet : pour récupérer l'objet exemple parcelable fourni par le service VisionService. [utilisation de mémoire partagée + stockage locale + AIDL]
 *
 */
public class MainActivity extends Activity {

    private static final String TAG = "POC_SERVICE_VISION";

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    //Streaming (Flux d'images)
    //grand-angle
    private ImageView imageViewGetStreamGrandAngle;
    private TextView errorTextGetStreamGrandAngle;
    private IRemoteSharedMemory remoteMemoryOfStreamFramesGrandAngle;
    //zoom
    private ImageView imageViewGetStreamZoom;
    private TextView errorTextGetStreamZoom;
    private IRemoteSharedMemory remoteMemoryOfStreamFramesZoom;

    //GetImage
    //grand-angle
    private ImageView imageViewGetImageGrandAngle;
    private TextView errorTextGetImageGrandAngle;
    private ProgressBar progressBar_getImageGrandAngle;
    private Bitmap bitmapGetImageGrandAngle;
    //zoom
    private ImageView imageViewGetImageZoom;
    private TextView errorTextGetImageZoom;
    private ProgressBar progressBar_getImageZoom;
    private Bitmap bitmapGetImageZoom;

    //GetObject
    private ObjectExample objectExample;
    private TextView arrayInteger;
    private TextView arrayString;
    private TextView errorTextGetObjet;
    private ProgressBar progressBar_getObjet;
    private ImageView imageViewGetObjet_sharedMemory;
    private Bitmap bitmapSharedMemoryGetObjet;
    private IRemoteSharedMemory remoteMemoryOfCvResultingFrame;
    private ImageView imageViewGetObjet_storage;
    private Bitmap bitmapStorageGetObjet;

    /**
     *  Callback de connexion au service externe VisionService
     */
    private IVisionService mVisionService;
    private ServiceConnection mConnectionVisionService = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG,"POC connecté au service VisionService");
            mVisionService = IVisionService.Stub.asInterface(service);
        }
        public void onServiceDisconnected(ComponentName className) {
            Log.e(TAG,"POC déconnecté du service VisionService");
            mVisionService = null;
        }
    };


    /**
     *  Receiver de notification de stream grand-angle : nouvelle frame grand-angle écrite sur la mémoire partagée.
     *   - Lecture du byte[] et affichage de la frame
     */
    private BroadcastReceiver receiverNewFrameGrandAngle = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                //lecture
                final byte[] frameGrandAngleBytes = new byte[remoteMemoryOfStreamFramesGrandAngle.getSize()];
                remoteMemoryOfStreamFramesGrandAngle.readBytes(frameGrandAngleBytes, 0, 0, frameGrandAngleBytes.length);

                //affichage
                Bitmap bitmapFrameGrandAngleStream = BitmapFactory.decodeByteArray(frameGrandAngleBytes, 0, frameGrandAngleBytes.length);
                imageViewGetStreamGrandAngle.setImageBitmap(bitmapFrameGrandAngleStream);
            }
            catch (Exception e) {
                Log.e(TAG, "Erreur lors de la lecture depuis la mémoire partagée ("+remoteMemoryOfStreamFramesGrandAngle.getRegionName()+"): "+e);
                errorTextGetStreamGrandAngle.setText("Une erreur est survenue !");
            }
        }
    };

    /**
     *  Receiver de notification de stream zoom : nouvelle frame zoom écrite sur la mémoire partagée.
     *   - Lecture du byte[] et affichage de la frame
     */
    private BroadcastReceiver receiverNewFrameZoom = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                //lecture
                final byte[] frameZoomBytes = new byte[remoteMemoryOfStreamFramesZoom.getSize()];
                remoteMemoryOfStreamFramesZoom.readBytes(frameZoomBytes, 0, 0, frameZoomBytes.length);

                //affichage
                Bitmap bitmapFrameZoomStream = BitmapFactory.decodeByteArray(frameZoomBytes, 0, frameZoomBytes.length);
                imageViewGetStreamZoom.setImageBitmap(bitmapFrameZoomStream);
            }
            catch (Exception e) {
                Log.e(TAG, "Erreur lors de la lecture depuis la mémoire partagée ("+remoteMemoryOfStreamFramesZoom.getRegionName()+"): "+e);
                errorTextGetStreamZoom.setText("Une erreur est survenue !");
            }
        }
    };

    /**
     * Vérification des permissions puis lancement des traitements
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)
        ){
            start();
        }
    }

    /**
     * La fonction start() permet de :
     *   - initialiser les views
     *   - lancer la connexion au service externe VisionService
     *   - initialiser les objets de mémoire partagée de VisionService
     */
    private void start(){

        /*
         * Initialisations
         */
        arrayInteger = (TextView) findViewById(R.id.arrayInteger);
        arrayString = (TextView) findViewById(R.id.arrayString);
        imageViewGetImageGrandAngle = (ImageView) findViewById(R.id.imageViewGetImageGrandAngle);
        imageViewGetImageZoom = (ImageView) findViewById(R.id.imageViewGetImageZoom);
        imageViewGetObjet_sharedMemory = (ImageView) findViewById(R.id.imageViewGetObjet_sharedMemory);
        imageViewGetObjet_storage = (ImageView) findViewById(R.id.imageViewGetObjet_storage);
        imageViewGetStreamGrandAngle = (ImageView) findViewById(R.id.imageViewGetStreamGrandAngle);
        imageViewGetStreamZoom = (ImageView) findViewById(R.id.imageViewGetStreamZoom);
        errorTextGetImageGrandAngle = (TextView) findViewById(R.id.errorTextGetImageGrandAngle);
        errorTextGetImageZoom = (TextView) findViewById(R.id.errorTextGetImageZoom);
        errorTextGetObjet = (TextView) findViewById(R.id.errorTextGetObjet);
        errorTextGetStreamGrandAngle = (TextView) findViewById(R.id.errorTextGetStreamGrandAngle);
        errorTextGetStreamZoom = (TextView) findViewById(R.id.errorTextGetStreamZoom);
        progressBar_getImageGrandAngle = (ProgressBar) findViewById(R.id.progressBar_getImageGrandAngle);
        progressBar_getImageZoom = (ProgressBar) findViewById(R.id.progressBar_getImageZoom);
        progressBar_getObjet = (ProgressBar) findViewById(R.id.progressBar_getObjet);

        /*
         *  Connexion au service externe VisionService
         */
        Intent intent = new Intent();
        intent.setAction("services.visionService");
        intent.setPackage("com.bfr.main.visionservice");
        bindService(intent, mConnectionVisionService, Context.BIND_AUTO_CREATE);

        /*
         *   Récupération des objets de mémoire partagée de VisionService
         */
        new Thread(new Runnable() {
            public void run(){
                String visionServiceAppId = "com.bfr.main.visionservice";

                String regionName_stream_frames_grandAngle = getString(R.string.name_region_shared_memory_stream_frames_grand_angle);
                remoteMemoryOfStreamFramesGrandAngle = RemoteMemoryAdapter.getDefaultAdapter().getSharedMemory(MainActivity.this, visionServiceAppId, regionName_stream_frames_grandAngle);

                String regionName_stream_frames_zoom = getString(R.string.name_region_shared_memory_stream_frames_zoom);
                remoteMemoryOfStreamFramesZoom = RemoteMemoryAdapter.getDefaultAdapter().getSharedMemory(MainActivity.this, visionServiceAppId, regionName_stream_frames_zoom);

                String regionName_cv_resulting_frame = getString(R.string.name_region_shared_memory_cv_resulting_frame);
                remoteMemoryOfCvResultingFrame = RemoteMemoryAdapter.getDefaultAdapter().getSharedMemory(MainActivity.this, visionServiceAppId, regionName_cv_resulting_frame);
            }
        }).start();

    }

    /**
     * Déconnexion du service externe VisionService et désinscription du broadcastReceiver
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            if (mConnectionVisionService != null) {
                unbindService(mConnectionVisionService);
            }
            unregisterReceiver(receiverNewFrameGrandAngle);
            unregisterReceiver(receiverNewFrameZoom);
        }
        catch (Exception ignored){}
    }

    /**
     * La fonction BtnStartStreamGrandAngle() est executée suite au clic sur le bouton 'startStream' de la rubrique Grand Angle.
     * Elle fait appel à la méthode [startFrameStream("grand-angle")] du service externe pour que le service lance
     * l'écriture des byte[] des frames grand-angle sur la mémoire partagée.
     * Elle permet aussi de s'enregistrer au broadcast qui notifie la présence d'une nouvelle frame grand-angle
     */
    public void BtnStartStreamGrandAngle(final View view) {

        /*
         * initialisations
         */
        imageViewGetStreamGrandAngle.setImageBitmap(null);
        errorTextGetStreamGrandAngle.setText("");

        /*
         * Enregistrement au broadcast qui notifie la présence d'une nouvelle frame grand-angle
         */
        registerReceiver(receiverNewFrameGrandAngle, new IntentFilter("NEW_FRAME_OPENCV_IS_WRITTEN_GRAND_ANGLE"));

        /*
         * Appel de la méthode du service externe [startFrameStream("grand-angle")] pour que le service lance
         * l'écriture des byte[] des frames grand-angle sur la mémoire partagée.
         */
        if (mVisionService != null){
            try {
                mVisionService.startFrameStream("grand-angle");
            } catch (RemoteException e) {
                Log.e(TAG,"Erreur pendant l'appel de la fonction startFrameStream(\"grand-angle\") du service Vision : "+e);
                errorTextGetStreamGrandAngle.setText("Une erreur est survenue !");
            }
        }
        else {
            Log.e(TAG,"mVisionService is null");
            errorTextGetStreamGrandAngle.setText("non connecté au service Vision");
        }
    }


    /**
     * La fonction BtnStartStreamZoom() est executée suite au clic sur le bouton 'startStream' de la rubrique Zoom.
     * Elle fait appel à la méthode [startFrameStream("zoom")] du service externe pour que le service lance
     * l'écriture des byte[] des frames zoom sur la mémoire partagée.
     * Elle permet aussi de s'enregistrer au broadcast qui notifie la présence d'une nouvelle frame zoom
     */
    public void BtnStartStreamZoom(final View view) {

        /*
         * initialisations
         */
        imageViewGetStreamZoom.setImageBitmap(null);
        errorTextGetStreamZoom.setText("");

        /*
         * Enregistrement au broadcast qui notifie la présence d'une nouvelle frame zoom
         */
        registerReceiver(receiverNewFrameZoom, new IntentFilter("NEW_FRAME_OPENCV_IS_WRITTEN_ZOOM"));

        /*
         * Appel de la méthode du service externe [startFrameStream("zoom")] pour que le service lance
         * l'écriture des byte[] des frames zoom sur la mémoire partagée.
         */
        if (mVisionService != null){
            try {
                mVisionService.startFrameStream("zoom");
            } catch (RemoteException e) {
                Log.e(TAG,"Erreur pendant l'appel de la fonction startFrameStream(\"zoom\") du service Vision : "+e);
                errorTextGetStreamZoom.setText("Une erreur est survenue !");
            }
        }
        else {
            Log.e(TAG,"mVisionService is null");
            errorTextGetStreamZoom.setText("non connecté au service Vision");
        }
    }



    /**
     * La fonction BtnStopStreamGrandAngle() est executée suite au clic sur le bouton 'stopStream' de la rubrique Grand Angle.
     * Elle fait appel à la méthode [stopFrameStream("grand-angle")] du service externe pour que le service arrête
     * l'écriture des byte[] des frames grand-angle sur la mémoire partagée.
     * Elle permet aussi de se désinscrire du broadcast qui notifie la présence d'une nouvelle frame grand-angle
     */
    public void BtnStopStreamGrandAngle(View view) {

        /*
         * Désinscription du broadcastReceiver
         */
        try{
            unregisterReceiver(receiverNewFrameGrandAngle);
        }
        catch (Exception ignored){}

        /*
         * initialisations
         */
        imageViewGetStreamGrandAngle.setImageBitmap(null);
        errorTextGetStreamGrandAngle.setText("");

        /*
         * Appel de la méthode du service externe [stopFrameStream("grand-angle")] pour que le service arrête
         * l'écriture des byte[] des frames grand-angle sur la mémoire partagée.
         */
        if (mVisionService != null){
            try {
                mVisionService.stopFrameStream("grand-angle");
            } catch (RemoteException e) {
                Log.e(TAG,"Erreur pendant l'appel de la fonction stopFrameStream(\"grand-angle\") du service Vision : "+e);
                errorTextGetStreamGrandAngle.setText("Une erreur est survenue !");
            }
        }
        else {
            Log.e(TAG,"mVisionService is null");
            errorTextGetStreamGrandAngle.setText("non connecté au service Vision");
        }
    }


    /**
     * La fonction BtnStopStreamZoom() est executée suite au clic sur le bouton 'stopStream' de la rubrique Zoom.
     * Elle fait appel à la méthode [stopFrameStream("zoom")] du service externe pour que le service arrête
     * l'écriture des byte[] des frames zoom sur la mémoire partagée.
     * Elle permet aussi de se désinscrire du broadcast qui notifie la présence d'une nouvelle frame zoom
     */
    public void BtnStopStreamZoom(View view) {

        /*
         * Désinscription du broadcastReceiver
         */
        try{
            unregisterReceiver(receiverNewFrameZoom);
        }
        catch (Exception ignored){}

        /*
         * initialisations
         */
        imageViewGetStreamZoom.setImageBitmap(null);
        errorTextGetStreamZoom.setText("");

        /*
         * Appel de la méthode du service externe [stopFrameStream("zoom")] pour que le service arrête
         * l'écriture des byte[] des frames zoom sur la mémoire partagée.
         */
        if (mVisionService != null){
            try {
                mVisionService.stopFrameStream("zoom");
            } catch (RemoteException e) {
                Log.e(TAG,"Erreur pendant l'appel de la fonction stopFrameStream(\"zoom\") du service Vision : "+e);
                errorTextGetStreamZoom.setText("Une erreur est survenue !");
            }
        }
        else {
            Log.e(TAG,"mVisionService is null");
            errorTextGetStreamZoom.setText("non connecté au service Vision");
        }
    }


    /**
     * La fonction BtnGetImageGrandAngle() est executée suite au clic sur le bouton 'getImage' de la rubrique Grand Angle.
     * Elle fait appel à la méthode [getImageByteFilePath("grand-angle")] du service externe pour récupérer
     * le chemin vers l'image grand-angle capturée et ensuite l'afficher.
     * NB: le traitement est exécuté sous un Thread pour éviter les ANR.
     *
     */
    public void BtnGetImageGrandAngle(final View view) {

        /*
         * initialisations
         */
        imageViewGetImageGrandAngle.setImageBitmap(null);
        errorTextGetImageGrandAngle.setText("");
        bitmapGetImageGrandAngle = null;

        /*
         * Appel de la méthode du service externe [getImageByteFilePath("grand-angle")] pour récupérer
         * le chemin vers l'image grand-angle capturée et ensuite l'afficher.
         */
        if (mVisionService != null){

            //bouton invisible + spinner visible
            view.setVisibility(View.GONE);
            progressBar_getImageGrandAngle.setVisibility(View.VISIBLE);

            new Thread(new Runnable() {
                public void run(){
                    try {

                        //récupération du chemin
                        final String imageByteFilePath = mVisionService.getImageByteFilePath("grand-angle");

                        //récupération du byte[]
                        if(!imageByteFilePath.equals("ERROR")){
                            RandomAccessFile f = new RandomAccessFile(imageByteFilePath, "r");
                            try {
                                int length = (int) f.length();
                                byte[] imageByte = new byte[length];
                                f.readFully(imageByte);
                                bitmapGetImageGrandAngle = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length);
                            }
                            finally {
                                f.close();
                            }
                        }

                        //affichage
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(bitmapGetImageGrandAngle != null) imageViewGetImageGrandAngle.setImageBitmap(bitmapGetImageGrandAngle);
                                else {
                                    errorTextGetImageGrandAngle.setText("Image introuvable !");
                                    Log.e(TAG,"Image grand-angle introuvable sur le stockage locale ["+imageByteFilePath+"]");
                                }
                                //bouton visible + spinner invisible
                                view.setVisibility(View.VISIBLE);
                                progressBar_getImageGrandAngle.setVisibility(View.GONE);
                            }
                        });

                    } catch (Exception e) {
                        Log.e(TAG,"Erreur pendant la récupération de l'image grand-angle capturée : " + e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                errorTextGetImageGrandAngle.setText("Une erreur est survenue !");
                                //bouton visible + spinner invisible
                                view.setVisibility(View.VISIBLE);
                                progressBar_getImageGrandAngle.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            }).start();
        }
        else {
            Log.e(TAG,"mVisionService is null");
            errorTextGetImageGrandAngle.setText("non connecté au service Vision");
        }
    }


    /**
     * La fonction BtnGetImageZoom() est executée suite au clic sur le bouton 'getImage' de la rubrique Zoom.
     * Elle fait appel à la méthode [getImageByteFilePath("zoom")] du service externe pour récupérer
     * le chemin vers l'image zoom capturée et ensuite l'afficher.
     * NB: le traitement est exécuté sous un Thread pour éviter les ANR.
     *
     */
    public void BtnGetImageZoom(final View view) {

        /*
         * initialisations
         */
        imageViewGetImageZoom.setImageBitmap(null);
        errorTextGetImageZoom.setText("");
        bitmapGetImageZoom = null;

        /*
         * Appel de la méthode du service externe [getImageByteFilePath("zoom")] pour récupérer
         * le chemin vers l'image zoom capturée et ensuite l'afficher.
         */
        if (mVisionService != null){

            //bouton invisible + spinner visible
            view.setVisibility(View.GONE);
            progressBar_getImageZoom.setVisibility(View.VISIBLE);

            new Thread(new Runnable() {
                public void run(){
                    try {

                        //récupération du chemin
                        final String imageByteFilePath = mVisionService.getImageByteFilePath("zoom");

                        //récupération du byte[]
                        if(!imageByteFilePath.equals("ERROR")){
                            RandomAccessFile f = new RandomAccessFile(imageByteFilePath, "r");
                            try {
                                int length = (int) f.length();
                                byte[] imageByte = new byte[length];
                                f.readFully(imageByte);
                                bitmapGetImageZoom = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length);
                            }
                            finally {
                                f.close();
                            }
                        }

                        //affichage
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(bitmapGetImageZoom != null) imageViewGetImageZoom.setImageBitmap(bitmapGetImageZoom);
                                else {
                                    errorTextGetImageZoom.setText("Image introuvable !");
                                    Log.e(TAG,"Image zoom introuvable sur le stockage locale ["+imageByteFilePath+"]");
                                }
                                //bouton visible + spinner invisible
                                view.setVisibility(View.VISIBLE);
                                progressBar_getImageZoom.setVisibility(View.GONE);
                            }
                        });

                    } catch (Exception e) {
                        Log.e(TAG,"Erreur pendant la récupération de l'image zoom capturée : " + e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                errorTextGetImageZoom.setText("Une erreur est survenue !");
                                //bouton visible + spinner invisible
                                view.setVisibility(View.VISIBLE);
                                progressBar_getImageZoom.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            }).start();
        }
        else {
            Log.e(TAG,"mVisionService is null");
            errorTextGetImageZoom.setText("non connecté au service Vision");
        }
    }

    /**
     * La fonction BtnGetObjet() est executée suite au clic sur le bouton 'getObjet'.
     * Elle fait appel à la méthode [getObjectExample()] du service externe pour récupérer l'objet
     * et afficher son contenu
     * NB: le traitement est exécuté sous un Thread pour éviter les ANR.
     *
     */
    public void BtnGetObjet(final View view) {

        /*
         * initialisations
         */
        arrayInteger.setText("arrayInteger :");
        arrayString.setText("arrayString :");
        imageViewGetObjet_sharedMemory.setImageBitmap(null);
        imageViewGetObjet_storage.setImageBitmap(null);
        errorTextGetObjet.setText("");
        bitmapSharedMemoryGetObjet = null;
        bitmapStorageGetObjet = null;

        /*
         * Appel de la méthode du service externe [getObjectExample()] pour récupérer l'objet
         * et afficher son contenu
         */
        if (mVisionService != null){
            //bouton invisible + spinner visible
            view.setVisibility(View.GONE);
            progressBar_getObjet.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                public void run(){
                    try {

                        //récupérer l'objet
                        objectExample = mVisionService.getObjectExample();

                        //lecture du byte[] de l'image en mémoire partagée
                        if(objectExample.isFrameWrittenInSharedMemory()){
                            byte [] imageBytesSharedMemory = new byte[remoteMemoryOfCvResultingFrame.getSize()];
                            remoteMemoryOfCvResultingFrame.readBytes(imageBytesSharedMemory, 0, 0, imageBytesSharedMemory.length);
                            bitmapSharedMemoryGetObjet = BitmapFactory.decodeByteArray(imageBytesSharedMemory, 0, imageBytesSharedMemory.length);
                        }

                        //récupérer le byte[] de l'image en stockage locale
                        final String imageFilePath = objectExample.getImageBytesFilePath();
                        if(!imageFilePath.equals("")){
                            RandomAccessFile f = new RandomAccessFile(imageFilePath, "r");
                            try {
                                int length = (int) f.length();
                                byte [] imageBytesStorage = new byte[length];
                                f.readFully(imageBytesStorage);
                                bitmapStorageGetObjet = BitmapFactory.decodeByteArray(imageBytesStorage, 0, imageBytesStorage.length);
                            }
                            finally {
                                f.close();
                            }
                        }

                        //Affichage
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                //Array d'integer
                                arrayInteger.setText(String.format("arrayInteger : %s", Arrays.toString(objectExample.getArrayInteger())));

                                //Array de string
                                arrayString.setText(String.format("arrayString : %s", Arrays.toString(objectExample.getArrayString())));

                                //Image récupérée depuis la mémoire partagée
                                if(bitmapSharedMemoryGetObjet != null) imageViewGetObjet_sharedMemory.setImageBitmap(bitmapSharedMemoryGetObjet);
                                else {
                                    errorTextGetObjet.setText("Image introuvable !");
                                    Log.e(TAG,"Image introuvable sur la mémoire partagée");
                                }

                                //Image récupérée depuis le stockage locale
                                if(bitmapStorageGetObjet != null) imageViewGetObjet_storage.setImageBitmap(bitmapStorageGetObjet);
                                else {
                                    errorTextGetObjet.setText("Image introuvable !");
                                    Log.e(TAG,"Image introuvable sur le stockage locale ["+imageFilePath+"]");
                                }

                                //bouton visible + spinner invisible
                                view.setVisibility(View.VISIBLE);
                                progressBar_getObjet.setVisibility(View.GONE);
                            }
                        });
                    }
                    catch (Exception e) {
                        Log.e(TAG,"Erreur pendant la récupération de l'objet et l'affichage de son contenu : " + e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                errorTextGetObjet.setText("Une erreur est survenue !");
                                //bouton visible + spinner invisible
                                view.setVisibility(View.VISIBLE);
                                progressBar_getObjet.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            }).start();
        }
        else {
            Log.e(TAG,"mVisionService is null");
            errorTextGetObjet.setText("non connecté au service Vision");
        }
    }


    /**
     * Méthodes de gestion de permissions
     */

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    private boolean checkPermission(@NonNull int[] grantResults){
        return grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQ_ID && checkPermission(grantResults)) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            getApplicationContext(),
                            "Need permissions "
                                    + Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    + "/"
                                    + Manifest.permission.READ_EXTERNAL_STORAGE,
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
            finish();
            return;
        }else {
            start();
        }
    }

}
