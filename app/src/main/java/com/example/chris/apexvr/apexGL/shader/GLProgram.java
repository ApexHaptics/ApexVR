package com.example.chris.apexvr.apexGL.shader;



import android.opengl.GLES30;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by Chris on 2/12/2017.
 */

public class GLProgram implements Closeable{

    private int program;
    private boolean closed = false;

    public GLProgram(){
        program = GLES30.glCreateProgram();

    }

    public void attachShader(Shader... shaders){

        if(closed)
            return;

        for(Shader shader : shaders){
            GLES30.glAttachShader(program, shader.getShaderID());
        }

        GLES30.glLinkProgram(program);

    }

    public void use(){
        GLES30.glUseProgram(program);
    }

    public int getAtttributeID(String name) throws Exception {

        int location = GLES30.glGetAttribLocation(program,name);

        if(location < 0){
            throw new Exception("Attribue " + name + " is not suported in program " + program);
        }

        return location;
    }

    public int getUniformID(String name) throws Exception {

        int location = GLES30.glGetUniformLocation(program,name);

        if(location < 0){
            throw new Exception("Uniform " + name + " is not suported in program " + program);
        }

        return location;
    }

    @Override
    public void close() throws IOException {
        if(closed)
            return;

        closed = true;

        GLES30.glDeleteProgram(program);

    }
}
