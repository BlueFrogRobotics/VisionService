package com.bfr.main.visionservice;

public class DetectedFace {

    public int x1, y1, x2, y2;
    //classification
    public float smilingProbability, leftEyeOpenProbability, rightEyeOpenProbability;
    //tracking
    public int trackingId;

    // Landmarks
    public int x_leftEye, y_leftEye;
    public int x_rightEye, y_rightEye;
    public int x_nose, y_nose;
    public int x_leftEar, y_leftEear;
    public int x_rightEar, y_rightEar;


}
