package com.example.chris.apexvr.filtering;

import android.opengl.Matrix;
import android.os.SystemClock;

import org.ejml.simple.SimpleMatrix;

import io.github.apexhaptics.apexhapticsdisplay.datatypes.HeadPacket;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.Joint;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.JointPacket;

/**
 * Created by Chris on 3/7/2017.
 */

public class ApexSensors {

    private static final String TAG = "Apex_Kalman";

    boolean leftHandAboveGround = false;
    boolean rightHandAboveGround = false;

    float[] translation = new float[16];
    float[] rotation = new float[16];
    float[] leftHand = new float[16];
    float[] rigthHand = new float[16];

    float[] skeletonPos = new float[3];
    float[] stickerPos = new float[3];

    float imuYaw = 0;
    float stickerYaw = 0;

    OneEuro[] leftHandEuro,rigthHandEuro, headPosEuro;

//    private KinectCorrectionData kinectCorrectionData;

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

    private boolean ready = false;

    public ApexSensors(){

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
        Rlin.set(0,0,0.1);
        Rlin.set(1,1,0.3);

        Qlin = new SimpleMatrix(2,2);
        Qlin.set(0,0,0);
        Qlin.set(1,1,1.5);


        leftHandEuro = new OneEuro[3];
        rigthHandEuro = new OneEuro[3];

        leftHandEuro[0] = new OneEuro(15.0f,0.5f,2.5f);
        rigthHandEuro[0] = new OneEuro(15.0f,0.5f,2.5f);

        leftHandEuro[1] = new OneEuro(15.0f,0.5f,2.5f);
        rigthHandEuro[1] = new OneEuro(15.0f,0.5f,2.5f);

        leftHandEuro[2] = new OneEuro(15.0f,0.5f,2.5f);
        rigthHandEuro[2] = new OneEuro(15.0f,0.5f,2.5f);

        headPosEuro = new OneEuro[3];
        headPosEuro[0] = new OneEuro(15.0f,0.2f,3.0f);
        headPosEuro[1] = new OneEuro(15.0f,0.2f,3.0f);
        headPosEuro[2] = new OneEuro(15.0f,0.2f,3.0f);


        Matrix.setIdentityM(rotation,0);
        Matrix.setIdentityM(translation,0);
        Matrix.setIdentityM(leftHand,0);
        Matrix.setIdentityM(rigthHand,0);
        Matrix.translateM(translation,0,0,-1.8f,0);

//        kinectCorrectionData = new KinectCorrectionData();


    }
    float[] pos = new float[3];

    public void step(float[] orientation, HeadPacket headPacket, JointPacket jointPacket) {



        if (!ready) {
            ready = !(headPacket == null || jointPacket == null || headPacket.rotMat == null);

            if (ready) {
                startKalman(orientation, headPacket, jointPacket);
            }

            return;
        }


        //float newImuYaw = unroll(-extractYaw(orientation), (float) xYaw.get(0));
        float dImuYaw = unroll((-extractYaw(orientation)) - imuYaw, 0);
        imuYaw = imuYaw + dImuYaw;

        long time = SystemClock.currentThreadTimeMillis();
        float dt = (time - frameTime) / 1000.0f;
        frameTime = time;

//        Log.i(TAG,"Frame Rate: " + 1/dt);

        {
            SimpleMatrix xk = A.mult(xYaw);
            Pyaw.set(A.mult(Pyaw.mult(A.transpose())).plus(Qyaw));

            if (headPacket == null || headPacket.rotMat == null) {
                stickerYaw = stickerYaw + (float) xYaw.get(1);
            } else {
                float[] upVector = upVector(orientation);

                //Log.i(TAG, Float.toString(extractYaw(headPacket.rotMat)));

                float[] sticker = headPacket.rotMat;


                //stickerYaw = extractYaw(sticker);
                stickerYaw = unroll(extractYaw(sticker), stickerYaw);
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
//                    (float) Math.toDegrees(xYaw.get(0)),0.0f,1.0f,0.0f);


        //Matrix.rotateM(rotation,0,orientation,0,
        //        (float) Math.toDegrees(imuYaw),0.0f,1.0f,0.0f);



//        rotation = orientation;
//        Matrix.rotateM(rotation,0,
//                (float) Math.toDegrees(xYaw.get(0) - imuYaw),0.0f,1.0f,0.0f);

        float[] yawMatrix = yawRotation((float)(xYaw.get(0) + Math.PI) - imuYaw);
        Matrix.multiplyMM(rotation,0,orientation,0,yawMatrix,0);



        if(jointPacket != null){
            Joint headJoint = jointPacket.getJoint(Joint.JointType.Head);
            Joint leftHandJoint = jointPacket.getJoint(Joint.JointType.HandLeft);
            Joint rightHandJoint = jointPacket.getJoint(Joint.JointType.HandRight);

            float[] headPos = new float[]{headJoint.X,headJoint.Y,headJoint.Z,1.0f};
            float[] lhPos = new float[]{leftHandJoint.X,leftHandJoint.Y,leftHandJoint.Z,1.0f};
            float[] rhPos = new float[]{rightHandJoint.X,rightHandJoint.Y,rightHandJoint.Z,1.0f};


            leftHandAboveGround = lhPos[1] > -10.f;
            rightHandAboveGround = rhPos[1] > -10.f;


            float[] lhrp = new float[3];
            float[] rhrp = new float[3];

            for(int i = 0; i < 3; ++i){

                float relative = (float) xPos[i].get(0) - headPos[i];

                lhrp[i] = leftHandEuro[i].filter(lhPos[i]  + relative,dt);
                rhrp[i] = rigthHandEuro[i].filter(rhPos[i] + relative,dt);

//                lhrp[i] = leftHandEuro[i].filter(lhPos[i],dt);
//                rhrp[i] = rigthHandEuro[i].filter(rhPos[i],dt);
            }

            float[] translation = new float[16];

            Matrix.setIdentityM(translation,0);
            Matrix.translateM(translation,0,lhrp[0],lhrp[1],lhrp[2]);
            Matrix.multiplyMM(leftHand,0,translation,0,yawRotation(
                    (float)(Math.PI + Math.atan2(lhPos[0]-headPos[0],lhPos[2]-headPos[2]))),0);
            Matrix.translateM(translation,0,0.0f,0.0f,-0.06f);

            Matrix.setIdentityM(translation,0);
            Matrix.translateM(translation,0,rhrp[0],rhrp[1],rhrp[2]);
            Matrix.multiplyMM(rigthHand,0,translation,0,yawRotation(0.0f),0);
            Matrix.multiplyMM(rigthHand,0,translation,0,yawRotation(
                    (float)(Math.PI + Math.atan2(rhPos[0]-headPos[0],rhPos[2]-headPos[2]))),0);
            Matrix.translateM(translation,0,0.0f,0.0f,-0.06f);

//            Log.i(TAG,Arrays.toString(lhPos));
//
//            Matrix.setIdentityM(leftHand,0);
//            Matrix.translateM(leftHand,0,lhPos[0],lhPos[1],lhPos[2]);
//            Matrix.multiplyMM(leftHand,0,translation,0,yawRotation(
//                    (float)(Math.PI + Math.atan2(lhPos[0]-headPos[0],lhPos[2]-headPos[2]))),0);

//            Matrix.setIdentityM(rigthHand,0);
//            Matrix.translateM(rigthHand,0,rhPos[0],rhPos[1],rhPos[2]);
//            Matrix.multiplyMM(rigthHand,0,translation,0,yawRotation(
//                    (float)(Math.PI + Math.atan2(rhPos[0]-headPos[0],rhPos[2]-headPos[2]))),0);

//            rotateTo(leftHand,lhVec);
//            rotateTo(rigthHand,rhVec);

        }

        if (headPacket != null){
            stickerPos = new float[]{headPacket.X,headPacket.Y,headPacket.Z,1.0f};
            //Log.i(TAG, Arrays.toString(stickerPos));
        }

        if(jointPacket != null){
            Joint head = jointPacket.getJoint(Joint.JointType.Head);

            float[] newSkeletonPos = new float[]{head.X,head.Y,head.Z,1.0f};
            float[] dSkeletonPos = new float[3];
            for(int i = 0; i < 3; ++i){
                dSkeletonPos[i] = (newSkeletonPos[i] - skeletonPos[i]);
            }
            skeletonPos = newSkeletonPos;

            if(headPacket == null){
                for(int i = 0; i < 3; ++i){
                    stickerPos[i] += dSkeletonPos[i];
                }
            } else {
                stickerPos = new float[]{headPacket.X,headPacket.Y,headPacket.Z,1.0f};
            }

            for(int i = 0; i < 3; ++i){
                SimpleMatrix xkp = A.mult(xPos[i]);
                PPos[i].set(A.mult(PPos[i].mult(A.transpose())).plus(Qlin));

                SimpleMatrix z = new SimpleMatrix(2,1,false, stickerPos[i], dSkeletonPos[i]);
                SimpleMatrix yk = z.minus(C.mult(xkp));
                SimpleMatrix s = C.mult(PPos[i].mult(C.transpose())).plus(Rlin);
                SimpleMatrix k = PPos[i].mult(C.transpose()).mult(s.invert());

                xPos[i].set(xkp.plus(k.mult(yk)));
                PPos[i].set(SimpleMatrix.identity(2).minus(k.mult(C)).mult(PPos[i]));

                pos[i] = headPosEuro[i].filter((float) xPos[i].get(0),dt);
            }
        }
    }

    public void startKalman(float[] orientation, HeadPacket headPacket, JointPacket jointPacket){
        frameTime = SystemClock.currentThreadTimeMillis();

        float[] sticker = headPacket.rotMat;

        xYaw = new SimpleMatrix(2,1);
        imuYaw = -extractYaw(orientation);
        stickerYaw = extractYaw(sticker);
        xYaw.set(0, stickerYaw);

        Pyaw = new SimpleMatrix(2,2);
        Pyaw.set(0,0,3.1416);
        Pyaw.set(1,1,0.01);

        Joint head = jointPacket.getJoint(Joint.JointType.Head);

        stickerPos = new float[]{headPacket.X,headPacket.Y,headPacket.Z,1.0f};

        skeletonPos = new float[]{head.X,head.Y,head.Z,1.0f};

        xPos = new SimpleMatrix[3];
        PPos = new SimpleMatrix[3];
        for(int i = 0; i < 3; ++i){
            xPos[i] = new SimpleMatrix(2,1);
            xPos[i].set(0,stickerPos[i]);

            PPos[i] = new SimpleMatrix(2,2);
            PPos[i].set(0,0,1);
            PPos[i].set(1,1,1);
        }
    }


    public float[] getHeadTransform(){

        float[] camera = new float[16];
        Matrix.setIdentityM(translation,0);
        Matrix.translateM(translation,0,-pos[0],-pos[1],-pos[2]);

        Matrix.multiplyMM(camera,0, rotation,0,translation,0);

        return camera;
    }

    public boolean isLeftHandAboveGround() {
        return leftHandAboveGround;
    }

    public boolean isRightHandAboveGround() {
        return rightHandAboveGround;
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

    private float[] yawRotation(float yaw){

        float cyaw = (float) Math.cos(yaw);
        float syaw = (float) Math.sin(yaw);

       return new float[]{
               cyaw,0,-syaw,0,
               0,1,0,0,
               syaw,0,cyaw,0,
               0,0,0,1
       };
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

//    private static class KinectCorrectionData {
//        float roll = 0;
//        float pitch = 0;
//        int averages = 0;
//        float[] correction_premultiplier_matrix = new float[16];
//    }
//
//    private void calculateKinectCorrectionsStep(float[] kinect_space_transform, float[] up_vector){
//        // cumulative averaging
//        kinectCorrectionData.averages = Math.min(kinectCorrectionData.averages+1, 100);
//        float f = 1.f / kinectCorrectionData.averages;
//        float[] up_vector_kinect = new float[4];
//        Matrix.multiplyMV(up_vector_kinect, 0, kinect_space_transform, 0, up_vector, 0);
//        //kinectCorrectionData.roll = (float)Math.atan2(up_vector_kinect[0], up_vector_kinect[1])*f + kinectCorrectionData.roll*(1.f - f);
//        kinectCorrectionData.roll = (float)Math.PI;
//        float[] kinect_roll_corrector = new float[16];
//        Matrix.setRotateM(kinect_roll_corrector, 0, (float)Math.toDegrees(kinectCorrectionData.roll), 0, 0, 1);
//        float[] up_vector_kinect_corrected = new float[4];
//        Matrix.multiplyMV(up_vector_kinect_corrected, 0, kinect_roll_corrector, 0, up_vector_kinect, 0);
//        kinectCorrectionData.pitch = (float)Math.atan2(-up_vector_kinect_corrected[2], up_vector_kinect_corrected[1])*f + kinectCorrectionData.pitch*(1.f - f);
//        kinectCorrectionData.pitch = 0;
//        float[] kinect_pitch_corrector = new float[16];
//        Matrix.setRotateM(kinect_pitch_corrector, 0, (float)Math.toDegrees(kinectCorrectionData.pitch), 1, 0, 0);
//        Matrix.multiplyMM(kinectCorrectionData.correction_premultiplier_matrix, 0, kinect_pitch_corrector, 0, kinect_roll_corrector, 0);
//    }
//
//    private float[] correctKinectVector(float[] vector){
//        float[] correctedVector = new float[4];
//        Matrix.multiplyMV(correctedVector, 0, kinectCorrectionData.correction_premultiplier_matrix, 0, vector, 0);
//        return correctedVector;
//    }
//
//    private float[] correctKinectMatrix(float[] matrix){
//        float[] correctedMatrix = new float[16];
//        Matrix.multiplyMM(correctedMatrix, 0, kinectCorrectionData.correction_premultiplier_matrix, 0, matrix, 0);
//        return correctedMatrix;
//    }



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
