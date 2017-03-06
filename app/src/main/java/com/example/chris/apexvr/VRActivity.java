package com.example.chris.apexvr;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;

import com.example.chris.apexvr.apexGL.GLError;
import com.example.chris.apexvr.apexGL.mesh.ColouredInterleavedMesh;
import com.example.chris.apexvr.apexGL.mesh.MatLib;
import com.example.chris.apexvr.apexGL.mesh.VertexMesh;
import com.example.chris.apexvr.apexGL.object.ColouredStaticObject;
import com.example.chris.apexvr.apexGL.object.GLObject;
import com.example.chris.apexvr.apexGL.object.MultiCSObject;
import com.example.chris.apexvr.apexGL.object.VertexObject;
import com.example.chris.apexvr.apexGL.shader.GLProgram;
import com.example.chris.apexvr.apexGL.shader.Shader;
import com.example.chris.apexvr.apexGL.shader.Shadow;
import com.example.chris.apexvr.apexGL.world.GroundCreater;
import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;


public class VRActivity extends GvrActivity implements GvrView.StereoRenderer{

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 130.0f;

    private static final String TAG = "ApexVR_VRA";

    private static final float[] LIGHT_DIR_IN_WORLD_SPACE = new float[] {0.0f, 0.70710678118f, 0.70710678118f};

    private static final float RAD_TO_DEG = (float) (180.0f / Math.PI);

    private GvrAudioEngine gvrAudioEngine;

    private float[] headPos;
    private float[] headRotation;



    Shadow shadows;


    private List<GLObject> glObjects = new ArrayList<>(10);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vr);

        Log.i(TAG, "Starting");

        headPos = new float[3];
        headRotation = new float[4];

        headPos[1] = 1.8f;


        gvrAudioEngine = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);

        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
        // Associate a GvrView.StereoRenderer with gvrView.
        gvrView.setRenderer(this);


        //gvrView.setTransitionViewEnabled(true);
        // Associate the gvrView with this activity.
        setGvrView(gvrView);


        Log.i(TAG, "Ready");
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {

        Log.i(TAG, "Creating Surface");



        GLProgram texProgram,colProgram,skyProgram,shadowProgram;

        //Textured shader
        try(Shader vertexShader = Shader.loadShader(getAssets().open("shaders/textured.vert"),GLES30.GL_VERTEX_SHADER)){
            try( Shader fragmentShader  = Shader.loadShader(getAssets().open("shaders/textured.frag"),GLES30.GL_FRAGMENT_SHADER)){
                texProgram = new GLProgram();
                texProgram.attachShader(vertexShader,fragmentShader);
            }
        } catch (IOException e) {
            Log.e(TAG,"Could not load shader: " + e.toString());
            throw new RuntimeException("Could not load shader: " + e.toString());
        }

        //Coloured shader
        try(Shader vertexShader = Shader.loadShader(getAssets().open("shaders/coloured.vert"),GLES30.GL_VERTEX_SHADER)){
            try( Shader fragmentShader  = Shader.loadShader(getAssets().open("shaders/coloured.frag"),GLES30.GL_FRAGMENT_SHADER)){
                colProgram = new GLProgram();
                colProgram.attachShader(vertexShader,fragmentShader);
            }
         } catch (IOException e) {
            Log.e(TAG,"Could not load shader: " + e.toString());
            throw new RuntimeException("Could not load shader: " + e.toString());
        }

        //sky shader
        try(Shader vertexShader = Shader.loadShader(getAssets().open("shaders/sky.vert"),GLES30.GL_VERTEX_SHADER)){
            try( Shader fragmentShader  = Shader.loadShader(getAssets().open("shaders/sky.frag"),GLES30.GL_FRAGMENT_SHADER)){
                skyProgram = new GLProgram();
                skyProgram.attachShader(vertexShader,fragmentShader);
            }
        } catch (IOException e) {
            Log.e(TAG,"Could not load shader: " + e.toString());
            throw new RuntimeException("Could not load shader: " + e.toString());
        }

        //shadow shader
        try(Shader vertexShader = Shader.loadShader(getAssets().open("shaders/shadow.vert"),GLES30.GL_VERTEX_SHADER)){
            try( Shader fragmentShader  = Shader.loadShader(getAssets().open("shaders/shadow.frag"),GLES30.GL_FRAGMENT_SHADER)){
                shadowProgram = new GLProgram();
                shadowProgram.attachShader(vertexShader,fragmentShader);
            }
        } catch (IOException e) {
            Log.e(TAG,"Could not load shader: " + e.toString());
            throw new RuntimeException("Could not load shader: " + e.toString());
        }

        //Materials
        MatLib matLib = new MatLib();
        try {
            matLib.addMatLib(getAssets().open("matlibs/tree.mtl"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open matlib file: matlibs/tree.mtl");
        }
        try {
            matLib.addMatLib(getAssets().open("matlibs/ground.mtl"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open matlib file: matlibs/graound.mtl");
        }
        try {
            matLib.addMatLib(getAssets().open("matlibs/grass.mtl"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open matlib file: matlibs/graound.mtl");
        }
        try {
            matLib.addMatLib(getAssets().open("matlibs/table.mtl"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open matlib file: matlibs/graound.mtl");
        }
        try {
            matLib.addMatLib(getAssets().open("matlibs/pillar.mtl"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open matlib file: matlibs/graound.mtl");
        }


        shadows = new Shadow(shadowProgram,
                LIGHT_DIR_IN_WORLD_SPACE,
                new Shadow.BoundingBox(new float[]{0.0f,0.0f,0.0f},new float[]{120.0f,120.0f,20.0f}));


        GroundCreater groundCreater = new GroundCreater(240.0f,200);


        groundCreater.perturb(5.0f,0.5f,6,6);

        float groudAtZero = groundCreater.maxHight(1.5f) + 0.5f;

        ColouredStaticObject ground = new ColouredStaticObject(colProgram,groundCreater.getMesh());
        Matrix.translateM(ground.getOrientation(),0,0.0f,-groudAtZero,0.0f);
        ground.setCastingShadow(true);
        ground.addExtention(shadows);
        glObjects.add(ground);


        try {
            ColouredInterleavedMesh mesh = ColouredInterleavedMesh.importOBJInterleavedMesh(getAssets().open("meshes/pillar.obj"),matLib);
            ColouredStaticObject platform = new ColouredStaticObject(colProgram,mesh);
            platform.setCastingShadow(true);
            platform.addExtention(shadows);
            glObjects.add(platform);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open obj file: meshes/pillar.obj");
        }

        try {
            ColouredInterleavedMesh mesh = ColouredInterleavedMesh.importOBJInterleavedMesh(getAssets().open("meshes/table.obj"),matLib);
            ColouredStaticObject table = new ColouredStaticObject(colProgram,mesh);
            Matrix.translateM(table.getOrientation(),0,1.0f,0.0f,0.0f);
            table.setCastingShadow(true);
            table.addExtention(shadows);
            glObjects.add(table);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open obj file: meshes/table.obj");
        }

        try {
            VertexMesh mesh = VertexMesh.importOBJInterleavedMesh(getAssets().open("meshes/sky.obj"));
            VertexObject sky = new VertexObject(skyProgram,mesh);
            Matrix.translateM(sky.getOrientation(),0,0.0f,-groudAtZero,0.0f);
            Matrix.scaleM(sky.getOrientation(),0,2,2,2);
            glObjects.add(sky);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open obj file: meshes/sky.obj");
        }


        try {
            ColouredInterleavedMesh colouredMesh = ColouredInterleavedMesh.importOBJInterleavedMesh(getAssets().open("meshes/tree.obj"),matLib);
            MultiCSObject tree = new MultiCSObject(colProgram,colouredMesh);
            Matrix.translateM(tree.getOrientation(),0,0.0f,-groudAtZero,0.0f);
            tree.setCastingShadow(true);
            tree.addExtention(shadows);

            float[] sub = new float[16];
            float[] eye = new float[16];
            Random random = new Random();

            Matrix.setIdentityM(eye,0);


            for(float i = -110; i < 110; i += 6){
                for(float j = -110; j < 110; j += 6){
                    float dist2 = i*i + j*j;
                    if(dist2 > 100.0f && dist2 < 12100.0f && random.nextFloat() > 0.4f + 0.4f * dist2 / 12100.0f){

                        float xPos = i-1.6f+3.2f*random.nextFloat();
                        float zPos = j-1.6f+3.2f*random.nextFloat();
                        float yPos = groundCreater.interpolate(xPos,zPos);

                        float[] normal = groundCreater.normal(xPos,zPos);
                        float normalAngle = (float) ((float) Math.acos(normal[1])/Math.PI*180.0f);

                        if(normalAngle > 30)
                            continue;

                        Matrix.translateM(sub,0,eye,0,xPos,yPos,zPos);

                        float rotation = random.nextFloat() * 360.0f;
                        Matrix.rotateM(sub,0,rotation,0,1.0f,0);


                        float xzScale = random.nextFloat()*0.5f + 0.75f;
                        float yScale = random.nextFloat()*0.5f + 0.75f;
                        Matrix.scaleM(sub,0,xzScale,yScale,xzScale);


                        tree.addSubObject(sub.clone());
                    }
                }
            }

            glObjects.add(tree);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open obj file: meshes/ground.obj");
        }


        try {
            ColouredInterleavedMesh colouredMesh = ColouredInterleavedMesh.importOBJInterleavedMesh(getAssets().open("meshes/grass.obj"),matLib);
            MultiCSObject grass = new MultiCSObject(colProgram,colouredMesh);
            Matrix.translateM(grass.getOrientation(),0,0.0f,-groudAtZero,0.0f);
            grass.setCastingShadow(true);
            grass.addExtention(shadows);

            float[] sub = new float[16];
            float[] eye = new float[16];
            Random random = new Random();

            Matrix.setIdentityM(eye,0);

            for(int i = 0; i < 75; ++i){
                float r = 2.0f + 20.0f * random.nextFloat();
                float angle = (float) (2.0 * Math.PI * random.nextFloat());

                float xPos = (float) (Math.cos(angle) * r);
                float zPos = (float) (Math.sin(angle) * r);
                float yPos = groundCreater.interpolate(xPos,zPos) + 0.05f;
                Matrix.translateM(sub,0,eye,0,xPos,yPos,zPos);

                float[] normal = groundCreater.normal(xPos,zPos);
                float normalAngle = (float) ((float) Math.acos(normal[1])/Math.PI*180.0f);

                if(normalAngle > 60)
                    continue;

                if(normalAngle > 0.001)
                    Matrix.rotateM(sub,0,normalAngle ,normal[2],0.0f,-normal[0]);

                float rotation = random.nextFloat() * 360.0f;
                Matrix.rotateM(sub,0,rotation,0,1.0f,0);

                float xzScale = random.nextFloat()*0.5f + 0.75f;
                float yScale = random.nextFloat()*0.5f + 0.75f;
                Matrix.scaleM(sub,0,xzScale,yScale,xzScale);

                grass.addSubObject(sub.clone());

            }


            glObjects.add(grass);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open obj file: meshes/ground.obj");
        }



        shadows.createStaticShadowMap(glObjects);

        //create static shadows
        //shadows.createStaticShadowMap(glObjects);

/*
        //Texture titling
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_MIRRORED_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_MIRRORED_REPEAT);

        //texture scale filtering
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR);

        //texture mipmap
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR);
                */

        GLES30.glCullFace(GLES30.GL_FRONT);

        //Matrix.setLookAtM(mCamera, 0, 0.0f, 1.8034f, 0.0f, 1.8034f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f);


        GLError.checkGLError(TAG,"Surface created");


        Log.i(TAG, "Creating Surface Done");

        Matrix.setIdentityM(viewRot,0);

    }

    float[] viewRot = new float[16];



    @Override
    public void onNewFrame(HeadTransform headTransform) {

        headTransform.getHeadView(viewRot,0);

        for(int i = 12; i < 15; ++i){
            viewRot[i] = 0.0f;
        }



    }

    @Override
    public void onDrawEye(Eye eye) {



        //shadows.createStaticShadowMap(glObjects);

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LESS);
        //GLES30.glClearColor(0.779f, 0.883f, 0.346f, 1.0f);
        //GLES30.glClearColor(0.8f, 0.93f, 1.0f, 1.0f);

        GLES30.glClearColor(0.6172f, 0.0f, 0.9453f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        GLError.checkGLError(TAG,"colorParam");

        float[] view = new float[16];
        float[] viewTran = new float[16];
        float[] eyeTran = new float[16];


        float[] rh = new float[16];

        Matrix.setIdentityM(viewTran,0);
        Matrix.translateM(viewTran,0,-headPos[0],-headPos[1],-headPos[2]);

        Matrix.setIdentityM(eyeTran,0);
        Matrix.translateM(eyeTran,0,(eye.getType() == Eye.Type.LEFT)?-0.03f:0.03f,0.0f,0.0f);

        Matrix.multiplyMM(rh,0,viewRot,0,viewTran,0);
        Matrix.multiplyMM(view,0,eyeTran,0,rh,0);


        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

        GLES30.glCullFace(GLES30.GL_BACK);

        for(GLObject glObject : glObjects){
            glObject.draw(perspective,view);
        }

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
