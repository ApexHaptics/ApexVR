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
    private static final float KINETEC_HEIGHT = 1.8f;

    float[] translation = new float[16];
    float[] rotation = new float[16];
    float[] leftHand = new float[16];
    float[] rigthHand = new float[16];
    float imuYaw = 0;
    float stickerYaw = 0;

    private KinectCorrectionData kinectCorrectionData;

    private SimpleMatrix A;
    private SimpleMatrix C;
    private SimpleMatrix Ryaw;
    private SimpleMatrix Qyaw;
    private SimpleMatrix Rlin;
    private SimpleMatrix Qlin;

    private SimpleMatrix xYaw;
    private SimpleMatrix Pyaw;

    private SimpleMatrix[] xPos;
    private SimpleMatrix[] PPos;

    private Long frameTime;

    private boolean ready;

    public HeadKalman(){

        A = new SimpleMatrix(2,2);
        A.set(0,0,1);
        A.set(0,1,1);
        A.set(1,1,1);

        C = new SimpleMatrix(2,2);
        C.set(0,0,1);
        C.set(1,1,1);

        Ryaw = new SimpleMatrix(2,2);
        Ryaw.set(0,0,4);
        Ryaw.set(1,1,.01);

        Qyaw = new SimpleMatrix(2,2);
        Qyaw.set(0,0,0);
        Qyaw.set(1,1,1.5708);

        Rlin = new SimpleMatrix(2,2);
        Rlin.set(0,0,0.2);
        Rlin.set(1,1,0.2);

        Qlin = new SimpleMatrix(3,3);
        Qlin.set(0,0,0.2);
        Qlin.set(1,1,1.5);
        Qlin.set(2,2,0.01);


        Matrix.setIdentityM(rotation,0);
        Matrix.setIdentityM(translation,0);
        Matrix.setIdentityM(leftHand,0);
        Matrix.setIdentityM(rigthHand,0);
        Matrix.translateM(translation,0,0,-1.8f,0);


    }
    float[] pos = new float[3];

    public void step(float[] orientation, HeadPacket headPacket, JointPacket jointPacket) {

        rotation = orientation;



        if (!ready) {
            ready = !(headPacket == null || jointPacket == null);

            if (ready) {
                startKalman(orientation, headPacket, jointPacket);
            }

            return;
        }


        //float newImuYaw = unroll(-extractYaw(orientation), (float) xYaw.get(0));
        float dImuYaw = unroll((-extractYaw(orientation)) - imuYaw, 0);
        imuYaw = imuYaw + dImuYaw;

        long time = SystemClock.currentThreadTimeMillis();
        float dt = (time - frameTime) / 1000;
        frameTime = time;
        //A.set(0,1,dt);

        {
            SimpleMatrix xk = A.mult(xYaw);
            Pyaw.set(A.mult(Pyaw.mult(A.transpose())).plus(Qyaw));

            if (headPacket == null) {
                stickerYaw = stickerYaw + (float) xYaw.get(1);
            } else {
                float[] upVector = upVector(orientation);
                calculateKinectCorrectionsStep(headPacket.rotMat, upVector);

                float[] sticker = correctKinectMatrix(headPacket.rotMat);
                stickerYaw = extractYaw(sticker);
                //stickerYaw = unroll(extractYaw(sticker), stickerYaw);
                //stickerYaw = unroll(extractYaw(headPacket.rotMat), stickerYaw);
            }

            SimpleMatrix z = new SimpleMatrix(2, 1, false, stickerYaw, dImuYaw);
            SimpleMatrix yk = z.minus(C.mult(xk));
            SimpleMatrix s = C.mult(Pyaw.mult(C.transpose())).plus(Ryaw);
            SimpleMatrix k = Pyaw.mult(C.transpose()).mult(s.invert());

            xYaw.set(xk.plus(k.mult(yk)));
            Pyaw.set(SimpleMatrix.identity(2).minus(k.mult(C)).mult(Pyaw));
        }


//            stickerYaw = unroll(extractYaw(headPacket.rotMat),stickerYaw);
//            Matrix.setIdentityM(rotation,0);
//            Matrix.rotateM(rotation,0,
//                    (float) Math.toDegrees(stickerYaw),0.0f,1.0f,0.0f);


        //Matrix.rotateM(rotation,0,orientation,0,
        //        (float) Math.toDegrees(imuYaw),0.0f,1.0f,0.0f);

//        Matrix.setIdentityM(rotation,0);
//        Matrix.rotateM(rotation,0,
//                (float) Math.toDegrees(xYaw.get(0)),0.0f,1.0f,0.0f);


        if(jointPacket != null){
            float[] lhPos = jointPacket.getJoint(Joint.JointType.HandLeft).getCoordArray();
            float[] lwPos = jointPacket.getJoint(Joint.JointType.WristLeft).getCoordArray();

            float[] rhPos = jointPacket.getJoint(Joint.JointType.HandRight).getCoordArray();
            float[] rwPos = jointPacket.getJoint(Joint.JointType.WristRight).getCoordArray();

            Matrix.setIdentityM(leftHand,0);
            Matrix.translateM(leftHand,0,lhPos[0],lhPos[1]-KINETEC_HEIGHT,lhPos[2]);

            Matrix.setIdentityM(rigthHand,0);
            Matrix.translateM(rigthHand,0,rhPos[0],rhPos[1]-KINETEC_HEIGHT,rhPos[2]);

            float[] lhVec = new float[3];
            float[] rhVec = new float[3];

            for(int i = 0; i < 3; ++i){
                lhVec[i] = lhPos[i] - lwPos[i];
                rhVec[i] = lwPos[i] - rwPos[i];
            }


            rotateTo(leftHand,lhVec);
            rotateTo(rigthHand,rhVec);

        }

//        if(jointPacket != null){
//            Joint head = jointPacket.getJoint(Joint.JointType.Head);
//            float[] skellPos = correctKinectVector(kinectCorrectionData,
//                    new float[]{head.X,head.Y,head.Z,1.0f});
//
//
//            for(int i = 0; i < 3; ++i){
//                SimpleMatrix xkp = A.mult(xPos[i]);
//                PPos[i].set(A.mult(PPos[i].mult(A.transpose())).plus(Qlin));
//
//                if(headPacket == null){
//
//                    SimpleMatrix subC = C.extractVector(true,1);
//                    double subR = Rlin.get(1,1);
//
//                    double yk = skellPos[i] - subC.mult(xkp).get(0);
//                    double s = subC.mult(PPos[i].mult(subC.transpose())).get(0) + subR;
//                    SimpleMatrix K = PPos[i].mult(subC.transpose()).divide(s);
//
//                    xPos[i].set(xkp.plus(K.scale(yk)));
//                    PPos[i].set(SimpleMatrix.identity(3).minus(K.mult(subC)).mult(PPos[i]));
//
//                }else{
//
//                    float[] stickerPos = correctKinectVector(kinectCorrectionData,
//                            new float[]{headPacket.X,headPacket.Y,headPacket.Z,1.0f});
//
//
//                    SimpleMatrix z = new SimpleMatrix(2,1,false, stickerPos[i],skellPos[i]);
//                    SimpleMatrix yk = z.minus(C.mult(xkp));
//                    SimpleMatrix s = C.mult(PPos[i].mult(C.transpose())).plus(Rlin);
//                    SimpleMatrix k = PPos[i].mult(C.transpose()).mult(s.invert());
//
//                    xPos[i].set(xkp.plus(k.mult(yk)));
//                    PPos[i].set(SimpleMatrix.identity(3).minus(k.mult(C)).mult(PPos[i]));
//
//                    //pos[i] = (float) xPos[i].get(0);
//
//                }
//
//
//            }
//
//
//
//        }


    }

    public void startKalman(float[] orientation, HeadPacket headPacket, JointPacket jointPacket){
        frameTime = SystemClock.currentThreadTimeMillis();

        float[] upVector = upVector(orientation);
        calculateKinectCorrectionsStep(headPacket.rotMat, upVector);

        float[] sticker = correctKinectMatrix(headPacket.rotMat);

        xYaw = new SimpleMatrix(2,1);
        imuYaw = -extractYaw(orientation);
        stickerYaw = extractYaw(sticker);
        xYaw.set(0, stickerYaw);

        Pyaw = new SimpleMatrix(2,2);
        Pyaw.set(0,0,3.1416);
        Pyaw.set(1,1,0.01);

        Joint head = jointPacket.getJoint(Joint.JointType.Head);

        float[] stickerPos = correctKinectVector(
                new float[]{headPacket.X,headPacket.Y,headPacket.Z,1.0f});

        float[] skellPos = correctKinectVector(
                new float[]{head.X,head.Y,head.Z,1.0f});

        xPos = new SimpleMatrix[3];
        PPos = new SimpleMatrix[3];
        for(int i = 0; i < 3; ++i){
            xPos[i] = new SimpleMatrix(3,1);
            xPos[i].set(0,stickerPos[i]);
            xPos[i].set(2,stickerPos[i] - skellPos[i]);

            PPos[i] = new SimpleMatrix(3,3);
            PPos[i].set(0,0,0.0157);
            PPos[i].set(1,1,3.1416);
            PPos[i].set(2,2,0.1571);
        }
    }


    public float[] getHeadTransform(){

        float[] camera = new float[16];
        Matrix.setIdentityM(translation,0);
        Matrix.translateM(translation,0,pos[0],pos[1]-KINETEC_HEIGHT,pos[2]);

        Matrix.multiplyMM(camera,0, rotation,0,translation,0);

        return camera;
    }

    public float[] getLeftHand(){
        return leftHand;
    }

    public float[] getRigthHand(){
        return rigthHand;
    }

    private float unroll(float v,float roll) {
//        float vmod = roll % (float)(2*Math.PI);
//        if (roll < 0){
//            vmod += 2*Math.PI;
//        }
//        if (vmod > Math.PI) {
//            vmod -= 2*Math.PI;
//        }

        //float diff = vmod - v;
        float diff = (v - roll) % (float)(2*Math.PI);

        if (diff < 0) {
            diff += 2*Math.PI;
        }
        if (diff > Math.PI) {
            diff -= 2*Math.PI;
        }

        roll += diff;

//        if(Math.abs(diff) < Math.PI){
//            roll -= diff;
//        }else{
//            if(diff > 0){
//                roll += (float)(diff - Math.PI);
//            }else{
//                roll += (float)(diff + Math.PI);
//            }
//        }

        return roll;

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

    private float[] crossProduct(float[] a, float[] b){
        return new float[]{
                a[1]*b[2] - a[2]*b[1],
                a[0]*b[2] - a[2]*b[0],
                a[0]*b[1] - a[1]*b[0]
        };
    }

    private float dotProduct(float[] a, float[] b){
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    private float[] rotateTo(float[] matrix, float[] target){

        float mag = 0;
        for(int i = 0; i < 3; ++i){
            mag += target[i];
        }

        float angle = (float)Math.toDegrees(Math.acos(target[2]/mag));
        Matrix.rotateM(matrix,0,angle, target[2],0.0f, -target[0]);

        return matrix;

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

    private static class KinectCorrectionData {
        float roll = 0;
        float pitch = 0;
        int averages = 0;
        float[] correction_premultiplier_matrix = new float[16];
    }

    private void calculateKinectCorrectionsStep(float[] kinect_space_transform, float[] up_vector){
        // cumulative averaging
        float f = 1.f / kinectCorrectionData.averages++;
        float[] up_vector_kinect = new float[4];
        Matrix.multiplyMV(up_vector_kinect, 0, kinect_space_transform, 0, up_vector, 0);
        kinectCorrectionData.roll = (float)Math.atan2(up_vector_kinect[0], up_vector_kinect[1])*f + kinectCorrectionData.roll*(1.f - f);
        float[] kinect_roll_corrector = new float[16];
        Matrix.setRotateM(kinect_roll_corrector, 0, (float)Math.toDegrees(kinectCorrectionData.roll), 0, 0, 1);
        float[] up_vector_kinect_corrected = new float[4];
        Matrix.multiplyMV(up_vector_kinect_corrected, 0, kinect_roll_corrector, 0, up_vector_kinect, 0);
        kinectCorrectionData.pitch = (float)Math.atan2(-up_vector_kinect_corrected[2], up_vector_kinect_corrected[1])*f + kinectCorrectionData.pitch*(1.f - f);
        float[] kinect_pitch_corrector = new float[16];
        Matrix.setRotateM(kinect_pitch_corrector, 0, (float)Math.toDegrees(kinectCorrectionData.pitch), 1, 0, 0);
        Matrix.multiplyMM(kinectCorrectionData.correction_premultiplier_matrix, 0, kinect_pitch_corrector, 0, kinect_roll_corrector, 0);
    }

    private float[] correctKinectVector(float[] vector){
        float[] correctedVector = new float[4];
        Matrix.multiplyMV(correctedVector, 0, kinectCorrectionData.correction_premultiplier_matrix, 0, vector, 0);
        return correctedVector;
    }

    private float[] correctKinectMatrix(float[] matrix){
        float[] correctedMatrix = new float[16];
        Matrix.multiplyMM(correctedMatrix, 0, kinectCorrectionData.correction_premultiplier_matrix, 0, matrix, 0);
        return correctedMatrix;
    }



    //        if(headPacket != null){
//            float[] upVector = upVector(orientation);
//            kinectCorrectionData = calculateKinectCorrections(
//                    headPacket.rotMat, upVector);
//
//            float[] sticker = correctKinectMatrix(kinectCorrectionData,headPacket.rotMat);
//            float newYaw = extractYaw(sticker);
//
//
//            float yawmod = (float) ((Math.abs(stickerYaw)+Math.PI)%(2*Math.PI)-Math.PI);
//            if(stickerYaw < 0){
//                yawmod =-yawmod;
//            }
//
//            float diff = yawmod - newYaw;
//
//            if(Math.abs(diff) < Math.PI){
//                stickerYaw -= diff * 0.1f;
//            }else{
//                if(diff > 0){
//                    stickerYaw += (float)(diff - Math.PI)*0.05f;
//                }else{
//                    stickerYaw += (float)(diff + Math.PI)*0.05f;
//                }
//            }
//
//            pos[0] = headPacket.X*-1; // The data is pre-filtered
//            pos[1] = headPacket.Y-1.8f;
//            pos[2] = headPacket.Z;
//            //pos[2] = headPacket.Z+1.5f;
//
//            Matrix.translateM(translation,0,0,-1.2f,0);
//        }
//
//        //Log.i(TAG,"stickerYaw: " + stickerYaw);
//
//        float imu = -extractYaw(orientation);
//
//        offSet = 0.01f*(stickerYaw - imu) + (1-0.01f)*offSet;
//
//
//
//
//        for(int i = 12; i<15; ++i){
//            orientation[i] = 0;
//        }
//
//        //Matrix.rotateM(rotation,0,1.0f,0.0f,1.0f,0.0f);
//        //Matrix.setIdentityM(rotation,0);
//        Matrix.rotateM(rotation,0,orientation,0, (float) Math.toDegrees(offSet),0.0f,1.0f,0.0f);
//        //Matrix.rotateM(rotation,0, (float) Math.toDegrees(offSet + imu),0.0f,1.0f,0.0f);
//
//        //Matrix.rotateM(rotation,0,orientation,0, (float) Math.toDegrees(xYaw.get(0)+xYaw.get(2)-imu),0,1,0);
//
//        if(jointPacket != null){
//            Joint head = jointPacket.getJoint(Joint.JointType.Head);
//
////            pos[0] = (float) (pos[0]*0.5-0.5*head.X);
////            pos[1] = (float) (pos[1]*0.5+0.5*(head.Y -1.8f));
////            pos[2] = (float) (pos[2]*0.5+0.5*head.Z);
//
//            //Matrix.setIdentityM(translation,0);
//            //Matrix.translateM(translation,0,-head.X,-head.Y,-head.Z);
//        }


}
