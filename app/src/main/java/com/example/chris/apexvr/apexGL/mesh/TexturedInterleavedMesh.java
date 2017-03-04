package com.example.chris.apexvr.apexGL.mesh;

import android.content.res.AssetFileDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by Chris on 2/20/2017.
 */

public class TexturedInterleavedMesh extends Mesh {


    public FloatBuffer vertices;
    public IntBuffer indexes;

    private TexturedInterleavedMesh(MeshConstructionData meshData){


        float[] vertices = new float[meshData.nVertices * 8];

        addVertex(vertices, meshData.vertexTree);

        indexes = IntBuffer.wrap(meshData.indices);
        this.vertices = FloatBuffer.wrap(vertices);

    }

    private static void addVertex(float[] vertices, MeshVertex meshNode){
        System.arraycopy(meshNode.vertex,0,vertices,meshNode.index*8,3);
        System.arraycopy(meshNode.normal,0,vertices,meshNode.index*8+3,3);
        System.arraycopy(meshNode.uv,0,vertices,meshNode.index*8+6,2);

        if(meshNode.lower != null){
            addVertex(vertices,meshNode.lower);
        }

        if(meshNode.upper != null){
            addVertex(vertices,meshNode.upper);
        }

    }

    public static TexturedInterleavedMesh importOBJInterleavedMesh(InputStream inputStream) throws IOException {

        Mesh.ImportOptions options = new ImportOptions(){};
        options.useMaterial = false;

        return new TexturedInterleavedMesh(importOBJ(inputStream,null,options));
    }




}
