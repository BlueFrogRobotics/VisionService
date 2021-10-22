package com.bfr.main.visionservice.utils;

import android.os.RemoteException;

import com.bfr.usbservice.BodySensorData;
import com.bfr.usbservice.HeadSensorData;
import com.bfr.usbservice.IUsbAidlCbListner;
import com.bfr.usbservice.MotorHeadData;
import com.bfr.usbservice.MotorMotionData;

// Class for callbac
public class BuddyData extends IUsbAidlCbListner.Stub {

// Enable / disable
public String yesStatus, noStatus, leftWheelStatus, rightWheelStatus;
// position
public int yesPos, noPos;


    @Override
    // called when the datas from the motor are received
    public void ReceiveMotorMotionData(MotorMotionData msg) throws RemoteException {
        leftWheelStatus = msg.leftWheelMode;
        rightWheelStatus = msg.rightWheelMode;
    }

    @Override
    // called when the datas from the head motor are received
    public void ReceiveMotorHeadData(MotorHeadData msg) throws RemoteException {
        yesStatus = msg.yesMode;
        noStatus = msg.noMode;
        yesPos = msg.yesPosition;
        noPos = msg.noPosition;
    } // end receiveMotorHead Data

    @Override
    // called when the datas from the head sensor motor are received
    public void ReceiveHeadSensorData(HeadSensorData msg) throws RemoteException {
    }

    @Override
    // called when the datas from the body sensor motor are received
    public void ReceiveBodySensorData(BodySensorData data) throws RemoteException {
    } // receiveBodySensorData


} // end BuddyData class

