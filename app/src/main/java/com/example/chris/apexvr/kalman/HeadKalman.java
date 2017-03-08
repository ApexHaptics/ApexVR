package com.example.chris.apexvr.kalman;

import android.opengl.Matrix;

import io.github.apexhaptics.apexhapticsdisplay.datatypes.Head;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.Joint;

/**
 * Created by Chris on 3/7/2017.
 */

public class HeadKalman {

    float[] headPos = new float[]{0.0f,1.8f,0.0f};
    float[] orientation = new float[]{0.0f,0.0f,0.0f,1.0f};

    public void predict(){

    }

    public void updateIMU(float[] angle){
        orientation = angle;
    }

    public void updateJoint(Joint headJoint){

        headPos = headJoint.getCoordArray();

    }

    public void updateSticker(Head head){

    }

    public float[] getHeadTransform(){

        float[] camera = quat2Mat(orientation);

        Matrix.translateM(camera,0, -headPos[0], -headPos[1], -headPos[2]);

        /*
        camera[12] = -headPos[0];
        camera[13] = -headPos[1];
        camera[14] = -headPos[2];
        */

        return camera;
    }


    private float[] quat2Mat(float[] quaternion){

        float qx = quaternion[0];
        float qy = quaternion[1];
        float qz = quaternion[2];
        float qw = -quaternion[3];

        return  new float[]{
                1 - 2*qy*qy - 2*qz*qz,
                2*qx*qy + 2*qz*qw,
                2*qx*qz - 2*qy*qw,
                0.0f,

                2*qx*qy - 2*qz*qw,
                1 - 2*qx*qx - 2*qz*qz,
                2*qy*qz + 2*qx*qw,
                0.0f,

                2*qx*qz + 2*qy*qw,
                2*qy*qz - 2*qx*qw,
                1 - 2*qx*qx - 2*qy*qy,
                0.0f,

                0.0f,
                0.0f,
                0.0f,
                1.0f
        };
    }

}
