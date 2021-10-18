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
 * Dès sa création, deux régions de mémoire partagée sont alloués :
 *  --> une pour le streaming des images.
 *  --> l'autre pour l'image résultante du traitment par CV.
 *
 * Elle fournie aussi les fonctions :
 *  - writeCvResultingFrameInSharedMemory() : pour écrire le byte[] de l'image résultante du traitment par CV sur la mémoire partagée.
 *  - registerObserver() , removeObserver() , notifyObservers() : pour la gestion des notifications entre les classes du projet [pattern Observer]
 *
 */
public class VisionServiceApplication extends Application {

    private static final String TAG = "SERVICE_VISION_Application";

    private List<IDBObserver> observers = new ArrayList<>();

    private ISharedMemory sharedMemoryOfStreamFrames; // l'objet de mémoire partagée, utilisé pour écrire les byte[] des frames pour le partage du flux de frames avec les autres applications
    private ISharedMemory sharedMemoryOfCvResultingFrame; // l'objet de mémoire partagée, utilisé pour écrire le byte[] de l'image résultante du traitement par CV

    private boolean isOpenCVFrameCaptured; // Indique si l'image est prise ou pas encore
    private Mat openCVFrame; // L'image capturée sous format de Mat


    /**
     * Getters and Setters
     */

    public ISharedMemory getSharedMemoryOfStreamFrames() {
        return sharedMemoryOfStreamFrames;
    }

    public void setSharedMemoryOfStreamFrames(ISharedMemory sharedMemoryOfStreamFrames) {
        this.sharedMemoryOfStreamFrames = sharedMemoryOfStreamFrames;
    }

    public ISharedMemory getSharedMemoryOfCvResultingFrame() {
        return sharedMemoryOfCvResultingFrame;
    }

    public void setSharedMemoryOfCvResultingFrame(ISharedMemory sharedMemoryOfCvResultingFrame) {
        this.sharedMemoryOfCvResultingFrame = sharedMemoryOfCvResultingFrame;
    }

    public boolean isOpenCVFrameCaptured() {
        return isOpenCVFrameCaptured;
    }

    public void setOpenCVFrameCaptured(boolean openCVFrameCaptured) {
        isOpenCVFrameCaptured = openCVFrameCaptured;
    }

    public Mat getOpenCVFrame() {
        return openCVFrame;
    }

    public void setOpenCVFrame(Mat openCVFrame) {
        this.openCVFrame = openCVFrame;
    }


    /**
     * Initialisations + Allocation de région de mémoire partagée
     */
    @Override
    public void onCreate() {
        super.onCreate();

        isOpenCVFrameCaptured = false;

        /*
         * Allocation de [size_MB_region_shared_memory_stream_frames]MB de mémoire partagée pour le streaming
         */
        int sizeInBytes_stream_frames = Integer.parseInt(getString(R.string.size_MB_region_shared_memory_stream_frames))*(1024*1024);
        String regionName_stream_frames = getString(R.string.name_region_shared_memory_stream_frames);
        try {
            this.sharedMemoryOfStreamFrames = SharedMemoryProducer.getInstance().allocate(regionName_stream_frames, sizeInBytes_stream_frames);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'allocation de mémoire partagée (" + regionName_stream_frames + " , "+sizeInBytes_stream_frames + ") : " + e);
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
