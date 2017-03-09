package com.example.chris.apexvr.kalman;

import android.opengl.Matrix;
import android.os.SystemClock;

import org.ejml.simple.SimpleMatrix;

import io.github.apexhaptics.apexhapticsdisplay.datatypes.HeadPacket;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.JointPacket;

/**
 * Created by Chris on 3/7/2017.
 */

public class HeadKalman {

    private static final String TAG = "Apex_Kalman";
    float[] headPos = new float[]{0.0f,1.8f,0.0f};
    float[] orientation = new float[]{0.0f,0.0f,0.0f,1.0f};

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
        Q.set(2,2,0.0314);



    }

    public void step(float[] orientation, HeadPacket headPacket, JointPacket jointPacket){

        if(!ready){

            ready = !(headPacket == null || jointPacket == null);

            if(ready){
                frameTime = SystemClock.currentThreadTimeMillis();

                x = new SimpleMatrix(3,1);
                x.set(0,extractYaw(orientation));

                //TODO:set initial yaw offset

                P = new SimpleMatrix(3,3);
                P.set(0,0,0.0157);
                P.set(1,1,3.1416);
                P.set(2,2,0.1571);

            }

            return;
        }

        long time = SystemClock.currentThreadTimeMillis();
        float dt = 1000*(time - frameTime);
        frameTime = time;
        A.set(0,1,dt);

        x.set(A.mult(x));
        P.set(A.mult(P.mult(A.transpose())).plus(Q));

        if(headPacket == null){

            SimpleMatrix subC = C.extractVector(true,0);
            double subR = R.get(0,0);

            double yk = unrole(extractYaw(orientation)) - subC.mult(x).get(0);
            double s = subC.mult(P.mult(subC.transpose())).get(0);
            SimpleMatrix K = P.mult(subC).divide(s);

            x = x.plus(K.scale(yk));
            P = SimpleMatrix.identity(3).minus(K.mult(subC)).mult(P);

        }else{



        }

    }

    private float unrole(float v) {
        return v;
    }

    public float[] getHeadTransform(){

        float[] camera = quat2Mat(orientation);

        Matrix.translateM(camera,0, -headPos[0], -headPos[1], -headPos[2]);

        return camera;
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
