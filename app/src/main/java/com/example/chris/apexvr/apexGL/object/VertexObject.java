package com.example.chris.apexvr.apexGL.object;

import android.opengl.GLES30;
import android.util.Log;

import com.example.chris.apexvr.apexGL.mesh.VertexMesh;
import com.example.chris.apexvr.apexGL.shader.GLProgram;
import com.example.chris.apexvr.apexGL.shader.Shadow;

/**
 * Created by Chris on 2/25/2017.
 */

public class VertexObject extends GLObject {

    private static String TAG = "StaticObject";
    private int vetexAtribID, pvmUniformID, vmUniformID, mUniformID;
    private int[] glBuffers;

    private int nIndexes;

    public VertexObject(GLProgram program, VertexMesh mesh) {
        super(program);

        glBuffers = new int[2];

        GLES30.glGenBuffers(2,glBuffers,0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,glBuffers[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                mesh.vertices.limit() * Float.SIZE / 8,
                mesh.vertices,
                GLES30.GL_STATIC_DRAW);


        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,glBuffers[1]);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER,
                mesh.indexes.limit() * Integer.SIZE / 8,
                mesh.indexes,
                GLES30.GL_STATIC_DRAW);



        nIndexes = mesh.indexes.limit();


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

        try {
            vmUniformID = program.getUniformID("VM");
        } catch (Exception e) {
            Log.e(TAG,"Could not link uniform: " + e.toString());
            throw new RuntimeException("Could not link uniform: " + e.toString());
        }

        try {
            mUniformID = program.getUniformID("M");
        } catch (Exception e) {
            Log.e(TAG,"Could not link uniform: " + e.toString());
            throw new RuntimeException("Could not link uniform: " + e.toString());
        }

    }


    @Override
    public void onDraw(float[] pvm, float[] vm, float[] v) {
        GLES30.glEnableVertexAttribArray(vetexAtribID);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,glBuffers[0]);
        GLES30.glVertexAttribPointer(vetexAtribID,3,GLES30.GL_FLOAT,false,0,0);

        GLES30.glUniformMatrix4fv(pvmUniformID,1,false,pvm,0);
        GLES30.glUniformMatrix4fv(vmUniformID,1,false,vm,0);
        GLES30.glUniformMatrix4fv(mUniformID,1,false,getOrientation(),0);

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,glBuffers[1]);


        GLES30.glDrawElements(GLES30.GL_TRIANGLES,nIndexes,GLES30.GL_UNSIGNED_INT,0);


        GLES30.glDisableVertexAttribArray(vetexAtribID);
    }

    @Override
    protected void onDrawShadow(Shadow shadow) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,glBuffers[0]);
        GLES30.glVertexAttribPointer(vetexAtribID,3,GLES30.GL_FLOAT,false,0,0);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,glBuffers[1]);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES,nIndexes,GLES30.GL_UNSIGNED_INT,0);
    }
}
