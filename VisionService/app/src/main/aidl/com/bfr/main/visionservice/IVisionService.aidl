// IVisionService.aidl
package com.bfr.main.visionservice;

// Declare any non-default types here with import statements
import com.bfr.main.visionservice.models.ObjectExample;

interface IVisionService {
    void startFrameStream();
    void stopFrameStream();
    ObjectExample getObjectExample();
    String getImageByteFilePath();
}
