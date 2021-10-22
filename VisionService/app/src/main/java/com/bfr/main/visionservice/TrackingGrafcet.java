package com.bfr.main.visionservice;


import android.os.RemoteException;
import android.util.Log;

import com.bfr.usbservice.IUsbCommadRsp;
import com.bfr.main.visionservice.utils.bfr_Grafcet;

import org.opencv.core.Rect;

public class TrackingGrafcet extends bfr_Grafcet {

    TrackingGrafcet(String mname) {
        super(mname);
        this.grafcet_runnable = mysequence;

    }

    private TrackingGrafcet grafcet=this;

    // Static variable (to manage the grafcet from outside)
    public static int step_num =0;
    public static boolean go = false;

    private int previous_step = 0;
    private double time_in_curr_step = 0;
    private boolean bypass = false;

    public static Rect tracked=new Rect();

    //Requested speed
    private int yesSpeed = 30;
    //motor response
    private String YesMvtAck = "YesMove";


    // Define the sequence/grafcet to be executed
   /* This provides a template for a grafcet.
   The sequence is as follows:
   - check the checkbox
   - Move the No from Left to right
   - Move the no from right to left
   - If the check box is unchecked then stop
   - if not, repeat
    */
    // runable for grafcet
    private Runnable mysequence = new Runnable()
    {
        @Override
        public void run()
        {
            // if step changed
            if( !(step_num == previous_step)) {
                // display current step
                Log.i("GRAFCET", "current step: " + step_num + "  ");
                // update
                previous_step = step_num;
            } // end if step = same


            // which grafcet step?
            switch (step_num) {
                case 0: // Wait for checkbox
                    //wait until check box
                    if (go) {
                        // go to next step
                        step_num = 1;
                    }
                    break;

                case 1: // Enable No
                    try {
                        grafcet.mBuddySDK.getUsbInterface().enableNoMove(1, new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String success) throws RemoteException {
                                Log.i("enable No SUCESS:", success);
                            }

                            @Override
                            public void onFailed(String error) throws RemoteException {
                                Log.e("enable No FAILED", error);
                            }
                        });
                    } //end try enable
                    catch (RemoteException e) {
                        e.printStackTrace();
                    } // end catch

                    // go to next step
                    step_num = 2;
                    break;

                case 2: //wait for status No enable
                    // if no not disabled
                    if (!grafcet.mBuddyData.noStatus.toUpperCase().contains("DISABLE")) {
                        // go to next step
                        step_num = 3;
                    }
                    break;

                case 3: // Move left or right or exit

                    // if checkbox unchecked
                    if (!go)
                    {
                        // go to next step
                        step_num = 10;
                    }
                    // if face to track too much on the left
                    if (tracked.x < 350)
                    {
                        // go to next step
                        step_num = 20;
                    }
                    else if (tracked.x > 450)
                    {
                        // go to next step
                        step_num = 30;
                    }

                case 20: // Move to the left

                    // start moving

                   // go to next step
                    step_num = 21;
                    break;

                case 21: // Wait for target to be in range

                    // if tracked target in range
                    if (tracked.x > 350)
                    {   // go to next step
                        step_num = 3;
                    }
                    break;

                case 30: // Move to the right

                    // start moving

                    // go to next step
                    step_num = 31;
                    break;

                case 31: // Wait for target to be in range

                    // if tracked target in range
                    if (tracked.x < 450)
                    {   // go to next step
                        step_num = 3;
                    }
                    break;


                case 10: // Disable motor

                    // disable motor

                    // go to next step
                    step_num = 11;
                    break;

                case 11: // Wait for motor to be disable

                    // if tracked target in range
                    if (grafcet.mBuddyData.noStatus.toUpperCase().contains("DISABLE"))
                    {   // go to next step
                        step_num = 0;
                    }
                    break;


                default :
                    // go to next step
                    step_num = 0;
                    break;
            } //End switch

        } // end run
    }; // end new runnable

}
