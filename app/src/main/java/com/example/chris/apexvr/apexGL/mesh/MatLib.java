package com.example.chris.apexvr.apexGL.mesh;

import android.content.res.AssetFileDescriptor;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Created by Chris on 2/23/2017.
 */

public class MatLib {
    private static final String TAG = "MATLIB";
    HashMap<String,Material> materials;

    public MatLib(){
        materials = new HashMap<>(10);
    }

    public void addMatLib(InputStream inputStream) throws IOException {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"))){
            Material currentMaterial = null;

            String line;

            while ((line = reader.readLine()) != null){

                String[] words = line.split(" ");

                switch (words[0]){
                    case "newmtl":
                        if(words.length < 2){
                            throw new IOException("Material has no name: " + line);
                        }

                        addMat(currentMaterial);

                        currentMaterial = new Material();
                        currentMaterial.name = words[1];
                        break;

                    case "Kd":
                        if(words.length < 4){
                            throw new IOException("Defuse has not colour: " + line);
                        }

                        currentMaterial.diffuseColour = new float[3];
                        currentMaterial.diffuseColour[0] = Float.parseFloat(words[1]);
                        currentMaterial.diffuseColour[1] = Float.parseFloat(words[2]);
                        currentMaterial.diffuseColour[2] = Float.parseFloat(words[3]);

                        break;

                }
            }

            addMat(currentMaterial);
        }
    }

    private void addMat(Material mat) throws IOException {
        if(mat != null){
            if(mat.diffuseColour == null){
                throw new IOException("Material " + mat.name + "has not diffuse colour");
            }

            if(materials.containsKey(mat.name)){
                Log.w(TAG,"material collision " + mat.name);
            }

            materials.put(mat.name,mat);
        }
    }

    public Material getMatterial(String name){
        return materials.get(name);
    }

    public class Material{
        String name;
        float[] diffuseColour;
    }
}
