package com.example.chris.apexvr.apexGL.object;

import android.opengl.GLES30;
import android.util.Log;

import com.example.chris.apexvr.apexGL.mesh.ColouredInterleavedMesh;
import com.example.chris.apexvr.apexGL.shader.GLProgram;
import com.example.chris.apexvr.apexGL.shader.Shadow;

/**
 * Created by Chris on 2/23/2017.
 */

public class ColouredStaticObject extends GLObject {

    private static String TAG = "ColouredStaticObject";
    private int vetexAtribID,normalAtribID,colourAtribID, pvmUniformID, vmUniformID, vUniformID;
    private int[] glBuffers;

    private int nIndexes;


    public ColouredStaticObject(GLProgram program, ColouredInterleavedMesh mesh) {
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
            colourAtribID = program.getAtttributeID("colour");
        } catch (Exception e) {
            Log.e(TAG,"Could not link buffers: " + e.toString());
            throw new RuntimeException("Could not link buffers: " + e.toString());
        }

        try {
            normalAtribID = program.getAtttributeID("normal");
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
            vUniformID = program.getUniformID("V");
        } catch (Exception e) {
            Log.e(TAG,"Could not link uniform: " + e.toString());
            throw new RuntimeException("Could not link uniform: " + e.toString());
        }

    }

    @Override
    public void onDraw(float[] pvm, float[] vm, float[] v) {
        GLES30.glEnableVertexAttribArray(vetexAtribID);
        GLES30.glEnableVertexAttribArray(normalAtribID);
        GLES30.glEnableVertexAttribArray(colourAtribID);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,glBuffers[0]);
        GLES30.glVertexAttribPointer(vetexAtribID,3,GLES30.GL_FLOAT,false,ColouredInterleavedMesh.FLOAT_STRIDE*Float.SIZE / 8,0);
        GLES30.glVertexAttribPointer(normalAtribID,3,GLES30.GL_FLOAT,false,ColouredInterleavedMesh.FLOAT_STRIDE*Float.SIZE / 8,3*Float.SIZE / 8);
        GLES30.glVertexAttribPointer(colourAtribID,3,GLES30.GL_FLOAT,false,ColouredInterleavedMesh.FLOAT_STRIDE*Float.SIZE / 8,6*Float.SIZE / 8);

        GLES30.glUniformMatrix4fv(pvmUniformID,1,false,pvm,0);
        GLES30.glUniformMatrix4fv(vmUniformID,1,false,vm,0);
        GLES30.glUniformMatrix4fv(vUniformID,1,false,v,0);

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,glBuffers[1]);


        GLES30.glDrawElements(GLES30.GL_TRIANGLES,nIndexes,GLES30.GL_UNSIGNED_INT,0);


        GLES30.glDisableVertexAttribArray(vetexAtribID);
        GLES30.glDisableVertexAttribArray(normalAtribID);
        GLES30.glDisableVertexAttribArray(colourAtribID);

    }

    @Override
    protected void onDrawShadow(Shadow shadow) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,glBuffers[0]);
        GLES30.glVertexAttribPointer(shadow.getVetexAtribID(),3,GLES30.GL_FLOAT,false,ColouredInterleavedMesh.FLOAT_STRIDE*Float.SIZE / 8,0);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,glBuffers[1]);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES,nIndexes,GLES30.GL_UNSIGNED_INT,0);
    }
}
