package com.example.chris.apexvr.kalman;

import android.opengl.Matrix;
import android.os.SystemClock;

import org.ejml.simple.SimpleMatrix;

import io.github.apexhaptics.apexhapticsdisplay.datatypes.HeadPacket;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.Joint;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.JointPacket;

/**
 * Created by Chris on 3/7/2017.
 */

public class HeadKalman {

    private static final String TAG = "Apex_Kalman";
    float[] translation = new float[16];
    float[] rotation = new float[16];

    private KinectCorrectionData kinectCorrectionData;

    private SimpleMatrix A;
    private SimpleMatrix C;
    private SimpleMatrix R;
    private SimpleMatrix Q;

    private SimpleMatrix x;
    private SimpleMatrix P;


    private Long frameTime;

    private boolean ready;

    public HeadKalman(){

        A = new SimpleMatrix(3,3);
        A.set(0,0,1);
        A.set(1,1,1);
        A.set(2,2,1);

        C = new SimpleMatrix(2,3);
        C.setRow(0,0,1,0,0);
        C.setRow(1,0,1,0,1);

        R = new SimpleMatrix(2,2);
        R.set(0,0,0.0157);
        R.set(1,1,0.1571);

        Q = new SimpleMatrix(3,3);
        Q.set(0,0,0.1571);
        Q.set(1,1,1.5708);
        Q.set(2,2,0.000314);


        Matrix.setIdentityM(rotation,0);
        Matrix.setIdentityM(translation,0);
        Matrix.translateM(translation,0,0,-1.8f,0);


    }

    float stickerYaw;
    float offSet = 0;
    float[] pos = new float[3];

    public void step(float[] orientation, HeadPacket headPacket, JointPacket jointPacket){

        if(!ready){

            ready = !(headPacket == null || jointPacket == null);

            ready = !(headPacket == null);//TODO:REMOVE

            if(ready){
                frameTime = SystemClock.currentThreadTimeMillis();

                x = new SimpleMatrix(3,1);
                x.set(0,extractYaw(orientation));

                float[] upVector = upVector(orientation);
                kinectCorrectionData = calculateKinectCorrections(
                        headPacket.rotMat, upVector);

                float[] sticker = correctKinectMatrix(kinectCorrectionData,headPacket.rotMat);

                stickerYaw = extractYaw(sticker);

                x.set(0,extractYaw(sticker) - x.get(0));

                P = new SimpleMatrix(3,3);
                P.set(0,0,0.0157);
                P.set(1,1,3.1416);
                P.set(2,2,0.1571);

            }

            return;
        }

        long time = SystemClock.currentThreadTimeMillis();
        float dt = (time - frameTime)/1000;
        frameTime = time;
        A.set(0,1,dt);

        SimpleMatrix xk = A.mult(x);
        P.set(A.mult(P.mult(A.transpose())).plus(Q));

        //double imu = unroll(extractYaw(orientation),x.get(0));
        //Log.i(TAG,"IMU: " + imu);

        /*

        if(headPacket == null){

            SimpleMatrix subC = C.extractVector(true,0);
            double subR = R.get(0,0);

            double yk = imu - subC.mult(xk).get(0);
            double s = subC.mult(P.mult(subC.transpose())).get(0) + subR;
            SimpleMatrix K = P.mult(subC.transpose()).divide(s);

            x.set(xk.plus(K.scale(yk)));
            P.set(SimpleMatrix.identity(3).minus(K.mult(subC)).mult(P));

        }else{

            float[] upVector = upVector(orientation);
            kinectCorrectionData = calculateKinectCorrections(
                    headPacket.rotMat, upVector);

            float[] sticker = correctKinectMatrix(kinectCorrectionData,headPacket.rotMat);
            float stickerYaw = unroll(extractYaw(sticker),x.get(0) + x.get(2));

            //Log.i(TAG,"STR: " + stickerYaw);

            SimpleMatrix z = new SimpleMatrix(2,1,false,new double[]{imu,stickerYaw});
            SimpleMatrix yk = z.minus(C.mult(xk));
            SimpleMatrix s = C.mult(P.mult(C.transpose())).plus(R);
            SimpleMatrix k = P.mult(C.transpose()).mult(s.invert());

            x.set(xk.plus(k.mult(yk)));
            P.set(SimpleMatrix.identity(3).minus(k.mult(C)).mult(P));

        }
        */

        //Log.i(TAG,"ref: " + x.get(0) + "off: " + x.get(2) + "abs: " + x.get(2) + x.get(0));

        //Log.i(TAG,"off: " + x.get(2));
        //Log.i(TAG,"abs: " + (x.get(2) + x.get(0)));

        if(headPacket != null){
            float[] upVector = upVector(orientation);
            kinectCorrectionData = calculateKinectCorrections(
                    headPacket.rotMat, upVector);

            float[] sticker = correctKinectMatrix(kinectCorrectionData,headPacket.rotMat);
            float newYaw = extractYaw(sticker);


            float yawmod = (float) ((Math.abs(stickerYaw)+Math.PI)%(2*Math.PI)-Math.PI);
            if(stickerYaw < 0){
                yawmod =-yawmod;
            }

            float diff = yawmod - newYaw;

            if(Math.abs(diff) < Math.PI){
                stickerYaw -= diff * 0.1f;
            }else{
                if(diff > 0){
                    stickerYaw += (float)(diff - Math.PI)*0.05f;
                }else{
                    stickerYaw += (float)(diff + Math.PI)*0.05f;
                }
            }

            pos[0] = headPacket.X*-1; // The data is pre-filtered
            pos[1] = headPacket.Y-1.8f;
            pos[2] = headPacket.Z;
            //pos[2] = headPacket.Z+1.5f;

            Matrix.translateM(translation,0,0,-1.2f,0);
        }

        //Log.i(TAG,"stickerYaw: " + stickerYaw);

        float imu = -extractYaw(orientation);

        offSet = 0.01f*(stickerYaw - imu) + (1-0.01f)*offSet;




        for(int i = 12; i<15; ++i){
            orientation[i] = 0;
        }

        //Matrix.rotateM(rotation,0,1.0f,0.0f,1.0f,0.0f);
        //Matrix.setIdentityM(rotation,0);
        Matrix.rotateM(rotation,0,orientation,0, (float) Math.toDegrees(offSet),0.0f,1.0f,0.0f);
        //Matrix.rotateM(rotation,0, (float) Math.toDegrees(offSet + imu),0.0f,1.0f,0.0f);

        //Matrix.rotateM(rotation,0,orientation,0, (float) Math.toDegrees(x.get(0)+x.get(2)-imu),0,1,0);

        if(jointPacket != null){
            Joint head = jointPacket.getJoint(Joint.JointType.Head);

//            pos[0] = (float) (pos[0]*0.5-0.5*head.X);
//            pos[1] = (float) (pos[1]*0.5+0.5*(head.Y -1.8f));
//            pos[2] = (float) (pos[2]*0.5+0.5*head.Z);

            //Matrix.setIdentityM(translation,0);
            //Matrix.translateM(translation,0,-head.X,-head.Y,-head.Z);
        }

    }


    public float[] getHeadTransform(){

        float[] camera = new float[16];
        Matrix.setIdentityM(translation,0);
        Matrix.translateM(translation,0,pos[0],pos[1],pos[2]);

        Matrix.multiplyMM(camera,0, rotation,0,translation,0);

        return camera;
    }

    private float unroll(float v,double roll) {

        return v;

//        if(v > 0){
//            if(roll < -Math.PI/2){
//                return (float) (-roll % Math.PI - Math.PI + v);
//            }
//            return v;
//        }else{
//            if(roll > Math.PI/2){
//                return (float) (roll % Math.PI + Math.PI + v);
//            }
//            return v;
//        }
    }

    private float[] upVector(float[] transform){
        float[] up = new float[4];
        float[] y = new float[]{0, 1, 0, 1};

        Matrix.multiplyMV(up,0,transform,0,y,0);

        return up;
    }

    private float extractYaw(float[] transform){

        return (float) Math.atan2(transform[openGlMatrixIndex(0,2)],transform[openGlMatrixIndex(2,2)]);

    }
    private int openGlMatrixIndex(int m, int n){
        return m * 4 + n;
    }

    public boolean isReady() {
        return ready;
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

    public static class KinectCorrectionData {
        float roll;
        float pitch;
        float[] correction_premultiplier_matrix = new float[16];
    }

    public static KinectCorrectionData calculateKinectCorrections(float[] kinect_space_transform, float[] up_vector){
        KinectCorrectionData output = new KinectCorrectionData();
        float[] up_vector_kinect = new float[4];
        Matrix.multiplyMV(up_vector_kinect, 0, kinect_space_transform, 0, up_vector, 0);
        output.roll = (float)Math.atan2(up_vector_kinect[0], up_vector_kinect[1]);
        float[] kinect_roll_corrector = new float[16];
        Matrix.setRotateM(kinect_roll_corrector, 0, (float)Math.toDegrees(output.roll), 0, 0, 1);
        float[] up_vector_kinect_corrected = new float[4];
        Matrix.multiplyMV(up_vector_kinect_corrected, 0, kinect_roll_corrector, 0, up_vector_kinect, 0);
        output.pitch = (float)Math.atan2(-up_vector_kinect_corrected[2], up_vector_kinect_corrected[1]);
        float[] kinect_pitch_corrector = new float[16];
        Matrix.setRotateM(kinect_pitch_corrector, 0, (float)Math.toDegrees(output.pitch), 1, 0, 0);
        Matrix.multiplyMM(output.correction_premultiplier_matrix, 0, kinect_pitch_corrector, 0, kinect_roll_corrector, 0);
        return output;
    }

    public static float[] correctKinectVector(KinectCorrectionData correctionData, float[] vector){
        float[] correctedVector = new float[4];
        Matrix.multiplyMV(correctedVector, 0, correctionData.correction_premultiplier_matrix, 0, vector, 0);
        return correctedVector;
    }

    public static float[] correctKinectMatrix(KinectCorrectionData correctionData, float[] matrix){
        float[] correctedMatrix = new float[16];
        Matrix.multiplyMM(correctedMatrix, 0, correctionData.correction_premultiplier_matrix, 0, matrix, 0);
        return correctedMatrix;
    }


}
