package com.example.chris.apexvr.apexGL.mesh;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by Chris on 2/23/2017.
 */

public class ColouredInterleavedMesh extends Mesh {

    public FloatBuffer vertices;
    public IntBuffer indexes;

    public static final int FLOAT_STRIDE = 9;

    private ColouredInterleavedMesh(MeshConstructionData meshData){


        float[] vertices = new float[meshData.nVertices * FLOAT_STRIDE];

        addVertex(vertices, meshData.vertexTree);

        indexes = IntBuffer.wrap(meshData.indices);
        this.vertices = FloatBuffer.wrap(vertices);

    }

    private static void addVertex(float[] vertices, MeshVertex meshNode){

        System.arraycopy(meshNode.vertex,0,vertices,meshNode.index*FLOAT_STRIDE,3);
        System.arraycopy(meshNode.normal,0,vertices,meshNode.index*FLOAT_STRIDE+3,3);
        System.arraycopy(meshNode.colour,0,vertices,meshNode.index*FLOAT_STRIDE+6,3);

        if(meshNode.lower != null){
            addVertex(vertices,meshNode.lower);
        }

        if(meshNode.upper != null){
            addVertex(vertices,meshNode.upper);
        }

    }

    public static ColouredInterleavedMesh importOBJInterleavedMesh(InputStream inputStream, MatLib matLib) throws IOException {

        Mesh.ImportOptions options = new ImportOptions(){};
        options.useTexture = false;

        return new ColouredInterleavedMesh(importOBJ(inputStream, matLib, options));
    }

}
