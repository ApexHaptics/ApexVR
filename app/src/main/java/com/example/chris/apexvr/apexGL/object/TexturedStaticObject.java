package com.example.chris.apexvr.apexGL.object;

import android.opengl.GLES30;
import android.util.Log;

import com.example.chris.apexvr.apexGL.mesh.InterleavedMesh;
import com.example.chris.apexvr.apexGL.shader.GLProgram;
import com.example.chris.apexvr.apexGL.mesh.TexturedInterleavedMesh;
import com.example.chris.apexvr.apexGL.shader.Shadow;
import com.example.chris.apexvr.apexGL.shader.Texture;

/**
 * Created by Chris on 2/14/2017.
 */

public class TexturedStaticObject extends GLObject {

    private static String TAG = "TexturedStaticObject";
    private int vetexAtribID,pvmUniforID,vmUniforID,textCordAtribID,normalAtribID;
    private int[] glBuffers;
    private Texture texture;
    private int nIndexes;


    public TexturedStaticObject(GLProgram program, TexturedInterleavedMesh mesh, Texture texture) {
        super(program);


        this.texture = texture;

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
            textCordAtribID = program.getAtttributeID("texCord");
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
            pvmUniforID = program.getUniformID("PVM");
        } catch (Exception e) {
            Log.e(TAG,"Could not link uniform: " + e.toString());
            throw new RuntimeException("Could not link uniform: " + e.toString());
        }

        try {
            vmUniforID = program.getUniformID("VM");
        } catch (Exception e) {
            Log.e(TAG,"Could not link uniform: " + e.toString());
            throw new RuntimeException("Could not link uniform: " + e.toString());
        }

    }


    @Override
    public void onDraw(float[] pvm, float[] vm, float[] v) {

        GLES30.glEnableVertexAttribArray(vetexAtribID);
        GLES30.glEnableVertexAttribArray(normalAtribID);
        GLES30.glEnableVertexAttribArray(textCordAtribID);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,glBuffers[0]);
        GLES30.glVertexAttribPointer(vetexAtribID,3,GLES30.GL_FLOAT,false,8*Float.SIZE / 8,0);
        GLES30.glVertexAttribPointer(normalAtribID,3,GLES30.GL_FLOAT,false,8*Float.SIZE / 8,3*Float.SIZE / 8);
        GLES30.glVertexAttribPointer(textCordAtribID,2,GLES30.GL_FLOAT,false,8*Float.SIZE / 8,6*Float.SIZE / 8);

        GLES30.glUniformMatrix4fv(pvmUniforID,1,false,pvm,0);
        GLES30.glUniformMatrix4fv(vmUniforID,1,false,vm,0);

        texture.use();


        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,glBuffers[1]);


        GLES30.glDrawElements(GLES30.GL_TRIANGLES,nIndexes,GLES30.GL_UNSIGNED_INT,0);
        //GLES30.glDrawRangeElements(GLES30.GL_TRIANGLES,1,6,6,GLES30.GL_UNSIGNED_INT,0);


        GLES30.glDisableVertexAttribArray(vetexAtribID);
        GLES30.glDisableVertexAttribArray(normalAtribID);
        GLES30.glDisableVertexAttribArray(textCordAtribID);




    }

    @Override
    protected void onDrawShadow(Shadow shadow) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,glBuffers[0]);
        GLES30.glVertexAttribPointer(vetexAtribID,3,GLES30.GL_FLOAT,false,8*Float.SIZE / 8,0);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,glBuffers[1]);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES,nIndexes,GLES30.GL_UNSIGNED_INT,0);
    }
}
