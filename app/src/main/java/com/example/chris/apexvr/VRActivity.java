package com.example.chris.apexvr;

import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;

import com.example.chris.apexvr.apexGL.GLError;
import com.example.chris.apexvr.kalman.HeadKalman;
import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.egl.EGLConfig;

import io.github.apexhaptics.apexhapticsdisplay.BluetoothService;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.EndEffectorMarkerPacket;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.HeadPacket;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.Joint;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.JointPacket;
import io.github.apexhaptics.apexhapticsdisplay.datatypes.RobotKinPosPacket;


public class VRActivity extends GvrActivity implements GvrView.StereoRenderer{

    private static final String TAG = "ApexVR_VRA";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 130.0f;

    private GvrAudioEngine gvrAudioEngine;
    private ApexGraphics graphics;

    private BluetoothService myBluetoothService;
    private ConcurrentLinkedQueue<HeadPacket> headQueue;
    private ConcurrentLinkedQueue<JointPacket> jointQueue;
    private ConcurrentLinkedQueue<EndEffectorMarkerPacket> endEffectorMarkerQueue;
    private ConcurrentLinkedQueue<RobotKinPosPacket> robotKinPosQueue;

    private HeadKalman headKalman;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vr);

        Log.i(TAG, "Starting");


        gvrAudioEngine = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
        graphics = new ApexGraphics();

        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
        gvrView.setRenderer(this);


        //gvrView.setTransitionViewEnabled(true);
        // Associate the gvrView with this activity.
        setGvrView(gvrView);


        headKalman = new HeadKalman();


        Log.i(TAG, "Ready");

        headQueue = new ConcurrentLinkedQueue<>();
        jointQueue = new ConcurrentLinkedQueue<>();
        endEffectorMarkerQueue = new ConcurrentLinkedQueue<>();
        robotKinPosQueue = new ConcurrentLinkedQueue<>();

        // Initialize Bluetooth
        myBluetoothService = new BluetoothService(this.getApplicationContext());
        myBluetoothService.registerPacketQueue(HeadPacket.packetString,headQueue);
        myBluetoothService.registerPacketQueue(JointPacket.packetString,jointQueue);
        myBluetoothService.registerPacketQueue(EndEffectorMarkerPacket.packetString,endEffectorMarkerQueue);
        myBluetoothService.registerPacketQueue(RobotKinPosPacket.packetString,robotKinPosQueue);
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {

        Log.i(TAG, "Creating Surface");

        graphics.loadAssets(getAssets());

        GLError.checkGLError(TAG,"Surface created");


        Log.i(TAG, "Creating Surface Done");

    }



    @Override
    public void onNewFrame(HeadTransform headTransform) {

        float[] quaternion = new float[16];

        headTransform.getQuaternion(quaternion,0);
        headKalman.updateIMU(quaternion);

        if(!headQueue.isEmpty()){
            headKalman.updateSticker(headQueue.poll());
        }

        if(!jointQueue.isEmpty()){
            headKalman.updateJoint(jointQueue.poll().getJoint(Joint.JointType.Head));
        }

        if(!endEffectorMarkerQueue.isEmpty()){
            headKalman.updateEndEffectorFromMarker(endEffectorMarkerQueue.poll());
        }

        if(!robotKinPosQueue.isEmpty()){
            headKalman.updateRobotKinPos(robotKinPosQueue.poll());
        }
    }

    @Override
    public void onDrawEye(Eye eye) {


        float[] view = new float[16];
        float[] eyeTran = new float[16];

        float[] camera = headKalman.getHeadTransform();
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

        Matrix.setIdentityM(eyeTran,0);
        Matrix.translateM(eyeTran,0,(eye.getType() == Eye.Type.LEFT)?-0.03f:0.03f,0.0f,0.0f);
        Matrix.multiplyMM(view,0,eyeTran,0,camera,0);


        graphics.drawEye(perspective,view);

        GLError.checkGLError(TAG,"Drawing cube");


    }


    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {
        Log.i(TAG,"Surface changed to " + i + " by " + i1);
    }


    @Override
    public void onPause(){
        gvrAudioEngine.pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        gvrAudioEngine.resume();
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG,"Render shutting down...");

    }

}
