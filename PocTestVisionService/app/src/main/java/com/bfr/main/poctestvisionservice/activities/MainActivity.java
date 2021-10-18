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
 * Ensuite, elle présente quatre boutons :
 *      + startStream : pour démarrer le streaming des images capturées par le service VisionService. [utilisation de mémoire partagée + AIDL]
 *      + stopStream : pour arrêter le streaming des images capturées par le service VisionService. [utilisation de mémoire partagée + AIDL]
 *      + getImage : pour récupérer l'image capturée par le service VisionService. [utilisation du stockage locale + AIDL]
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
    private ImageView imageViewGetStream;
    private TextView errorTextGetStream;
    private IRemoteSharedMemory remoteMemoryOfStreamFrames;

    //GetImage
    private ImageView imageViewGetImage;
    private TextView errorTextGetImage;
    private ProgressBar progressBar_getImage;
    private Bitmap bitmapGetImage;

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
     *  Receiver de notification de stream : nouvelle frame écrite sur la mémoire partagée.
     *   - Lecture du byte[] et affichage de la frame
     */
    private BroadcastReceiver receiverNewFrame = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                //lecture
                final byte[] frameBytes = new byte[remoteMemoryOfStreamFrames.getSize()];
                remoteMemoryOfStreamFrames.readBytes(frameBytes, 0, 0, frameBytes.length);

                //affichage
                Bitmap bitmapFrameStream = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.length);
                imageViewGetStream.setImageBitmap(bitmapFrameStream);
            }
            catch (Exception e) {
                Log.e(TAG, "Erreur lors de la lecture depuis la mémoire partagée : "+e);
                errorTextGetStream.setText("Une erreur est survenue !");
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
        imageViewGetImage = (ImageView) findViewById(R.id.imageViewGetImage);
        imageViewGetObjet_sharedMemory = (ImageView) findViewById(R.id.imageViewGetObjet_sharedMemory);
        imageViewGetObjet_storage = (ImageView) findViewById(R.id.imageViewGetObjet_storage);
        imageViewGetStream = (ImageView) findViewById(R.id.imageViewGetStream);
        errorTextGetImage = (TextView) findViewById(R.id.errorTextGetImage);
        errorTextGetObjet = (TextView) findViewById(R.id.errorTextGetObjet);
        errorTextGetStream = (TextView) findViewById(R.id.errorTextGetStream);
        progressBar_getImage = (ProgressBar) findViewById(R.id.progressBar_getImage);
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
                String regionName_stream_frames = getString(R.string.name_region_shared_memory_stream_frames);
                remoteMemoryOfStreamFrames = RemoteMemoryAdapter.getDefaultAdapter().getSharedMemory(MainActivity.this, visionServiceAppId, regionName_stream_frames);

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
            unregisterReceiver(receiverNewFrame);
        }
        catch (Exception ignored){}
    }

    /**
     * La fonction BtnStartStream() est executée suite au clic sur le bouton 'startStream'.
     * Elle fait appel à la méthode [startFrameStream()] du service externe pour que le service lance
     * l'écriture des byte[] des frames sur la mémoire partagée.
     * Elle permet aussi de s'enregistrer au broadcast qui notifie la présence d'une nouvelle frame
     */
    public void BtnStartStream(final View view) {

        /*
         * initialisations
         */
        imageViewGetStream.setImageBitmap(null);
        errorTextGetStream.setText("");

        /*
         * Enregistrement au broadcast qui notifie la présence d'une nouvelle frame
         */
        registerReceiver(receiverNewFrame, new IntentFilter("NEW_FRAME_OPENCV_IS_WRITTEN"));

        /*
         * Appel de la méthode du service externe [startFrameStream()] pour que le service lance
         * l'écriture des byte[] des frames sur la mémoire partagée.
         */
        if (mVisionService != null){
            try {
                mVisionService.startFrameStream();
            } catch (RemoteException e) {
                Log.e(TAG,"Erreur pendant l'appel de la fonction startFrameStream() du service Vision : "+e);
                errorTextGetStream.setText("Une erreur est survenue !");
            }
        }
        else {
            Log.e(TAG,"mVisionService is null");
            errorTextGetStream.setText("non connecté au service Vision");
        }
    }

    /**
     * La fonction BtnStopStream() est executée suite au clic sur le bouton 'stopStream'.
     * Elle fait appel à la méthode [stopFrameStream()] du service externe pour que le service arrête
     * l'écriture des byte[] des frames sur la mémoire partagée.
     * Elle permet aussi de se désinscrire du broadcast qui notifie la présence d'une nouvelle frame
     */
    public void BtnStopStream(View view) {

        /*
         * Désinscription du broadcastReceiver
         */
        try{
            unregisterReceiver(receiverNewFrame);
        }
        catch (Exception ignored){}

        /*
         * initialisations
         */
        imageViewGetStream.setImageBitmap(null);
        errorTextGetStream.setText("");

        /*
         * Appel de la méthode du service externe [stopFrameStream()] pour que le service arrête
         * l'écriture des byte[] des frames sur la mémoire partagée.
         */
        if (mVisionService != null){
            try {
                mVisionService.stopFrameStream();
            } catch (RemoteException e) {
                Log.e(TAG,"Erreur pendant l'appel de la fonction stopFrameStream() du service Vision : "+e);
                errorTextGetStream.setText("Une erreur est survenue !");
            }
        }
        else {
            Log.e(TAG,"mVisionService is null");
            errorTextGetStream.setText("non connecté au service Vision");
        }
    }

    /**
     * La fonction BtnGetImage() est executée suite au clic sur le bouton 'getImage'.
     * Elle fait appel à la méthode [getImageByteFilePath()] du service externe pour récupérer
     * le chemin vers l'image capturée et ensuite l'afficher.
     * NB: le traitement est exécuté sous un Thread pour éviter les ANR.
     *
     */
    public void BtnGetImage(final View view) {

        /*
         * initialisations
         */
        imageViewGetImage.setImageBitmap(null);
        errorTextGetImage.setText("");
        bitmapGetImage = null;

        /*
         * Appel de la méthode du service externe [getImageByteFilePath()] pour récupérer
         * le chemin vers l'image capturée et ensuite l'afficher.
         */
        if (mVisionService != null){

            //bouton invisible + spinner visible
            view.setVisibility(View.GONE);
            progressBar_getImage.setVisibility(View.VISIBLE);

            new Thread(new Runnable() {
                public void run(){
                    try {

                        //récupération du chemin
                        final String imageByteFilePath = mVisionService.getImageByteFilePath();

                        //récupération du byte[]
                        if(!imageByteFilePath.equals("ERROR")){
                            RandomAccessFile f = new RandomAccessFile(imageByteFilePath, "r");
                            try {
                                int length = (int) f.length();
                                byte[] imageByte = new byte[length];
                                f.readFully(imageByte);
                                bitmapGetImage = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length);
                            }
                            finally {
                                f.close();
                            }
                        }

                        //affichage
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(bitmapGetImage != null) imageViewGetImage.setImageBitmap(bitmapGetImage);
                                else {
                                    errorTextGetImage.setText("Image introuvable !");
                                    Log.e(TAG,"Image introuvable sur le stockage locale ["+imageByteFilePath+"]");
                                }
                                //bouton visible + spinner invisible
                                view.setVisibility(View.VISIBLE);
                                progressBar_getImage.setVisibility(View.GONE);
                            }
                        });

                    } catch (Exception e) {
                        Log.e(TAG,"Erreur pendant la récupération de l'image capturée : " + e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                errorTextGetImage.setText("Une erreur est survenue !");
                                //bouton visible + spinner invisible
                                view.setVisibility(View.VISIBLE);
                                progressBar_getImage.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            }).start();
        }
        else {
            Log.e(TAG,"mVisionService is null");
            errorTextGetImage.setText("non connecté au service Vision");
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
