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
import com.example.chris.apexvr.apexGL.shader.Texture;
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
    private static final float Z_FAR = 100.0f;

    private static final String TAG = "ApexVR_VRA";

    private static final float[] LIGHT_DIR_IN_WORLD_SPACE = new float[] {0.0f, 0.70710678118f, 0.70710678118f};

    private GvrAudioEngine gvrAudioEngine;

    private float[] mCamera = new float[16];

    Shadow shadows;


    private List<GLObject> glObjects = new ArrayList<>(10);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vr);

        Log.i(TAG, "Starting");


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
        /*

        //shadows
        Shadow shadows = new Shadow(shadowProgram,
                LIGHT_DIR_IN_WORLD_SPACE,
                new Shadow.BoundingBox(new float[]{0.0f,0.0f,0.0f},new float[]{120.0f,120.0f,20.0f}));
                */


        shadows = new Shadow(shadowProgram,
                LIGHT_DIR_IN_WORLD_SPACE,
                new Shadow.BoundingBox(new float[]{0.0f,0.0f,0.0f},new float[]{120.0f,120.0f,20.0f}));

        //Meshes
        try {
            ColouredInterleavedMesh colouredMesh = ColouredInterleavedMesh.importOBJInterleavedMesh(getAssets().open("meshes/ground.obj"),matLib);
            ColouredStaticObject ground = new ColouredStaticObject(colProgram,colouredMesh);
            Matrix.translateM(ground.getOrientation(), 0, 0, -3.0f, 0);
            ground.setCastingShadow(true);
            ground.addExtention(shadows);
            glObjects.add(ground);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open obj file: meshes/ground.obj");
        }

        try {
            VertexMesh mesh = VertexMesh.importOBJInterleavedMesh(getAssets().open("meshes/sky.obj"));
            VertexObject sky = new VertexObject(skyProgram,mesh);
            Matrix.translateM(sky.getOrientation(), 0, 0, -6.0f, 0);
            glObjects.add(sky);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not open obj file: meshes/sky.obj");
        }

        try {
            ColouredInterleavedMesh colouredMesh = ColouredInterleavedMesh.importOBJInterleavedMesh(getAssets().open("meshes/tree.obj"),matLib);
            MultiCSObject tree = new MultiCSObject(colProgram,colouredMesh);
            tree.setCastingShadow(true);
            tree.addExtention(shadows);

            float[] sub = new float[16];
            float[] eye = new float[16];
            Random random = new Random();

            Matrix.setIdentityM(eye,0);


            for(float i = -50; i < 50; i += 4){
                for(float j = -50; j < 50; j += 4){
                    if((i*i + j*j > 100.0f) && random.nextFloat() > 0.7){

                        float rotation = (float) Math.sin(random.nextDouble() * Math.PI / 2);
                        Matrix.rotateM(sub,0,eye,0,rotation,0,1-rotation,0);

                        float xzScale = random.nextFloat()*0.5f + 0.75f;
                        float yScale = random.nextFloat()*0.5f + 0.75f;
                        Matrix.scaleM(sub,0,xzScale,yScale,xzScale);


                        Matrix.translateM(sub,0,
                                i-2.0f+4*random.nextFloat(),
                                -2.6f,
                                j-2.0f+4*random.nextFloat());



                        tree.addSubObject(sub.clone());
                    }
                }
            }

            glObjects.add(tree);
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


        GLError.checkGLError(TAG,"Surface created");


        Log.i(TAG, "Creating Surface Done");

    }
    /*
    public void testObjects(){
        try {
            Texture texture = Texture.loadTexture(getAssets().openFd("tex/dot_cube.png"));
            TexturedStaticObject cube;
            TexturedInterleavedMesh mesh = TexturedInterleavedMesh.importOBJInterleavedMesh(getAssets().open("meshes/cube.obj"));
            cube = new TexturedStaticObject(texProgram,mesh,texture);
            Matrix.translateM(cube.getOrientation(), 0, 0, -5.0f,0);
            glObjects.add(cube);
        } catch (IOException e) {
            Log.e(TAG,"Could not open obj file: " + e.toString());
            throw new RuntimeException("Could not open obj file: " + e.toString());
        }

        try {
            Texture texture = Texture.loadTexture(getAssets().openFd("tex/poke_ball.png"));
            TexturedStaticObject pBall;
            TexturedInterleavedMesh mesh = TexturedInterleavedMesh.importOBJInterleavedMesh(getAssets().open("meshes/cyl.obj"));
            pBall = new TexturedStaticObject(texProgram, mesh, texture);
            Matrix.translateM(pBall.getOrientation(), 0, 0, 0, -5.0f);
            glObjects.add(pBall);
        } catch (IOException e) {
            Log.e(TAG, "Could not open obj file: " + e.toString());
            throw new RuntimeException("Could not open obj file: " + e.toString());
        }
    }
    */



    @Override
    public void onNewFrame(HeadTransform headTransform) {

        //Matrix.rotateM(glObjects.get(0).getOrientation(), 0, 0.3f, 0.5f, 0.5f, 1.0f);
        //Matrix.rotateM(glObjects.get(1).getOrientation(), 0, 0.3f, 0.5f, 0.5f, 1.0f);

        //Matrix.translateM(cube.getOrientation(), 0, 0, -0.1f,0);
        Matrix.setLookAtM(mCamera, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f);
        //shadows.createStaticShadowMap(glObjects);

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

        Matrix.multiplyMM(view, 0,eye.getEyeView(), 0, mCamera, 0);

        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

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
