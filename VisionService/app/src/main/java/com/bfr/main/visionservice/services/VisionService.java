package com.bfr.main.visionservice.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.bfr.main.visionservice.IVisionService;
import com.bfr.main.visionservice.R;
import com.bfr.main.visionservice.activities.CamViewGrandAngleActivity;
import com.bfr.main.visionservice.activities.CamViewZoomActivity;
import com.bfr.main.visionservice.application.VisionServiceApplication;
import com.bfr.main.visionservice.models.ObjectExample;
import com.bfr.main.visionservice.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 *  VisionService est le service objet de la demande VISION SERVICE.
 *  Il fournit les fonctions nécessaires pour :
 *      - Démarrer / Arrêter le streaming des images capturées depuis la caméra grand angle ou Zoom de Buddy (via OpenCV).
 *      - Récupérer un objet Parcelable qui va contenir le résultat d'algorithmes de CV.
 *      - Capturer une image et récupérer le chemin vers le fichier contenant son byte[].
 *      - Récupérer les informations des tags détectés depuis la caméra Grand-Angle ou Zoom.
 *
 *  Les autres applications peuvent se connecter à ce service par le bias d'AIDL pour appeler ces fonctions.
 */
public class VisionService extends Service {

    private static final String TAG = "SERVICE_VISION";

    private VisionServiceApplication application;

    private final IVisionService.Stub remoteBinder = new IVisionService.Stub(){

        /**
         * La fonction startFrameStream() permet de lancer l'écriture des frames sur la mémoire partagée
         * via l'utilisation de l'activity CameraActivity de OpenCV.
         *
         * @param camera : Camera grand angle ["grand-angle"] ou Zoom ["zoom"]
         * @throws RemoteException
         */
        @Override
        public void startFrameStream(String camera) throws RemoteException {

            if(camera.equals("grand-angle")){
                /*
                 * Ouverture de l'activity de camera Grand-Angle pour démarrer le stream Grand-Angle
                 */
                Intent intent = new Intent(VisionService.this, CamViewGrandAngleActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                application.notifyObservers("startStreamGrandAngle");
            }

            else if(camera.equals("zoom")) {
                /*
                 * Ouverture de l'activity de camera Zoom pour démarrer le stream Zoom
                 */
                Intent intent = new Intent(VisionService.this, CamViewZoomActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                application.notifyObservers("startStreamZoom");
            }
        }


        /**
         * La fonction stopFrameStream() permet d'arrêter l'écriture des frames sur la mémoire partagée
         *
         * @param camera : Camera grand angle ["grand-angle"] ou Zoom ["zoom"]
         * @throws RemoteException
         */
        @Override
        public void stopFrameStream(String camera) throws RemoteException {
            if(camera.equals("grand-angle")) {
                application.notifyObservers("stopStreamGrandAngle");
                application.notifyObservers("finishCamGrandAngle");
            }
            else if(camera.equals("zoom")) {
                application.notifyObservers("stopStreamZoom");
                application.notifyObservers("finishCamZoom");
            }
        }


        /**
         * La fonction getObjectExample() permet de récupérer pour le moment un objet exemple pour tester le transfert d'objet via AIDL.
         * Cet objet comportera par la suite le résultat d'algorithmes de CV.
         *
         * L'objet exemple contient pour le moment :
         *     - un array d'Integer
         *     - un array de string
         *     - un boolean pour indiquer le succés/échec de l'écriture du byte[] d'une image de test sur la mémoire partagée [méthode 1 : utilisation mémoire partagée]
         *     - le chemin vers le fichier contenant le byte[] d'une image de test [méthode 2 : utilisation stockage locale]
         *
         * @return : une instance de ObjectExample
         * @throws RemoteException
         */
        @Override
        public ObjectExample getObjectExample() throws RemoteException {

            /*
             * Préparation de l'objet exemple pour le test
             */

            ObjectExample objectExample = new ObjectExample();

            //array d'Integer
            int[] arrayInteger = {1, 2, 3};
            objectExample.setArrayInteger(arrayInteger);

            //array de string
            String[] arrayString = {"un", "deux", "trois"};
            objectExample.setArrayString(arrayString);

            /*
             * Image [2 méthodes : via la mémoire partagée ou via stockage locale ]
             */

            //Récupération du byte[] de l'image de test [R.drawable.picture_test] :
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.picture_test);
            byte[] bitmapBytes = Utils.getBytesFromBitmap(bitmap);


            /*
             * méthode 1 : Ecriture du byte[] sur la mémoire partagée
             */
            boolean success = application.writeCvResultingFrameInSharedMemory(bitmapBytes);
            objectExample.setFrameIsWrittenInSharedMemory(success);


            /*
             * méthode 2 : Enregistrement du byte[] sur le stockage locale
             */
            File image_bytes_directory = new File(getString(R.string.path_to_storage), getString(R.string.image_bytes_directory));
            if(!image_bytes_directory.exists()){
                image_bytes_directory.mkdir();
            }
            File cv_result_image_bytes_file  = new File(image_bytes_directory.getPath(),  getString(R.string.cv_result_image_bytes_file));
            String cv_result_image_bytes_file_path = getString(R.string.path_to_storage) + getString(R.string.image_bytes_directory) + "/" + getString(R.string.cv_result_image_bytes_file);
            try {
                FileOutputStream fos = new FileOutputStream(cv_result_image_bytes_file);
                fos.write(bitmapBytes);
                fos.close();
                objectExample.setImageBytesFilePath(cv_result_image_bytes_file_path);
            }
            catch (Exception e) {
                Log.e(TAG, "Erreur pendant l'enregistrement du byte[] sur le fichier [ "+cv_result_image_bytes_file_path+" ] : "+e);
                objectExample.setImageBytesFilePath("");
            }

            //Retourner l'objet

            Log.i(TAG,"getObjectExample() : \n"
                    + "arrayInteger : "+ Arrays.toString(arrayInteger)
                    + "\n"
                    + "arrayString : "+ Arrays.toString(arrayString)
                    + "\n"
                    +"Ecriture sur la mémoire partagée [méthode 1] : "+ success
                    + "\n"
                    +"chemin vers le byte[] de l'image [méthode 2] : "+ cv_result_image_bytes_file_path
            );

            return objectExample;
        }


        /**
         * La fonction getImageByteFilePath() permet de :
         *    - capturer une image depuis la caméra grand angle ou zoom de Buddy (via la camera d'OpenCV).
         *    - Ensuite, enregistrer son byte[] sur un fichier en stockage local.
         *    - Finalement, retourner le chemin vers ce fichier.
         *
         * @param camera : Camera grand angle ["grand-angle"] ou Zoom ["zoom"]
         * @return : le chemin vers le fichier contenant le byte[] de l'image capturée
         *           ou bien "ERROR" si une erreur est survenue.
         *
         * @throws RemoteException
         */
        @Override
        public String getImageByteFilePath(String camera) throws RemoteException {

            /*
             *  Prise de la photo et récupération du byte[]
             */

            byte[] capturedImageBytes = takePictureUsingCameraOpenCV(camera);

            String fileName = null;
            if(camera.equals("grand-angle")) fileName = getString(R.string.captured_image_grand_angle_bytes_file);
            else if(camera.equals("zoom")) fileName = getString(R.string.captured_image_zoom_bytes_file);


            /*
             * Enregistrement du byte[] sur le fichier : [path_to_storage/image_bytes_directory/fileName]
             *  NB : Nous ne pouvons pas envoyer le byte[] directement par AIDL lorsque sa taille est trés grande
             *  c'est pour ça nous avons opté pour l'enregistrement du byte[] dans un fichier et envoyer juste son path.
             */

            if(capturedImageBytes != null){

                String captured_image_bytes_file_path = getString(R.string.path_to_storage) + getString(R.string.image_bytes_directory) + "/" + fileName;

                File image_bytes_directory = new File(getString(R.string.path_to_storage), getString(R.string.image_bytes_directory));
                if(!image_bytes_directory.exists()){
                    image_bytes_directory.mkdir();
                }
                File captured_image_bytes_file  = new File(image_bytes_directory.getPath(),  fileName);
                try {
                    FileOutputStream fos = new FileOutputStream(captured_image_bytes_file);
                    fos.write(capturedImageBytes);
                    fos.close();
                }
                catch (Exception e) {
                    Log.e(TAG, "Erreur pendant l'enregistrement du byte[] sur le fichier [ "+captured_image_bytes_file_path+" ] : "+e);
                    return "ERROR";
                }

                Log.i(TAG,"Succès d'enregistrement du byte[] sur le fichier : [ "+captured_image_bytes_file_path+" ]");

                return captured_image_bytes_file_path;
            }
            else return "ERROR";

        }

        /**
         * La fonction getTagsInfos() permet de récupérer les informations des tags détectés depuis
         * la caméra Grand-Angle ou Zoom.
         * @param camera : Camera grand angle ["grand-angle"] ou Zoom ["zoom"]
         * @return : Json sous format de String contenant les informations de tags.
         * @throws RemoteException
         */
        @Override
        public String getTagsInfos(String camera) throws RemoteException {

            List<Mat> arucoCorners;
            Mat arucoIds;

            /*
             * Récupération des informations arucoCorners et arucoIds pour la construction du JSON
             */
            if(camera.equals("grand-angle") && application.getArucoCornersGrandAngle().size()>0) {
                arucoCorners = application.getArucoCornersGrandAngle();
                arucoIds = application.getArucoIdsGrandAngle();
            }
            else if(camera.equals("zoom") && application.getArucoCornersZoom().size()>0) {
                arucoCorners = application.getArucoCornersZoom();
                arucoIds = application.getArucoIdsZoom();
            }
            else return null;

            /*
             * Construction du JSON contenant les informations de tags :
             * Il est de la forme suivante :
             *  _____________________________________
             *  |   {                               |
             *  |       "numberOfTags" : ... ,      | ----> Nombre de tags détectés [=arucoCorners.size()]
             *  |       "tags":                     | ----> Array contenant les informations sur les tags détectés
             *  |           [                       |
             *  |               {                   |
             *  |                   "id" : ... ,    | ----> identifiant du premier tag détecté [= k = 0]
             *  |                   "value" : ...   | ----> valeur lue sur le tag
             *  |               },                  |
             *  |               {                   |
             *  |                   "id" : ... ,    | ----> identifiant du deuxième tag détecté
             *  |                   "value" : ...   | ----> valeur lue sur le tag
             *  |               },                  |
             *  |               ....                |        ...
             *  |           ]                       |
             *  |   }                               |
             *  |___________________________________|
             *
             */
            try {
                JSONObject tagsInfos = new JSONObject();
                JSONArray arrayTags = new JSONArray();

                tagsInfos.put("numberOfTags",arucoCorners.size());

                for (int k=0; k<arucoCorners.size(); k++){
                    JSONObject tag = new JSONObject();
                    tag.put("id",k);
                    tag.put("value",arucoIds.get(k,0)[0]);
                    arrayTags.put(tag);
                }

                tagsInfos.put("tags", arrayTags);

                Log.i(TAG, "Objet JSON - tagsInfos : "+tagsInfos.toString());

                return tagsInfos.toString();

            } catch (Exception e) {
                Log.e(TAG, "Erreur pendant la création de l'objet JSON [tagsInfos] : "+e);
                return null;
            }
        }
    };


    /**
     * La fonction takePictureUsingCameraOpenCV() permet de capturer une image depuis la caméra de Buddy grand-angle ou zoom
     * via l'utilisation de l'activity CameraActivity de OpenCV.
     *
     * @param camera : Camera grand angle ["grand-angle"] ou Zoom ["zoom"]
     * @return : L'image sous format de byte[] ou bien null si une erreur est survenue.
     */
    private byte[] takePictureUsingCameraOpenCV(String camera){

        if(camera.equals("grand-angle")) {
            /*
             * Initialisation (photo grand-angle pas encore capturée)
             */
            application.setFrameGrandAngleCaptured(false);

            /*
             * Ouverture de l'activity de camera grand-angle pour la capture de la photo
             */
            Intent intent = new Intent(VisionService.this, CamViewGrandAngleActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            /*
             * Attendre que la photo grand-angle soit prise
             */
            while (!application.isFrameGrandAngleCaptured()){
                try {
                    Thread.sleep(300);
                }
                catch (InterruptedException ignored) {}
            }

            // la photo grand-angle est capturée

            /*
             * Conversion de la frame (Mat) en BitMap puis en byte[] pour envoi via AIDL
             */
            Bitmap bitmapGrandAngle;
            try {
                bitmapGrandAngle = Bitmap.createBitmap(application.getFrameGrandAngle().cols(), application.getFrameGrandAngle().rows(), Bitmap.Config.ARGB_8888);
                org.opencv.android.Utils.matToBitmap(application.getFrameGrandAngle(), bitmapGrandAngle);
            }
            catch (Exception e){
                Log.e(TAG, "Erreur lors de la création du bitmap grand-angle : "+e);
                return null;
            }

            return Utils.getBytesFromBitmap(bitmapGrandAngle);
        }
        else if(camera.equals("zoom")) {
            /*
             * Initialisation (photo zoom pas encore capturée)
             */
            application.setFrameZoomCaptured(false);

            /*
             * Ouverture de l'activity de camera zoom pour la capture de la photo
             */
            Intent intent = new Intent(VisionService.this, CamViewZoomActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            /*
             * Attendre que la photo zoom soit prise
             */
            while (!application.isFrameZoomCaptured()){
                try {
                    Thread.sleep(300);
                }
                catch (InterruptedException ignored) {}
            }

            // la photo zoom est capturée

            /*
             * Conversion de la frame (Mat) en BitMap puis en byte[] pour envoi via AIDL
             */
            Bitmap bitmapZoom;
            try {
                bitmapZoom = Bitmap.createBitmap(application.getFrameZoom().cols(), application.getFrameZoom().rows(), Bitmap.Config.ARGB_8888);
                org.opencv.android.Utils.matToBitmap(application.getFrameZoom(), bitmapZoom);
            }
            catch (Exception e){
                Log.e(TAG, "Erreur lors de la création du bitmap zoom : "+e);
                return null;
            }

            return Utils.getBytesFromBitmap(bitmapZoom);
        }
        else return null;
    }



    /**
     *  Implémentation des méthodes de Service
     */

    @Override
    public IBinder onBind(Intent intent) {
        application = (VisionServiceApplication) getApplicationContext();
        return remoteBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

}
