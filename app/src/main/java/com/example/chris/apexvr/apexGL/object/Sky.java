package com.example.chris.apexvr.apexGL.object;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.example.chris.apexvr.apexGL.shader.GLProgram;
import com.example.chris.apexvr.apexGL.shader.LightingExtention;
import com.example.chris.apexvr.apexGL.shader.Shadow;

import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * Created by Chris on 3/14/2017.
 */

public class Sky extends GLObject {
    private static final String TAG = "APEX_SKY";
    private static final int N_VERTS = 18;
    private final int[] glBuffers;
    private final int vetexAtribID,prUniformID;

    public Sky(GLProgram program) {
        super(program);

        glBuffers = new int[1];

        GLES30.glGenBuffers(1,glBuffers,0);

        float[] background = new float[]{
                -1.0f,1.0f,1.0f,
                -1.0f,-1.0f,1.0f,
                1.0f,-1.0f,1.0f,

                1.0f,-1.0f,1.0f,
                1.0f,1.0f,1.0f,
                -1.0f,1.0f,1.0f
        };
        FloatBuffer vertices = FloatBuffer.wrap(background);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,glBuffers[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, N_VERTS * Float.SIZE / 8, vertices, GLES30.GL_STATIC_DRAW);

        try {
            vetexAtribID = program.getAtttributeID("position");
        } catch (Exception e) {
            Log.e(TAG,"Could not link buffers: " + e.toString());
            throw new RuntimeException("Could not link buffers: " + e.toString());
        }

        try {
            prUniformID = program.getUniformID("PR");
        } catch (Exception e) {
            Log.e(TAG,"Could not link buffers: " + e.toString());
            throw new RuntimeException("Could not link buffers: " + e.toString());
        }

    }

    @Override
    public void draw(float[] p, float[] v){
        if(!draw){
            return;
        }
        program.use();

        for(LightingExtention extention: extentions){
            extention.bind(p,v,orientation);
        }

        float[] pr = new float[16];
        float[] pri = new float[16];
        float[] r = Arrays.copyOf(v,16);
        for(int i = 12; i < 15; ++i){
            r[i] = 0.0f;
        }
        Matrix.multiplyMM(pr,0,p,0,r,0);
        Matrix.invertM(pri,0,pr,0);
        onDraw(pri,p,v);

    }

    @Override
    protected void onDraw(float[] pr, float[] p, float[] v) {
        GLES30.glEnableVertexAttribArray(vetexAtribID);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,glBuffers[0]);
        GLES30.glVertexAttribPointer(vetexAtribID,3,GLES30.GL_FLOAT,false,0,0);

        GLES30.glUniformMatrix4fv(prUniformID,1,false,pr,0);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, N_VERTS);

        GLES30.glDisableVertexAttribArray(vetexAtribID);

    }

    @Override
    protected void onDrawShadow(Shadow shadow) {}
}
