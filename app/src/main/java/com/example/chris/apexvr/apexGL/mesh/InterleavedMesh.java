package com.example.chris.apexvr.apexGL.mesh;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.example.chris.apexvr.apexGL.mesh.Mesh.importOBJ;

/**
 * Created by Chris on 2/25/2017.
 */

public class InterleavedMesh extends Mesh {


    public FloatBuffer vertices;
    public IntBuffer indexes;

    public static final int FLOAT_STRIDE = 6;

    private InterleavedMesh(MeshConstructionData meshData){

        float[] vertices = new float[meshData.nVertices * FLOAT_STRIDE];

        addVertex(vertices, meshData.vertexTree);

        indexes = IntBuffer.wrap(meshData.indices);
        this.vertices = FloatBuffer.wrap(vertices);

    }

    private static void addVertex(float[] vertices, Mesh.MeshVertex meshNode){
        System.arraycopy(meshNode.vertex,0,vertices,meshNode.index*FLOAT_STRIDE,3);
        System.arraycopy(meshNode.normal,0,vertices,meshNode.index*FLOAT_STRIDE+3,3);

        if(meshNode.lower != null){
            addVertex(vertices,meshNode.lower);
        }

        if(meshNode.upper != null){
            addVertex(vertices,meshNode.upper);
        }

    }

    public static InterleavedMesh importOBJInterleavedMesh(InputStream inputStream) throws IOException {

        Mesh.ImportOptions options = new Mesh.ImportOptions(){};
        options.useMaterial = false;
        options.useTexture = false;

        return new InterleavedMesh(importOBJ(inputStream,null,options));
    }
}
