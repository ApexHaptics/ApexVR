package com.example.chris.apexvr.apexGL.mesh;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by Chris on 3/5/2017.
 */

public class ColourizedMesh extends Mesh{

    private Mesh.MeshConstructionData meshData;



    public static final int FLOAT_STRIDE = 9;


    protected ColourizedMesh(Mesh.MeshConstructionData meshData){

        this.meshData = meshData;

    }

    protected static void addVertex(float[] vertices, Mesh.MeshVertex meshNode, float[] colour){

        System.arraycopy(meshNode.vertex,0,vertices,meshNode.index*FLOAT_STRIDE,3);
        System.arraycopy(meshNode.normal,0,vertices,meshNode.index*FLOAT_STRIDE+3,3);
        System.arraycopy(colour,0,vertices,meshNode.index*FLOAT_STRIDE+6,3);

        if(meshNode.lower != null){
            addVertex(vertices,meshNode.lower, colour);
        }

        if(meshNode.upper != null){
            addVertex(vertices,meshNode.upper, colour);
        }

    }

    public ColouredInterleavedMesh asColouredMesh(float[] colour){
        FloatBuffer vertBuffer;
        IntBuffer indexes;
        float[] vertices = new float[meshData.nVertices * FLOAT_STRIDE];

        addVertex(vertices, meshData.vertexTree, colour);

        indexes = IntBuffer.wrap(meshData.indices);
        vertBuffer = FloatBuffer.wrap(vertices);

        return new ColouredInterleavedMesh(vertBuffer, indexes);
    }

    public static ColourizedMesh importOBJInterleavedMesh(InputStream inputStream) throws IOException {

        Mesh.ImportOptions options = new Mesh.ImportOptions(){};
        options.useTexture = false;
        options.useMaterial = false;

        return new ColourizedMesh(importOBJ(inputStream, null, options));
    }

}
