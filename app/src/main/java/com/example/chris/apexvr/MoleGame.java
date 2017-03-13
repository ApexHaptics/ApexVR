package com.example.chris.apexvr;

import android.opengl.Matrix;

import com.example.chris.apexvr.kalman.ApexSensors;

/**
 * Created by Chris on 3/12/2017.
 */

public class MoleGame {

    private float[] tablelocation;
    private boolean ready = false;

    public MoleGame(ApexGraphics graphics, ApexSensors sensors){
        tablelocation = new float[16];
        Matrix.setIdentityM(tablelocation,0);
    }


    public void upadte(Object gamePacket, Object molePachet){

        if(!ready){
            ready = true;
            Matrix.setIdentityM(tablelocation,0);
            Matrix.translateM(tablelocation,0,0,0,4);
            return;
        }



    }

    public float[] getTablelocation() {
        return tablelocation;
    }
}
