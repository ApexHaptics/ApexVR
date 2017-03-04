package com.example.chris.apexvr.apexGL.object;

import android.opengl.GLES30;
import android.opengl.Matrix;

import com.example.chris.apexvr.apexGL.mesh.ColouredInterleavedMesh;
import com.example.chris.apexvr.apexGL.shader.GLProgram;
import com.example.chris.apexvr.apexGL.shader.LightingExtention;
import com.example.chris.apexvr.apexGL.shader.Shadow;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Chris on 2/23/2017.
 */

public class MultiCSObject extends ColouredStaticObject {
    private List<float[]> subOriens = new ArrayList<>(50);

    public MultiCSObject(GLProgram program, ColouredInterleavedMesh mesh) {
        super(program, mesh);
    }

    public void addSubObject(float[] orientation){
        if(orientation.length < 16){
            throw new RuntimeException("orientation must be 16 length (4x4) column array");
        }

        subOriens.add(orientation);
    }

    @Override
    public void draw(float[] p, float[] v){

        program.use();


        for(float[] subOrien : subOriens){
            float[] pvms = new float[16];
            float[] vms = new float[16];
            float[] ms = new float[16];

            Matrix.multiplyMM(ms,0,orientation,0,subOrien,0);
            Matrix.multiplyMM(vms,0,v,0,ms,0);
            Matrix.multiplyMM(pvms,0,p,0,vms,0);


            for(LightingExtention extention: extentions){
                extention.bind(p,v,ms);
            }

            onDraw(pvms,vms,v);
        }

    }

    @Override
    public void drawShadow(Shadow shadow){
        if(!castingShadow)
            return;

        float[] pvm = new float[16];
        Matrix.multiplyMM(pvm,0,shadow.getPV(),0,orientation,0);

        for(float[] subOrien : subOriens) {
            float[] pvms = new float[16];

            Matrix.multiplyMM(pvms,0,pvm,0,subOrien,0);
            GLES30.glUniformMatrix4fv(shadow.getPvmUniformID(),1,false,pvms,0);
            onDrawShadow(shadow);
        }

    }

}
