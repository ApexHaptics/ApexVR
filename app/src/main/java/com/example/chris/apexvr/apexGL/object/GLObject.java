package com.example.chris.apexvr.apexGL.object;

import android.opengl.GLES30;
import android.opengl.Matrix;

import com.example.chris.apexvr.apexGL.shader.GLProgram;
import com.example.chris.apexvr.apexGL.shader.LightingExtention;
import com.example.chris.apexvr.apexGL.shader.Shadow;

import java.security.cert.Extension;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Chris on 2/12/2017.
 */

public abstract class GLObject {
    protected GLProgram program;
    protected float[] orientation;
    protected boolean castingShadow = false;
    List<LightingExtention> extentions;


    public GLObject(GLProgram program){
        this.program = program;

        orientation = new float[16];
        Matrix.setIdentityM(orientation,0);

        extentions = new ArrayList<>(5);
    }

    public float[] getOrientation() {
        return orientation;
    }

    public void draw(float[] p, float[] v){
        program.use();

        float[] vm = new float[16];
        float[] pvm = new float[16];

        Matrix.multiplyMM(vm,0,v,0,orientation,0);
        Matrix.multiplyMM(pvm,0,p,0,vm,0);

        for(LightingExtention extention: extentions){
            extention.bind(p,v,orientation);
        }

        onDraw(pvm,vm,v);

    }

    public void drawShadow(Shadow shadow){
        if(!castingShadow)
            return;

        float[] pvm = new float[16];
        Matrix.multiplyMM(pvm,0,shadow.getPV(),0,orientation,0);
        GLES30.glUniformMatrix4fv(shadow.getPvmUniformID(),1,false,pvm,0);

        onDrawShadow(shadow);
    }

    public void addExtention(LightingExtention extention){
        extention.link(program);
        extentions.add(extention);
    }

    protected abstract void onDraw(float[] pvm, float[] vm, float[] v);
    protected abstract void onDrawShadow(Shadow shadow);

    public boolean isCastingShadow() {
        return castingShadow;
    }

    public void setCastingShadow(boolean castingShadow) {
        this.castingShadow = castingShadow;
    }
}
