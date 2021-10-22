package com.bfr.main.visionservice.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.bfr.buddysdk.sdk.BuddySDK;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class bfr_Grafcet
{
    private final String TAG = "BFR_GRAFCET";

    // Default Params for scheduler
    // initial delay before start
    private final long INITIALDELAY = 0;
    // time period of execution
    private final long TIME_PERIOD = 20;

    // grafcet step number


    // Scheduler for grafcet
    private ScheduledExecutorService myscheduler ;

    // SDK , data and context
    public Context gcontext;
    public BuddySDK mBuddySDK;
    public BuddyData mBuddyData;
    public Activity mMainActivity;

    // Name
    public String name = "";

    // state
    public boolean started = false;

    // Runnable containing the grafcet sequence
    public Runnable grafcet_runnable;

    // *********************  Constructor overloads
    public bfr_Grafcet(Runnable runnable, String mname)
    {
        name = mname;
        grafcet_runnable = runnable;
    }

    // without runnable
    public bfr_Grafcet(String mname)
    {
        name = mname;
    }

    //************************** Passing the BFR SDK and context
    // 2021/09/28: temporary, waiting for new architecture and multi activity compatible SDK

    // passing context sdk instance and data instance from main activity
    public void init(Context _context, BuddySDK _sdk, BuddyData _data)
    {
        // passing context sdk instance and data instance from main activity
        gcontext = _context;
        mBuddySDK = _sdk;
        mBuddyData = _data;
    }

    // passing context sdk instance and data instance from main activity
    public void init(Context _context, BuddySDK _sdk, BuddyData _data, Activity _mainActivity)
    {
        // passing context sdk instance and data instance from main activity
        gcontext = _context;
        mBuddySDK = _sdk;
        mBuddyData = _data;
        mMainActivity = _mainActivity;
    }


    //********************* Starting

    // start grafcet with default parameters
    public void start()
    {
        //if not started yet
        if (myscheduler == null || myscheduler.isShutdown())
        {
            //Log.i(TAG, "Grafcet " + name + " starting \n" + Log.getStackTraceString(new Exception()));
            Log.i(TAG, "\n********* GRAFCET **********\nGrafcet " + name + " starting \n" + Log.getStackTraceString(new Exception()));
            //init thread
            myscheduler = Executors.newScheduledThreadPool(1);
            // start scheduled task
            myscheduler.scheduleWithFixedDelay(grafcet_runnable, INITIALDELAY, TIME_PERIOD, TimeUnit.MILLISECONDS);
        }
        else // grafcet already started
        {
            //Log.i(TAG, "Grafcet "+ name+ " already started. Skipping \n" + Log.getStackTraceString(new Exception()));
            Log.i(TAG, "\n********* GRAFCET **********\nGrafcet "+ name+ " already started. Skipping \n" + Log.getStackTraceString(new Exception()));
        }

        // update state
        started = true;


        // Monitoring the step number
//        steNumber.setListener(new listenedVar.ChangeListener() {
//            @Override
//            public void onChange() {
//                Log.i(name, "Current step = " + steNumber.get());
//            } // end onchange
//        }); // end set listener

    } // end start

    // start grafcet and specify params (time period, ...)
    public void start(long mtime_period)
    {
        //if not started yet
        if (myscheduler.isShutdown())
        {
            Log.i(TAG, "\n********* GRAFCET **********\nGrafcet " + name + " starting \n");
            // log the stack strace in verbose mode
            Log.v(TAG, "Grafcet " + name + " called at " + Log.getStackTraceString(new Exception()));
            //init thread
            myscheduler = Executors.newScheduledThreadPool(1);
            // start scheduled task
            //myscheduler.scheduleWithFixedDelay(grafcet_runnable, INITIALDELAY, mtime_period, TimeUnit.MILLISECONDS);
            myscheduler.scheduleWithFixedDelay(grafcet_runnable, INITIALDELAY, mtime_period, TimeUnit.MILLISECONDS);
        }
        else // grafcet already started
        {
            Log.i(TAG, "\n********* GRAFCET **********\nGrafcet "+ name+ " already started. Skipping \n" );
            // log the stack strace in verbose mode
            Log.v(TAG, "Grafcet " + name + " called at " + Log.getStackTraceString(new Exception()));
        }

        // update state
        started = true;
    }


    public void start(long mtime_period, long m_init_delay )
    {
        //if not started yet
        if (myscheduler.isShutdown())
        {
            Log.i(TAG, "\n********* GRAFCET **********\nGrafcet " + name + " starting \n");
            // log the stack strace in verbose mode
            Log.v(TAG, "Grafcet " + name + " called at " + Log.getStackTraceString(new Exception()));
            //init thread
            myscheduler = Executors.newScheduledThreadPool(1);
            // start scheduled task
            myscheduler.scheduleWithFixedDelay(grafcet_runnable, m_init_delay, mtime_period, TimeUnit.MILLISECONDS);
        }
        else // grafcet already started
        {
            Log.i(TAG, "\n********* GRAFCET **********\nGrafcet "+ name+ " already started. Skipping \n");
            // log the stack strace in verbose mode
            Log.v(TAG, "Grafcet " + name + " called at " + Log.getStackTraceString(new Exception()));
        }

        // update state
        started = true;

    }

    // Stop the grafcet
    public void stop()
    {
        //if not started yet
        if (myscheduler.isShutdown())
        {
            Log.i(TAG, "\n********* GRAFCET **********\nGrafcet " + name + " not started yet \n");
            // log the stack strace in verbose mode
            Log.v(TAG, "Grafcet " + name + " Required to stop at " + Log.getStackTraceString(new Exception()));
        }
        else // grafcet already started
        {
            Log.i(TAG, "\n********* GRAFCET **********\nShutting down grafcet " + name + "\n");
            // log the stack strace in verbose mode
            Log.v(TAG, "Grafcet " + name + " required to stop at " + Log.getStackTraceString(new Exception()));
            myscheduler.shutdown();
        }

        // update state
        started = false;
    }
} // end of class bfr_grafcet
