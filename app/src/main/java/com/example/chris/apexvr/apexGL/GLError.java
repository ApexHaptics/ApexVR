package com.example.chris.apexvr.apexGL;

import android.opengl.GLES30;
import android.util.Log;

/**
 * Created by Chris on 2/26/2017.
 */

public class GLError {

    public static void checkGLError(String TAG, String label){

        for(int e = GLES30.glGetError(); e != GLES30.GL_NO_ERROR; e = GLES30.glGetError()){
            String errorName = ErrorToString(e);
            Log.e(TAG,label + ": glError " + errorName);
            throw new RuntimeException(label + ": glError " + errorName);
        }

    }

    public static String ErrorToString(int error){
        switch (error){
            case GLES30.GL_INVALID_ENUM:return "GL_INVALID_ENUM";
            case GLES30.GL_INVALID_VALUE:return "GL_INVALID_VALUE";
            case GLES30.GL_INVALID_OPERATION:return "GL_INVALID_OPERATION";
            case GLES30.GL_INVALID_FRAMEBUFFER_OPERATION:return "GL_INVALID_FRAMEBUFFER_OPERATION";
            case GLES30.GL_OUT_OF_MEMORY:return "GL_OUT_OF_MEMORY";
            default:return error + "is unknown, sorry :(";
        }
    }

    public static void checkFrameBuffer(String TAG, String label, int target){
        int e = GLES30.glCheckFramebufferStatus(target);
        if(e != GLES30.GL_FRAMEBUFFER_COMPLETE){
            String errorName = FrameErrorToString(e);
            Log.e(TAG,label + ": glError " + errorName);
            throw new RuntimeException(label + ": glError " + errorName);
        }

    }

    public static String FrameErrorToString(int error){
        switch (error){
            case GLES30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:return "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
            case GLES30.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:return "GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS";
            case GLES30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:return "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
            case GLES30.GL_FRAMEBUFFER_UNSUPPORTED:return "GL_FRAMEBUFFER_UNSUPPORTED";
            default:return error + "is unknown, sorry :(";
        }
    }
}
