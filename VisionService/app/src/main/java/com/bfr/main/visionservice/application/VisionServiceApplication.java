package com.bfr.main.visionservice.application;

import android.app.Application;
import android.util.Log;

import com.bfr.main.visionservice.R;
import com.bfr.main.visionservice.observer.IDBObserver;
import com.newtronlabs.sharedmemory.SharedMemoryProducer;
import com.newtronlabs.sharedmemory.prod.memory.ISharedMemory;

import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * La classe VisionServiceApplication est la classe Application, utilisée pour la sauvegarde des variables globales.
 *
 * Dès sa création, trois régions de mémoire partagée sont alloués :
 *  --> une pour le streaming des images de camera Grand-Angle.
 *  --> une pour le streaming des images de camera Zoom.
 *  --> une pour l'image résultante du traitment par CV.
 *
 * Elle fournie aussi les fonctions :
 *  - writeCvResultingFrameInSharedMemory() : pour écrire le byte[] de l'image résultante du traitment par CV sur la mémoire partagée.
 *  - registerObserver() , removeObserver() , notifyObservers() : pour la gestion des notifications entre les classes du projet [pattern Observer]
 *
 */
public class VisionServiceApplication extends Application {

    private static final String TAG = "SERVICE_VISION_Application";

    private List<IDBObserver> observers = new ArrayList<>();

    private ISharedMemory sharedMemoryOfStreamFramesGrandAngle; // l'objet de mémoire partagée [région Stream-Frames-GrandAngle], utilisé pour écrire les byte[] des frames Grand-Angle
    private ISharedMemory sharedMemoryOfStreamFramesZoom; // l'objet de mémoire partagée [région Stream-Frames-Zoom], utilisé pour écrire les byte[] des frames Zoom
    private ISharedMemory sharedMemoryOfCvResultingFrame; // l'objet de mémoire partagée, utilisé pour écrire le byte[] de l'image résultante du traitement par CV

    private boolean isFrameGrandAngleCaptured; // Indique si l'image Grand-Angle est prise ou pas encore
    private Mat frameGrandAngle; // L'image Grand-Angle capturée sous format de Mat

    private boolean isFrameZoomCaptured; // Indique si l'image Zoom est prise ou pas encore
    private Mat frameZoom; // L'image Zoom capturée sous format de Mat


    /**
     * Getters and Setters
     */

    public ISharedMemory getSharedMemoryOfStreamFramesGrandAngle() {
        return sharedMemoryOfStreamFramesGrandAngle;
    }

    public void setSharedMemoryOfStreamFramesGrandAngle(ISharedMemory sharedMemoryOfStreamFramesGrandAngle) {
        this.sharedMemoryOfStreamFramesGrandAngle = sharedMemoryOfStreamFramesGrandAngle;
    }

    public ISharedMemory getSharedMemoryOfStreamFramesZoom() {
        return sharedMemoryOfStreamFramesZoom;
    }

    public void setSharedMemoryOfStreamFramesZoom(ISharedMemory sharedMemoryOfStreamFramesZoom) {
        this.sharedMemoryOfStreamFramesZoom = sharedMemoryOfStreamFramesZoom;
    }

    public ISharedMemory getSharedMemoryOfCvResultingFrame() {
        return sharedMemoryOfCvResultingFrame;
    }

    public void setSharedMemoryOfCvResultingFrame(ISharedMemory sharedMemoryOfCvResultingFrame) {
        this.sharedMemoryOfCvResultingFrame = sharedMemoryOfCvResultingFrame;
    }

    public boolean isFrameGrandAngleCaptured() {
        return isFrameGrandAngleCaptured;
    }

    public void setFrameGrandAngleCaptured(boolean frameGrandAngleCaptured) {
        isFrameGrandAngleCaptured = frameGrandAngleCaptured;
    }

    public Mat getFrameGrandAngle() {
        return frameGrandAngle;
    }

    public void setFrameGrandAngle(Mat frameGrandAngle) {
        this.frameGrandAngle = frameGrandAngle;
    }

    public boolean isFrameZoomCaptured() {
        return isFrameZoomCaptured;
    }

    public void setFrameZoomCaptured(boolean frameZoomCaptured) {
        isFrameZoomCaptured = frameZoomCaptured;
    }

    public Mat getFrameZoom() {
        return frameZoom;
    }

    public void setFrameZoom(Mat frameZoom) {
        this.frameZoom = frameZoom;
    }



    /**
     * Initialisations + Allocation de régions de mémoire partagée
     */
    @Override
    public void onCreate() {
        super.onCreate();

        isFrameGrandAngleCaptured = false;
        isFrameZoomCaptured = false;

        /*
         * Allocation de [size_MB_region_shared_memory_stream_frames_grand_angle]MB de mémoire partagée pour le streaming GrandAngle
         */
        int sizeInBytes_stream_frames_grandAngle = Integer.parseInt(getString(R.string.size_MB_region_shared_memory_stream_frames_grand_angle))*(1024*1024);
        String regionName_stream_frames_grandAngle = getString(R.string.name_region_shared_memory_stream_frames_grand_angle);
        try {
            this.sharedMemoryOfStreamFramesGrandAngle = SharedMemoryProducer.getInstance().allocate(regionName_stream_frames_grandAngle, sizeInBytes_stream_frames_grandAngle);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'allocation de mémoire partagée (" + regionName_stream_frames_grandAngle + " , "+sizeInBytes_stream_frames_grandAngle + ") : " + e);
        }


        /*
         * Allocation de [size_MB_region_shared_memory_stream_frames_zoom]MB de mémoire partagée pour le streaming Zoom
         */
        int sizeInBytes_stream_frames_zoom = Integer.parseInt(getString(R.string.size_MB_region_shared_memory_stream_frames_zoom))*(1024*1024);
        String regionName_stream_frames_zoom = getString(R.string.name_region_shared_memory_stream_frames_zoom);
        try {
            this.sharedMemoryOfStreamFramesZoom = SharedMemoryProducer.getInstance().allocate(regionName_stream_frames_zoom, sizeInBytes_stream_frames_zoom);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'allocation de mémoire partagée (" + regionName_stream_frames_zoom + " , "+sizeInBytes_stream_frames_zoom + ") : " + e);
        }


        /*
         * Allocation de [size_MB_region_shared_memory_cv_resulting_frame]MB de mémoire partagée pour l'image résultante du traitment par CV
         */
        int sizeInBytes_cv_resulting_frame = Integer.parseInt(getString(R.string.size_MB_region_shared_memory_cv_resulting_frame))*(1024*1024);
        String regionName_cv_resulting_frame = getString(R.string.name_region_shared_memory_cv_resulting_frame);
        try {
            this.sharedMemoryOfCvResultingFrame = SharedMemoryProducer.getInstance().allocate(regionName_cv_resulting_frame, sizeInBytes_cv_resulting_frame);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'allocation de mémoire partagée (" + regionName_cv_resulting_frame + " , "+sizeInBytes_cv_resulting_frame + ") : " + e);
        }

    }

    /**
     * la fonction writeCvResultingFrameInSharedMemory() permet d'écrire le byte[] de l'image résultante du traitment par CV sur la mémoire partagée.
     * @param cvResultingFrameInBytes : le byte[] de l'image à écrire sur la mémoire.
     * @return : - true : si succés de l'écriture
     *           - false : si erreur
     */
    public boolean writeCvResultingFrameInSharedMemory(byte[] cvResultingFrameInBytes){
        try {
            this.sharedMemoryOfCvResultingFrame.writeBytes(cvResultingFrameInBytes, 0, 0, cvResultingFrameInBytes.length);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'écriture du byte[] de l'image résultante du traitment par CV sur la mémoire partagée : " + e);
            return false;
        }
    }

    /**
     * la fonction notifyObservers() permet d'envoyer un message "notification" aux classes qui implémentent IDBObserver.
     * @param message : le message à envoyer
     */
    public void notifyObservers(String message) {
        for (int i = 0; i < observers.size(); i++) {
            try {
                IDBObserver ob = observers.get(i);
                ob.update(message);
            } catch (IOException e) {
                Log.e(TAG, "Erreur lors de l'envoi de la notification aux observateurs [ "+message+" ] :" + e);
            }
        }
    }

    /**
     * la fonction registerObserver() permet de s'enregistrer au pattern Observer afin de recevoir les notifications
     */
    public void registerObserver(IDBObserver observer) {
        observers.add(observer);
    }

    /**
     * la fonction removeObserver() permet de se désinscrire du pattern Observer pour ne plus recevoir les notifications
     */
    public void removeObserver(IDBObserver observer) {
        observers.remove(observer);
    }

}
