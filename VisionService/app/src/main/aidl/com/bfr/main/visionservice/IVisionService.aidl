// IVisionService.aidl
package com.bfr.main.visionservice;

// Declare any non-default types here with import statements
import com.bfr.main.visionservice.models.ObjectExample;

interface IVisionService {
    void startFrameStream(String camera);
    void stopFrameStream(String camera);
    ObjectExample getObjectExample();
    String getImageByteFilePath(String camera);
    String getTagsInfos(String camera);
}
