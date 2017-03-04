package com.example.chris.apexvr.apexGL.shader;

/**
 * Created by Chris on 2/26/2017.
 */

public interface LightingExtention {
    abstract void link(GLProgram program);
    abstract void bind(float[] p, float[] v, float[] m);
}
