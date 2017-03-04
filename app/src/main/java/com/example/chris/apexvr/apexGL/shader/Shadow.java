package com.example.chris.apexvr.apexGL.shader;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.example.chris.apexvr.apexGL.GLError;
import com.example.chris.apexvr.apexGL.object.GLObject;

import java.util.List;

/**
 * Created by Chris on 2/26/2017.
 */

public class Shadow implements LightingExtention {


    private static final String TAG = "Shadows";
    private static final int TEXTURE_DIM = 1024;
    private static final float[] SCREEN_BIAS = new float[]{
            0.5f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.5f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.5f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f};

    private float[] pv;
    private float[] spv;

    private final int[] textures;
    private final int[] buffers;
    private GLProgram program;
    private int vetexAtribID, pvmUniformID,shadowPVMUniformID, texUniformID;


    public Shadow(GLProgram shadowProgram, float[] lightDir, BoundingBox bounding){


       if(!GLES30.glGetString(GLES30.GL_EXTENSIONS).contains("OES_depth_texture")){
           Log.e(TAG,"OES_depth_texture not sported");
           throw new RuntimeException("OES_depth_texture not sported");
       }
        program = shadowProgram;

        pv = new float[16];
        spv = new float[16];

        float[] p = new float[16];
        float[] v = new float[16];
        float[] lowerB = new float[3];
        float[] upperB = new float[3];

        for(int i = 0; i < 3; ++i){
            lowerB[i] = bounding.center[i] - bounding.size[i]/2.0f;
            upperB[i] = bounding.center[i] + bounding.size[i]/2.0f;
        }

        //Matrix.orthoM(p,0,lowerB[0],upperB[0],lowerB[1],upperB[1],lowerB[2],upperB[2]);
        Matrix.orthoM(p,0,-60,60,-60,60,-20,20);
        Matrix.setLookAtM(v,0,lightDir[0],lightDir[1],lightDir[2],0.0f,0.0f,0.0f,lightDir[1],lightDir[2],lightDir[0]);
        Matrix.multiplyMM(pv,0,p,0,v,0);
        Matrix.multiplyMM(spv,0,SCREEN_BIAS,0,pv,0);



        textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);
        GLError.checkGLError(TAG,"texture bind");

        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_DEPTH_COMPONENT32F,
                TEXTURE_DIM,
                TEXTURE_DIM,
                0,
                GLES30.GL_DEPTH_COMPONENT,
                GLES30.GL_FLOAT,
                null);

        /*GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_DEPTH_COMPONENT,
                TEXTURE_DIM,
                TEXTURE_DIM,
                0,
                GLES30.GL_DEPTH_COMPONENT,
                GLES30.GL_UNSIGNED_INT,
                null);*/


        GLError.checkGLError(TAG,"TexImage2D");


        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);
        GLError.checkGLError(TAG,"TexParameteri");



        buffers = new int[1];
        GLES30.glGenFramebuffers(1,buffers,0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,buffers[0]);
        GLError.checkGLError(TAG,"frame bind");

        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
                GLES30.GL_DEPTH_ATTACHMENT,
                GLES30.GL_TEXTURE_2D,
                textures[0],
                0);
        GLError.checkGLError(TAG,"FramebufferTexture2D");

        GLES30.glDrawBuffers(1,new int[]{GLES30.GL_NONE},0);
        //GLES30.glReadBuffer(GLES30.GL_NONE);
        GLError.checkGLError(TAG,"Draw/Read buffer");

       GLError.checkFrameBuffer(TAG,"finished",GLES30.GL_FRAMEBUFFER);

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);





        try {
            vetexAtribID = program.getAtttributeID("position");
        } catch (Exception e) {
            Log.e(TAG,"Could not link buffers: " + e.toString());
            throw new RuntimeException("Could not link buffers: " + e.toString());
        }

        try {
            pvmUniformID = program.getUniformID("PVM");
        } catch (Exception e) {
            Log.e(TAG,"Could not link uniform: " + e.toString());
            throw new RuntimeException("Could not link uniform: " + e.toString());
        }


    }

    public void createStaticShadowMap(List<GLObject> casters){

        program.use();

        int[] viewPort = new int[4];
        int[] boundBuffer = new int[1];

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LESS);

        GLES30.glGetIntegerv(GLES30.GL_VIEWPORT,viewPort,0);
        GLES30.glGetIntegerv(GLES30.GL_DRAW_FRAMEBUFFER_BINDING,boundBuffer,0);

        GLES30.glViewport(0, 0, TEXTURE_DIM, TEXTURE_DIM);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, buffers[0]);

        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT);

        GLES30.glEnableVertexAttribArray(vetexAtribID);
        for(GLObject caster : casters){
            caster.drawShadow(this);
        }
        GLES30.glDisableVertexAttribArray(vetexAtribID);

        GLError.checkGLError(TAG,"draw static shadow drawing");


        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, boundBuffer[0]);
        GLES30.glViewport(viewPort[0], viewPort[1], viewPort[2], viewPort[3]);
    }

    public float[] getPV() {
        return pv;
    }

    public int getVetexAtribID() {
        return vetexAtribID;
    }

    public int getPvmUniformID() {
        return pvmUniformID;
    }

    @Override
    public void link(GLProgram program) {
        try {
            shadowPVMUniformID = program.getUniformID("SPVM");
        } catch (Exception e) {
            Log.e(TAG,"Could not link uniform: " + e.toString());
            throw new RuntimeException("Could not link uniform: " + e.toString());
        }

        try {
            texUniformID = program.getUniformID("depthMap");
        } catch (Exception e) {
            Log.e(TAG,"Could not link uniform: " + e.toString());
            throw new RuntimeException("Could not link uniform: " + e.toString());
        }
    }

    @Override
    public void bind(float[] p, float[] v, float[] m) {
        float[] spvm = new float[16];
        Matrix.multiplyMM(spvm,0,spv,0,m,0);
        GLES30.glUniformMatrix4fv(shadowPVMUniformID, 1,false, spvm,0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);
        GLError.checkGLError(TAG,"shadow tex bind");

        GLES30.glUniform1i(texUniformID,0);

        GLError.checkGLError(TAG,"shadow bind");

    }

    public static class BoundingBox{
        private final float[] center;
        private final float[] size;

        public BoundingBox(float[] center, float[] size){
            this.center = center;
            this.size = size;
        }
    }
}
