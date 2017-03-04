package com.example.chris.apexvr.apexGL.shader;

import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;

import java.io.Closeable;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import android.opengl.GLES30;
import android.util.Log;

/**
 * Created by Chris on 2/9/2017.
 */

public class Shader implements Closeable{

    private static final String TAG = "Shader";
    final int[] compileStatus;

    private int shader;
    private boolean closed = false;

    public int getShaderID() {
        return shader;
    }



    public boolean isClosed() {
        return closed;
    }



    public Shader(int shaderID, String code){
        shader = shaderID;

        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);

        compileStatus = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES30.glGetShaderInfoLog(shader));

            close();
            throw new RuntimeException("Error creating shader.");
        }
    }


    public static Shader loadShader(InputStream inputStream,int type){

        String code = readRawTextFile(inputStream);

        return new Shader(GLES30.glCreateShader(type), code);

    }





    private static String readRawTextFile(InputStream inputStream) {


        try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))){
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("cannot read file.");
        }
    }


    @Override
    public void close(){
        if(closed)
            return;


        closed = true;
        GLES30.glDeleteShader(shader);


    }

    public int getCompileStatus() {
        return compileStatus[0];
    }
}
