package com.bfr.main.visionservice.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * La classe ObjectExample est un modèle d'objet parcelable pour tester l'envoi d'objet via AIDL.
 * L'objet exemple contient pour le moment :
 *        - un array d'Integer
 *        - un array de string
 *        - un boolean pour indiquer le succés/échec de l'écriture du byte[] de l'image sur la mémoire partagée [méthode 1 : utilisation mémoire partagée]
 *        - un string pour le chemin du fichier comportant le byte[] de l'image [méthode 2 : utilisation stockage locale]
 */
public class ObjectExample implements Parcelable {

    private int[] arrayInteger;
    private String[] arrayString;
    private String imageBytesFilePath;
    private boolean frameIsWrittenInSharedMemory;

    /**
     * Constructors
     */

    public ObjectExample(){
        this.frameIsWrittenInSharedMemory=false;
    }

    public ObjectExample(int[] arrayInteger, String[] arrayString, String imageBytesFilePath) {
        this.arrayInteger = arrayInteger;
        this.arrayString = arrayString;
        this.imageBytesFilePath = imageBytesFilePath;
        this.frameIsWrittenInSharedMemory=false;
    }

    /**
     * Getters and Setters
     */

    public int[] getArrayInteger() {
        return arrayInteger;
    }

    public void setArrayInteger(int[] arrayInteger) {
        this.arrayInteger = arrayInteger;
    }

    public String[] getArrayString() {
        return arrayString;
    }

    public void setArrayString(String[] arrayString) {
        this.arrayString = arrayString;
    }

    public String getImageBytesFilePath() {
        return imageBytesFilePath;
    }

    public void setImageBytesFilePath(String imageBytesFilePath) {
        this.imageBytesFilePath = imageBytesFilePath;
    }

    public boolean isFrameWrittenInSharedMemory() {
        return frameIsWrittenInSharedMemory;
    }

    public void setFrameIsWrittenInSharedMemory(boolean frameIsWrittenInSharedMemory) {
        this.frameIsWrittenInSharedMemory = frameIsWrittenInSharedMemory;
    }

    /**
     * Parcelable implementation
     */

    protected ObjectExample(Parcel in) {
        arrayInteger = in.createIntArray();
        arrayString = in.createStringArray();
        imageBytesFilePath = in.readString();
        frameIsWrittenInSharedMemory = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(arrayInteger);
        dest.writeStringArray(arrayString);
        dest.writeString(imageBytesFilePath);
        dest.writeByte((byte) (frameIsWrittenInSharedMemory ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ObjectExample> CREATOR = new Creator<ObjectExample>() {
        @Override
        public ObjectExample createFromParcel(Parcel in) {
            return new ObjectExample(in);
        }

        @Override
        public ObjectExample[] newArray(int size) {
            return new ObjectExample[size];
        }
    };

}
