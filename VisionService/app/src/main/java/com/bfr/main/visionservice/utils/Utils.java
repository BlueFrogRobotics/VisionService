package com.bfr.main.visionservice.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;

public class Utils {

    private static final String TAG = "SERVICE_VISION_utils";

    /**
     * La fonction getBytesFromBitmap() permet de convertir un Bitmap à un byte[].
     * @param bitmap : le bitmap à convertir.
     * @return : le résultat de la conversion en byte[].
     */
    public static byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    /**
     * La fonction getBitmapFromBytes() permet de convertir un byte[] à un Bitmap.
     * @param bytes : le byte[] à convertir.
     * @return : le résultat de la conversion en Bitmap.
     */
    public static Bitmap getBitmapFromBytes(byte[] bytes) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            Log.e(TAG,"Erreur pendant la conversion du bitmap en byte[] : "+e);
        }
        return bitmap;
    }

}
