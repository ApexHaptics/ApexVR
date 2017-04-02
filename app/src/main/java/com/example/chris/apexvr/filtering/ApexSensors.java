package com.example.chris.apexvr.filtering;

import android.opengl.Matrix;
import android.os.SystemClock;

import com.example.chris.apexvr.DataLogger;

import org.ejml.simple.SimpleMatrix;

import java.nio.Buffer;
import java.nio.FloatBuffer;

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

    private boolean ready = false;
    private DataLogger dataLogger;

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

        kinectCorrectionData = new KinectCorrectionData();
        dataLogger = new DataLogger();


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

        long time = SystemClock.elapsedRealtime();
        float dt = (time - frameTime) / 1000.0f;
        frameTime = time;

        dataLogger.logIMU(orientation,time);
        if(headPacket != null){
            if(headPacket.rotMat == null){
                dataLogger.logSticker(new float[]{headPacket.X,headPacket.Y,headPacket.Z},time);
            }else{
                dataLogger.logSticker(headPacket.rotMat,new float[]{headPacket.X,headPacket.Y,headPacket.Z},time);
            }
        }
        if(jointPacket != null){
            Joint head = jointPacket.getJoint(Joint.JointType.Head);
            dataLogger.logSkell(head.getCoordArray(),time);
        }

//        Log.i(TAG,"Frame Rate: " + 1/dt);

        {
            SimpleMatrix xk = A.mult(xYaw);
            Pyaw.set(A.mult(Pyaw.mult(A.transpose())).plus(Qyaw));

            if (headPacket == null || headPacket.rotMat == null) {
                stickerYaw = stickerYaw + (float) xYaw.get(1);
            } else {
//                float[] upVector = upVector(orientation);
//                calculateKinectCorrectionsStep(headPacket.rotMat,upVector);

                //Log.i(TAG, Float.toString(extractYaw(headPacket.rotMat)));

//                float[] sticker = correctKinectMatrix(headPacket.rotMat);
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


            float[] yawMatrix = yawRotation((float)(xYaw.get(0) + Math.PI) - imuYaw);
            Matrix.multiplyMM(rotation,0,orientation,0,yawMatrix,0);
        }

        if (headPacket != null){
//            stickerPos = correctKinectVector(
//                    new float[]{headPacket.X,headPacket.Y,headPacket.Z,1.0f});

            stickerPos = new float[]{headPacket.X,headPacket.Y,headPacket.Z,1.0f};
            //Log.i(TAG, Arrays.toString(stickerPos));
        }

        if(jointPacket != null){
            Joint head = jointPacket.getJoint(Joint.JointType.Head);

//            float[] newSkeletonPos = correctKinectVector(new float[]{head.X,head.Y,head.Z,1.0f});
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
//                stickerPos = correctKinectVector(
//                        new float[]{headPacket.X,headPacket.Y,headPacket.Z,1.0f});
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
        frameTime = SystemClock.elapsedRealtime();

//        float[] upVector = upVector(orientation);
//        calculateKinectCorrectionsStep(headPacket.rotMat,upVector);

        float[] sticker = correctKinectMatrix(headPacket.rotMat);

        xYaw = new SimpleMatrix(2,1);
        imuYaw = -extractYaw(orientation);
        stickerYaw = extractYaw(sticker);
        xYaw.set(0, stickerYaw);

        Pyaw = new SimpleMatrix(2,2);
        Pyaw.set(0,0,3.1416);
        Pyaw.set(1,1,0.01);

        Joint head = jointPacket.getJoint(Joint.JointType.Head);

//        stickerPos = correctKinectVector(new float[]{headPacket.X,headPacket.Y,headPacket.Z,1.0f});
        stickerPos = new float[]{headPacket.X,headPacket.Y,headPacket.Z,1.0f};

//        skeletonPos = correctKinectVector(new float[]{head.X,head.Y,head.Z,1.0f});
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

    private static class KinectCorrectionData {
        float roll = 0;
        float pitch = 0;
        int averages = 0;
        float[] correction_premultiplier_matrix = new float[16];
    }

    private void calculateKinectCorrectionsStep(float[] kinect_space_transform, float[] up_vector){
        // cumulative averaging
        kinectCorrectionData.averages = Math.min(kinectCorrectionData.averages+1, 100);
        float f = 1.f / kinectCorrectionData.averages;
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

    public DataLogger getDataLogger() {
        return dataLogger;
    }
}
