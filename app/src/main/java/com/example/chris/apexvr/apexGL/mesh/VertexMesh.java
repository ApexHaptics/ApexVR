package com.example.chris.apexvr.apexGL.mesh;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by Chris on 2/25/2017.
 */

public class VertexMesh extends Mesh {

    public FloatBuffer vertices;
    public IntBuffer indexes;

    public static final int FLOAT_STRIDE = 3;

    private VertexMesh(MeshConstructionData meshData){

        float[] vertices = new float[meshData.nVertices * FLOAT_STRIDE];

        addVertex(vertices, meshData.vertexTree);

        indexes = IntBuffer.wrap(meshData.indices);
        this.vertices = FloatBuffer.wrap(vertices);

    }

    private static void addVertex(float[] vertices, Mesh.MeshVertex meshNode){
        System.arraycopy(meshNode.vertex,0,vertices,meshNode.index*FLOAT_STRIDE,3);

        if(meshNode.lower != null){
            addVertex(vertices,meshNode.lower);
        }

        if(meshNode.upper != null){
            addVertex(vertices,meshNode.upper);
        }

    }

    public static VertexMesh importOBJInterleavedMesh(InputStream inputStream) throws IOException {

        Mesh.ImportOptions options = new Mesh.ImportOptions(){};
        options.useMaterial = false;
        options.useTexture = false;
        options.useNormal = false;

        return new VertexMesh(importOBJ(inputStream,null,options));
    }
}
