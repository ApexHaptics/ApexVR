package com.example.chris.apexvr.apexGL.mesh;

import android.content.res.AssetFileDescriptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Chris on 2/20/2017.
 */

public abstract class Mesh {

    protected static MeshConstructionData importOBJ(InputStream inputStream, MatLib matlib, ImportOptions options) throws IOException {

        List<float[]> vertices = null, uvs = null, normals = null;

        List<int[]> faceVerts = new ArrayList<>(100);
        List<MatLib.Material> facesMats = null;


        if(options.useVertex)vertices = new ArrayList<>(50);
        if(options.useTexture)uvs = new ArrayList<>(50);
        if(options.useNormal)normals = new ArrayList<>(50);
        if(options.useMaterial)facesMats = new ArrayList<>(30);



        try(BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, "UTF-8"))){

            String line;
            MatLib.Material currentMat = null;
            while((line= reader.readLine()) != null){
                String[] words = line.split(" ");
                switch (words[0]) {

                    case "v":
                        if(!options.useVertex)
                            break;

                        if (words.length < 4)
                            throw new IOException("Bad OBJ entry: " + line);

                        float[] vetex = new float[3];
                        vetex[0] = Float.parseFloat(words[1]);
                        vetex[1] = Float.parseFloat(words[2]);
                        vetex[2] = Float.parseFloat(words[3]);
                        vertices.add(vetex);

                        break;

                    case "vt":
                        if(!options.useTexture)
                            break;

                        if (words.length < 3)
                            throw new IOException("Bad OBJ entry: " + line);

                        float[] uv = new float[2];
                        uv[0] = Float.parseFloat(words[1]);
                        uv[1] = Float.parseFloat(words[2]);
                        uvs.add(uv);

                        break;

                    case "vn":
                        if(!options.useNormal)
                            break;

                        if (words.length < 4)
                            throw new IOException("Bad OBJ entry: " + line);

                        float[] normal = new float[3];
                        normal[0] = Float.parseFloat(words[1]);
                        normal[1] = Float.parseFloat(words[2]);
                        normal[2] = Float.parseFloat(words[3]);
                        normals.add(normal);

                        break;

                    case "usemtl":
                        if(!options.useMaterial)
                            break;

                        if (words.length < 2)
                            throw new IOException("Bad OBJ entry: " + line);

                        currentMat = matlib.getMatterial(words[1]);

                        if(currentMat == null){
                            throw new IOException("Material not found: " + line);
                        }

                        break;

                    case "f":
                        if (words.length < 4)
                            throw new IOException("Bad OBJ entry: " + line);

                        if(options.useMaterial){
                            if(currentMat == null)
                                throw new IOException("Material not set: " + line);

                            facesMats.add(currentMat);
                        }




                        for (int i = 1; i < 4; ++i) {
                            String[] vis = words[i].split("/");
                            if (vis.length < 3)
                                throw new IOException("Bad OBJ entry: " + line);

                            int[] face = new int[3];
                            try {

                                if (options.useVertex) face[0] = Integer.parseInt(vis[0]) - 1;
                                if (options.useTexture) face[1] = Integer.parseInt(vis[1]) - 1;
                                if (options.useNormal) face[2] = Integer.parseInt(vis[2]) - 1;

                            }catch (NumberFormatException e){
                                throw new IOException("Missing data in farce: " + line);
                            }

                            faceVerts.add(face);
                        }
                        break;
                }
            }

        }

        MeshConstructionData meshData = new MeshConstructionData();

        meshData.indices = new int[faceVerts.size()];

        if(faceVerts.size() < 1){
            meshData.vertexTree = null;
            return meshData;
        }

        meshData.vertexTree = new MeshVertex(options);
        meshData.nVertices = 1;

        if(options.useVertex)   meshData.vertexTree.vertex = vertices.get(faceVerts.get(0)[0]);
        if(options.useTexture)  meshData.vertexTree.uv = uvs.get(faceVerts.get(0)[1]);
        if(options.useNormal)   meshData.vertexTree.normal = normals.get(faceVerts.get(0)[2]);
        if(options.useMaterial) meshData.vertexTree.colour = facesMats.get(0).diffuseColour;

        meshData.indices[0] = 0;

        for(int i = 1; i < faceVerts.size(); ++i){
            int[] face = faceVerts.get(i);

            MeshVertex meshVertex = new MeshVertex(options);
            meshVertex.index = meshData.nVertices;

            if(options.useVertex)   meshVertex.vertex = vertices.get(face[0]);
            if(options.useTexture)  meshVertex.uv =     uvs.get(face[1]);
            if(options.useNormal)   meshVertex.normal = normals.get(face[2]);
            if(options.useMaterial) meshVertex.colour = facesMats.get(i/3).diffuseColour;

            if(meshData.vertexTree.findorpLace(meshVertex)){
                ++meshData.nVertices;
            }

            meshData.indices[i] = meshVertex.index;

        }

        return meshData;

    }


    protected static class MeshVertex implements Comparable<MeshVertex>{
        static final float FLOAT_EPP = 0.0001f;
        int index = 0;
        ImportOptions options;
        MeshVertex lower = null;
        MeshVertex upper = null;

        float[] vertex;
        float[] normal;
        float[] uv;
        float[] colour;

        public MeshVertex(ImportOptions options){

            this.options = options;
        }


        protected boolean findorpLace(MeshVertex meshVertex){
            int comp = compareTo((meshVertex));

            if(comp == 0){
                meshVertex.index = index;
                return false;
            }


            if(comp > 0){
                if(lower == null){
                    lower = meshVertex;
                    return true;
                }

                return lower.findorpLace(meshVertex);
            }

            if(upper == null){
                upper = meshVertex;
                return true;
            }

            return upper.findorpLace(meshVertex);
        }


        @Override
        public int compareTo( MeshVertex o) {

            if(options.useVertex){
                for(int i = 0; i < 3; ++i){
                    float diff = vertex[i] - o.vertex[i];
                    if(diff > FLOAT_EPP){
                        return 1;
                    }else if(-diff > FLOAT_EPP){
                        return -1;
                    }
                }
            }

            if (options.useNormal) {
                for(int i = 0; i < 3; ++i){
                    float diff = normal[i] - o.normal[i];
                    if(diff > FLOAT_EPP){
                        return 1;
                    }else if(-diff > FLOAT_EPP){
                        return -1;
                    }
                }
            }

            if(options.useMaterial){
                for(int i = 0; i < 3; ++i){
                    float diff = colour[i] - o.colour[i];
                    if(diff > FLOAT_EPP){
                        return 1;
                    }else if(-diff > FLOAT_EPP){
                        return -1;
                    }
                }
            }

            if(options.useTexture){
                for(int i = 0; i < 2; ++i){
                    float diff = uv[i] - o.uv[i];
                    if(diff > FLOAT_EPP){
                        return 1;
                    }else if(-diff > FLOAT_EPP){
                        return -1;
                    }
                }
            }

            return 0;
        }

    }

    protected static class ImportOptions {
        boolean useVertex = true;
        boolean useTexture = true;
        boolean useNormal = true;
        boolean useMaterial = true;
    }

    protected static class MeshConstructionData {
        int nVertices;
        MeshVertex vertexTree;
        int[] indices;
    }


}
